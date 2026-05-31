package com.gulshid.noteapp.presentation.viewmodel

import androidx.lifecycle.*
import com.gulshid.noteapp.domain.model.Note
import com.gulshid.noteapp.domain.usecase.NoteUseCases
import com.gulshid.noteapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder { DATE_DESC, DATE_ASC, TITLE_ASC, TITLE_DESC }
enum class ViewMode { GRID, LIST }

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val viewMode: ViewMode = ViewMode.GRID,
    val isSearchActive: Boolean = false
)

sealed class HomeEvent {
    data class ShowSnackbar(val message: String) : HomeEvent()
    data class NavigateToDetail(val noteId: Int = -1) : HomeEvent()
    object NoteDeleted : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val useCases: NoteUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private var notesJob: Job? = null
    private var recentlyDeletedNote: Note? = null

    init {
        loadNotes()
    }

    private fun loadNotes() {
        notesJob?.cancel()
        notesJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            useCases.getAllNotes()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { notes ->
                    val sorted = sortNotes(notes, _uiState.value.sortOrder)
                    _uiState.update { it.copy(notes = sorted, isLoading = false, error = null) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            loadNotes()
        } else {
            notesJob?.cancel()
            notesJob = viewModelScope.launch {
                useCases.searchNotes(query)
                    .catch { e -> _uiState.update { it.copy(error = e.message) } }
                    .collect { notes ->
                        val sorted = sortNotes(notes, _uiState.value.sortOrder)
                        _uiState.update { it.copy(notes = sorted) }
                    }
            }
        }
    }

    fun toggleSearch() {
        val isActive = !_uiState.value.isSearchActive
        _uiState.update { it.copy(isSearchActive = isActive, searchQuery = "") }
        if (!isActive) loadNotes()
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { state ->
            state.copy(
                sortOrder = order,
                notes = sortNotes(state.notes, order)
            )
        }
    }

    fun toggleViewMode() {
        _uiState.update { state ->
            state.copy(viewMode = if (state.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            recentlyDeletedNote = note
            useCases.deleteNote(note)
            _events.emit(HomeEvent.ShowSnackbar("Note deleted"))
            _events.emit(HomeEvent.NoteDeleted)
        }
    }

    fun restoreNote() {
        viewModelScope.launch {
            recentlyDeletedNote?.let { note ->
                useCases.insertNote(note)
                recentlyDeletedNote = null
            }
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            useCases.togglePin(note.id, !note.isPinned)
            val message = if (!note.isPinned) "Note pinned" else "Note unpinned"
            _events.emit(HomeEvent.ShowSnackbar(message))
        }
    }

    private fun sortNotes(notes: List<Note>, order: SortOrder): List<Note> {
        val pinned = notes.filter { it.isPinned }
        val unpinned = notes.filter { !it.isPinned }

        fun sort(list: List<Note>) = when (order) {
            SortOrder.DATE_DESC -> list.sortedByDescending { it.updatedAt }
            SortOrder.DATE_ASC -> list.sortedBy { it.updatedAt }
            SortOrder.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC -> list.sortedByDescending { it.title.lowercase() }
        }

        return sort(pinned) + sort(unpinned)
    }
}
