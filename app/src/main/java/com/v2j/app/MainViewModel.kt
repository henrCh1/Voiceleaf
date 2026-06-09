package com.v2j.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.v2j.app.data.Entry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SyncUi {
    data object Idle : SyncUi
    data object Running : SyncUi
    data class Done(val message: String) : SyncUi
}

class MainViewModel(private val repo: JournalRepository) : ViewModel() {

    val entries: StateFlow<List<Entry>> = repo.currentWeekEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All live entries (newest first) — for the History screen. */
    val allEntries: StateFlow<List<Entry>> = repo.allEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val _sync = MutableStateFlow<SyncUi>(SyncUi.Idle)
    val sync: StateFlow<SyncUi> = _sync.asStateFlow()

    init {
        viewModelScope.launch { _draft.value = repo.loadDraft() }
        // Debounced autosave: nothing is lost even before 完成 or if the app is killed.
        viewModelScope.launch {
            _draft.debounce(500).collectLatest { repo.saveDraft(it) }
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

    fun onSync() {
        if (_sync.value == SyncUi.Running) return
        viewModelScope.launch {
            _sync.value = SyncUi.Running
            val report = repo.sync()
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
