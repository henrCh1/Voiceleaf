package com.v2j.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.v2j.app.data.Entry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What a running sync is operating on — so only the relevant button shows a spinner. */
sealed interface SyncTarget {
    data object All : SyncTarget
    data class Week(val weekStart: Long) : SyncTarget
    data object CatchUp : SyncTarget
}

sealed interface SyncUi {
    data object Idle : SyncUi
    data class Running(val target: SyncTarget) : SyncUi
    data class Done(val message: String) : SyncUi
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val repo: JournalRepository) : ViewModel() {

    // Re-pointed on every foreground so a process that lived across a week boundary shows the
    // correct week instead of staying stuck on the week it was launched in.
    private val weekAnchor = MutableStateFlow(System.currentTimeMillis())

    val entries: StateFlow<List<Entry>> = weekAnchor
        .flatMapLatest { repo.entriesForWeek(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All live entries (newest first) — for the History screen. */
    val allEntries: StateFlow<List<Entry>> = repo.allEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val _sync = MutableStateFlow<SyncUi>(SyncUi.Idle)
    val sync: StateFlow<SyncUi> = _sync.asStateFlow()

    private var lastCatchUp = 0L

    init {
        viewModelScope.launch { _draft.value = repo.loadDraft() }
        // Debounced autosave: nothing is lost even before 完成 or if the app is killed.
        viewModelScope.launch {
            _draft.debounce(500).collectLatest { repo.saveDraft(it) }
        }
    }

    /** Called when the app comes to the foreground (lifecycle ON_START). */
    fun onForegrounded() {
        // Re-point the current-week query in case we crossed midnight / a week boundary while alive.
        weekAnchor.value = System.currentTimeMillis()
        // Throttled, silent catch-up of PAST un-synced weeks (current week stays manual).
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastCatchUp < 60_000) return
        if (_sync.value is SyncUi.Running) return
        lastCatchUp = nowMs
        viewModelScope.launch {
            _sync.value = SyncUi.Running(SyncTarget.CatchUp)
            val report = repo.catchUpPastWeeks()
            // Stay silent unless something actually happened (don't nag on every app open).
            val hasNews = report.synced.isNotEmpty() || report.failed.isNotEmpty() || report.skipped.isNotEmpty()
            _sync.value = if (hasNews) SyncUi.Done(report.message()) else SyncUi.Idle
        }
    }

    fun onDraftChange(text: String) {
        _draft.value = text
    }

    fun onFinish() {
        val text = _draft.value.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            // Persist the entry FIRST, then clear the draft — no lost-text window if killed mid-way.
            val id = repo.addEntry(text)
            _draft.value = ""
            repo.clearDraft()
            repo.polishEntry(id)
        }
    }

    fun onDelete(entry: Entry) {
        viewModelScope.launch { repo.softDelete(entry) }
    }

    fun onRetry(entry: Entry) {
        viewModelScope.launch { repo.polishEntry(entry.id) }
    }

    /** User hand-edit of an entry's finished text (home 本周记录 + history). */
    fun onEdit(entry: Entry, newText: String) {
        viewModelScope.launch { repo.editEntry(entry.id, newText) }
    }

    fun onSync() {
        if (_sync.value is SyncUi.Running) return
        viewModelScope.launch {
            _sync.value = SyncUi.Running(SyncTarget.All)
            val report = repo.sync()
            _sync.value = SyncUi.Done(report.message())
        }
    }

    /** History 单周同步 (force re-render & re-upload that week). */
    fun onSyncWeek(weekStart: Long) {
        if (_sync.value is SyncUi.Running) return
        viewModelScope.launch {
            _sync.value = SyncUi.Running(SyncTarget.Week(weekStart))
            val report = repo.syncWeek(weekStart)
            _sync.value = SyncUi.Done(report.message())
        }
    }

    fun onSyncMessageShown() {
        _sync.value = SyncUi.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as V2JApp
                MainViewModel(app.container.repository)
            }
        }
    }
}
