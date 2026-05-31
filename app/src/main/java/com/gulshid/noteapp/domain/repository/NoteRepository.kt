package com.gulshid.noteapp.domain.repository

import com.gulshid.noteapp.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
    fun getNotesByLabel(label: String): Flow<List<Note>>
    fun getPinnedNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: Int): Note?
    suspend fun insertNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun deleteNoteById(id: Int)
    suspend fun updatePinStatus(id: Int, isPinned: Boolean)
}
