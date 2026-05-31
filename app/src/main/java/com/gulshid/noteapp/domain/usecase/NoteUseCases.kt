package com.gulshid.noteapp.domain.usecase

import com.gulshid.noteapp.domain.model.InvalidNoteException
import com.gulshid.noteapp.domain.model.Note
import com.gulshid.noteapp.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Get all notes use case
class GetAllNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<Note>> = repository.getAllNotes()
}

// Get note by id
class GetNoteByIdUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(id: Int): Note? = repository.getNoteById(id)
}

// Search notes
class SearchNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(query: String): Flow<List<Note>> = repository.searchNotes(query)
}

// Insert note
class InsertNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    @Throws(InvalidNoteException::class)
    suspend operator fun invoke(note: Note): Long {
        if (note.title.isBlank()) throw InvalidNoteException.EmptyTitle()
        return repository.insertNote(note.copy(updatedAt = System.currentTimeMillis()))
    }
}

// Update note
class UpdateNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    @Throws(InvalidNoteException::class)
    suspend operator fun invoke(note: Note) {
        if (note.title.isBlank()) throw InvalidNoteException.EmptyTitle()
        repository.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    }
}

// Delete note
class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note) = repository.deleteNote(note)
    suspend operator fun invoke(id: Int) = repository.deleteNoteById(id)
}

// Toggle pin
class TogglePinNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(id: Int, isPinned: Boolean) =
        repository.updatePinStatus(id, isPinned)
}

// Get pinned notes
class GetPinnedNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<Note>> = repository.getPinnedNotes()
}

// Bundled use cases wrapper for cleaner injection
data class NoteUseCases @Inject constructor(
    val getAllNotes: GetAllNotesUseCase,
    val getNoteById: GetNoteByIdUseCase,
    val searchNotes: SearchNotesUseCase,
    val insertNote: InsertNoteUseCase,
    val updateNote: UpdateNoteUseCase,
    val deleteNote: DeleteNoteUseCase,
    val togglePin: TogglePinNoteUseCase,
    val getPinnedNotes: GetPinnedNotesUseCase
)
