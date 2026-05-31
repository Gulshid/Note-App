package com.gulshid.noteapp.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gulshid.noteapp.data.local.entity.Priority
import com.gulshid.noteapp.domain.model.InvalidNoteException
import com.gulshid.noteapp.domain.model.Note
import com.gulshid.noteapp.domain.usecase.NoteUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteDetailUiState(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val color: Int = 0,
    val priority: Priority = Priority.NONE,
    val label: String = "",
    val isPinned: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

sealed class NoteDetailEvent {
    data class ShowMessage(val message: String) : NoteDetailEvent()
    object NoteSaved : NoteDetailEvent()
    object NavigateBack : NoteDetailEvent()
}

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val useCases: NoteUseCases,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NoteDetailEvent>()
    val events: SharedFlow<NoteDetailEvent> = _events.asSharedFlow()

    private var currentNoteId: Int = -1

    init {
        savedStateHandle.get<Int>("noteId")?.let { id ->
            if (id != -1) {
                currentNoteId = id
                loadNote(id)
            } else {
                _uiState.update { it.copy(isEditing = true) }
            }
        }
    }

    private fun loadNote(id: Int) {
        viewModelScope.launch {
            useCases.getNoteById(id)?.let { note ->
                _uiState.update {
                    NoteDetailUiState(
                        id = note.id,
                        title = note.title,
                        content = note.content,
                        color = note.color,
                        priority = note.priority,
                        label = note.label,
                        isPinned = note.isPinned,
                        isEditing = false,
                        createdAt = note.createdAt,
                        updatedAt = note.updatedAt
                    )
                }
            }
        }
    }

    fun onTitleChange(title: String) = _uiState.update { it.copy(title = title) }
    fun onContentChange(content: String) = _uiState.update { it.copy(content = content) }
    fun onColorChange(color: Int) = _uiState.update { it.copy(color = color) }
    fun onPriorityChange(priority: Priority) = _uiState.update { it.copy(priority = priority) }
    fun onLabelChange(label: String) = _uiState.update { it.copy(label = label) }
    fun onPinToggle() = _uiState.update { it.copy(isPinned = !it.isPinned) }
    fun setEditing(editing: Boolean) = _uiState.update { it.copy(isEditing = editing) }

    fun saveNote() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val state = _uiState.value
                val note = Note(
                    id = currentNoteId.takeIf { it != -1 } ?: 0,
                    title = state.title.trim(),
                    content = state.content.trim(),
                    color = state.color,
                    priority = state.priority,
                    label = state.label,
                    isPinned = state.isPinned,
                    createdAt = state.createdAt
                )

                if (currentNoteId == -1) {
                    useCases.insertNote(note)
                } else {
                    useCases.updateNote(note)
                }

                _uiState.update { it.copy(isSaving = false, isEditing = false) }
                _events.emit(NoteDetailEvent.NoteSaved)
                _events.emit(NoteDetailEvent.ShowMessage("Note saved"))
            } catch (e: InvalidNoteException) {
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(NoteDetailEvent.ShowMessage(e.message ?: "Error saving note"))
            }
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            if (currentNoteId != -1) {
                useCases.deleteNote(currentNoteId)
                _events.emit(NoteDetailEvent.ShowMessage("Note deleted"))
                _events.emit(NoteDetailEvent.NavigateBack)
            }
        }
    }
}
