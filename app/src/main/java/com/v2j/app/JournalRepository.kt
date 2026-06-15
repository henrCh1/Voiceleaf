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

    /** Live entries of the ISO week that [anchorMillis] falls in. The caller passes a fresh
     *  timestamp so a long-lived process that crossed midnight/week boundary shows the right week. */
    fun entriesForWeek(anchorMillis: Long): Flow<List<Entry>> {
        val day = WeekMath.localDate(anchorMillis)
        return entryDao.observeWeekLive(
            WeekMath.weekStartMillis(day),
            WeekMath.weekEndExclusiveMillis(day)
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
        entryDao.softDeleteById(entry.id, now())
    }

    /** User hand-edit of an entry's finished text (from the 本周记录 / 历史 list). createdAt
     *  is left untouched so the entry stays in its original week/day; the week is marked dirty. */
    suspend fun editEntry(id: Long, newText: String) {
        val text = newText.trim()
        if (text.isEmpty()) return
        entryDao.editFinalText(id, text, now())
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
    /** Manual 同步: publish every week that has unsynced changes. */
    suspend fun sync(): SyncReport {
        val weekStarts = entryDao.dirtyEntries()
            .map { WeekMath.weekStartMillis(WeekMath.localDate(it.createdAt)) }
            .toSortedSet()
        if (weekStarts.isEmpty()) return SyncReport(nothingToDo = true)
        return syncWeeks(weekStarts, force = false)
    }

    /** History 单周同步: force re-render & re-upload one week even if it's locally clean
     *  (so a week whose cloud file was lost / path changed can be re-pushed on demand). */
    suspend fun syncWeek(weekStartMillis: Long): SyncReport =
        syncWeeks(listOf(weekStartMillis), force = true)

    /** Auto catch-up on app open: silently push only PAST weeks that still have unsynced changes.
     *  The current week is left alone so the "攒一周再发" flow stays under manual control. */
    suspend fun catchUpPastWeeks(): SyncReport {
        val currentWeekStart = WeekMath.weekStartMillis(WeekMath.localDate(now()))
        val weekStarts = entryDao.dirtyEntries()
            .map { WeekMath.weekStartMillis(WeekMath.localDate(it.createdAt)) }
            .filter { it < currentWeekStart }
            .toSortedSet()
        if (weekStarts.isEmpty()) return SyncReport(nothingToDo = true)
        return syncWeeks(weekStarts, force = false)
    }

    /**
     * Core sync over a set of weeks. Single-flight. A week is uploaded only when all its live
     * entries are POLISHED (unless allowRawPublish), so raw speech never leaks to Obsidian.
     *  - force = false: skip a week with no unsynced changes (used by sync()/catchUp, whose week
     *    lists are already dirty-only — this is belt-and-suspenders).
     *  - force = true: re-render & re-upload regardless of dirty state (used by the per-week button).
     */
    private suspend fun syncWeeks(weekStarts: Collection<Long>, force: Boolean): SyncReport =
        syncMutex.withLock {
            val s = settings.get()
            if (!s.isComplete) {
                return SyncReport(error = "请先在设置里填好润色配置（DeepSeek 或自定义）、坚果云邮箱/应用密码、目标路径")
            }
            val config = s.polishConfig()
                ?: return SyncReport(error = "请先在设置里填好润色配置（DeepSeek 或自定义）")
            if (weekStarts.isEmpty()) return SyncReport(nothingToDo = true)

            val synced = mutableListOf<String>()
            val skipped = mutableListOf<String>()
            val failed = mutableListOf<Pair<String, String>>()

            for (startMillis in weekStarts.toSortedSet()) {
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

                // Nothing changed and not forced → don't waste an upload.
                if (!force && snapshot.none { it.syncDirty }) continue

                // Emptying a week uploads a "本周暂无内容" file (WeekRenderer handles empty live);
                // we intentionally do NOT delete the remote file. The app stays the source of truth.
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
