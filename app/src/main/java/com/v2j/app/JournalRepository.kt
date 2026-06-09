package com.v2j.app

import com.v2j.app.data.Draft
import com.v2j.app.data.DraftDao
import com.v2j.app.data.Entry
import com.v2j.app.data.EntryDao
import com.v2j.app.data.PolishStatus
import com.v2j.app.data.SettingsRepository
import com.v2j.app.net.DeepSeekClient
import com.v2j.app.net.PolishConfig
import com.v2j.app.net.WebDavClient
import com.v2j.app.sync.WeekMath
import com.v2j.app.sync.WeekRenderer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Result of one sync run, summarised for the UI. */
data class SyncReport(
    val synced: List<String> = emptyList(),
    val skipped: List<String> = emptyList(),
    val failed: List<Pair<String, String>> = emptyList(),
    val error: String? = null,
    val nothingToDo: Boolean = false
) {
    fun message(): String = when {
        error != null -> error
        nothingToDo -> "没有需要同步的内容"
        else -> buildString {
            if (synced.isNotEmpty()) append("已同步 ${synced.size} 周：${synced.joinToString("、")}")
            if (skipped.isNotEmpty()) {
                if (isNotEmpty()) append("；")
                append("${skipped.size} 周因有未润色成功的条目被跳过")
            }
            if (failed.isNotEmpty()) {
                if (isNotEmpty()) append("；")
                append("${failed.size} 周上传失败：${failed.first().second}")
            }
            if (isEmpty()) append("完成")
        }
    }
}

class JournalRepository(
    private val entryDao: EntryDao,
    private val draftDao: DraftDao,
    private val deepSeek: DeepSeekClient,
    private val webDav: WebDavClient,
    private val settings: SettingsRepository
) {

    private val syncMutex = Mutex()

    private fun now() = System.currentTimeMillis()

    // ---- startup ----

    /** Reset POLISHING zombies left by a process killed mid-polish. */
    suspend fun recoverOnStart() = entryDao.resetPolishing()

    // ---- draft ----

    fun currentWeekEntries(): Flow<List<Entry>> {
        val today = WeekMath.localDate(now())
        return entryDao.observeWeekLive(
            WeekMath.weekStartMillis(today),
            WeekMath.weekEndExclusiveMillis(today)
        )
    }

    /** All live entries (newest first) for the History screen. */
    fun allEntries(): Flow<List<Entry>> = entryDao.observeAllLive()

    suspend fun loadDraft(): String = draftDao.load().orEmpty()

    suspend fun saveDraft(text: String) = draftDao.upsert(Draft(0, text, now()))

    suspend fun clearDraft() = draftDao.clear()

    // ---- entries ----

    /** Insert the draft as an entry (durable) and return its id. Polishing is a separate step
     *  so the entry is persisted BEFORE the draft is cleared — no lost-text window on crash. */
    suspend fun addEntry(rawText: String): Long {
        val ts = now()
        return entryDao.insert(
            Entry(createdAt = ts, updatedAt = ts, rawText = rawText, polishStatus = PolishStatus.RAW)
        )
    }

    /** Polish one entry if the active provider is configured (used after 完成 and for manual retry). */
    suspend fun polishEntry(id: Long) {
        val config = settings.get().polishConfig() ?: return
        entryDao.byId(id)?.let { tryPolish(it, config) }
    }

    suspend fun softDelete(entry: Entry) {
        entryDao.update(entry.copy(deletedAt = now(), syncDirty = true, updatedAt = now()))
    }

    private suspend fun tryPolish(entry: Entry, config: PolishConfig) {
        // Field-level updates guarded by `deletedAt IS NULL`: if the user deletes this entry
        // during the network call, the result is dropped instead of resurrecting the row.
        entryDao.updateStatus(entry.id, PolishStatus.POLISHING, now())
        try {
            val polished = deepSeek.polish(entry.rawText, config)
            entryDao.updatePolished(entry.id, polished, PolishStatus.POLISHED, now())
        } catch (t: Throwable) {
            entryDao.updateStatus(entry.id, PolishStatus.FAILED, now())
        }
    }

    /** Pick the active polish provider: custom (if switched on & filled) else DeepSeek default. */
    private fun SettingsRepository.Settings.polishConfig(): PolishConfig? = when {
        useCustomProvider ->
            if (customBaseUrl.isNotBlank() && customApiKey.isNotBlank() && customModel.isNotBlank())
                PolishConfig(customBaseUrl, customApiKey, customModel, deepseekDefaults = false)
            else null
        else ->
            if (deepseekKey.isNotBlank()) PolishConfig.deepSeek(deepseekKey) else null
    }

    // ---- connection tests (Settings screen) ----

    suspend fun testDeepSeek(key: String): Result<Unit> = deepSeek.ping(PolishConfig.deepSeek(key))

    suspend fun testCustomProvider(url: String, key: String, model: String): Result<Unit> =
        deepSeek.ping(PolishConfig(url, key, model, deepseekDefaults = false))

    suspend fun testWebDav(basePath: String, email: String, appPassword: String): Result<String> =
        webDav.test(basePath, email, appPassword)

    // ---- sync ----

    /**
     * Publish every dirty week. A week is uploaded only when all its live entries are POLISHED
     * (unless allowRawPublish is on), so raw speech never leaks to Obsidian. Single-flight.
     */
    suspend fun sync(): SyncReport = syncMutex.withLock {
        val s = settings.get()
        if (!s.isComplete) {
            return SyncReport(error = "请先在设置里填好润色配置（DeepSeek 或自定义）、坚果云邮箱/应用密码、目标路径")
        }
        val config = s.polishConfig()
            ?: return SyncReport(error = "请先在设置里填好润色配置（DeepSeek 或自定义）")

        val dirty = entryDao.dirtyEntries()
        if (dirty.isEmpty()) return SyncReport(nothingToDo = true)

        // Distinct week-start timestamps among dirty entries.
        val weekStarts = dirty
            .map { WeekMath.weekStartMillis(WeekMath.localDate(it.createdAt)) }
            .toSortedSet()

        val synced = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        val failed = mutableListOf<Pair<String, String>>()

        for (startMillis in weekStarts) {
            val anchor = WeekMath.localDate(startMillis)
            val endMillis = WeekMath.weekEndExclusiveMillis(anchor)
            val weekId = WeekMath.weekId(anchor)
            val startDate = WeekMath.weekStartDate(anchor)
            val endDate = WeekMath.weekEndDate(anchor)

            // Retry polish on live RAW/FAILED entries of this week.
            entryDao.entriesInRange(startMillis, endMillis)
                .filter { it.deletedAt == null && it.polishStatus != PolishStatus.POLISHED }
                .forEach { tryPolish(it, config) }

            val snapshot = entryDao.entriesInRange(startMillis, endMillis)
            val live = snapshot.filter { it.deletedAt == null }.sortedBy { it.createdAt }
            val snapshotAt = now()

            if (!s.allowRawPublish && live.any { it.polishStatus != PolishStatus.POLISHED }) {
                skipped.add(weekId)
                continue
            }

            val markdown = WeekRenderer.render(weekId, startDate, endDate, live)
            val fileName = WeekMath.fileName(weekId, startDate, endDate)
            val result = webDav.put(
                basePath = s.webdavBasePath,
                fileName = fileName,
                content = markdown,
                email = s.nutstoreEmail,
                appPassword = s.nutstoreAppPassword
            )

            if (result.isSuccess) {
                // Mark synced ONLY the entries we actually rendered, and only if untouched since
                // the snapshot — so anything added/deleted during the upload stays dirty for next sync.
                val liveIds = live.map { it.id }
                if (liveIds.isNotEmpty()) entryDao.markSyncedByIds(liveIds, now(), snapshotAt)
                val tombstoneIds = snapshot.filter { it.deletedAt != null }.map { it.id }
                if (tombstoneIds.isNotEmpty()) entryDao.purgeByIds(tombstoneIds)
                synced.add(weekId)
            } else {
                failed.add(weekId to (result.exceptionOrNull()?.message ?: "未知错误"))
            }
        }

        SyncReport(synced = synced, skipped = skipped, failed = failed)
    }
}
