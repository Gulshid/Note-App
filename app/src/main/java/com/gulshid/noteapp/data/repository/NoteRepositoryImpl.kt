package com.gulshid.noteapp.data.repository

import com.gulshid.noteapp.data.local.dao.NoteDao
import com.gulshid.noteapp.domain.model.Note
import com.gulshid.noteapp.domain.model.toEntity
import com.gulshid.noteapp.domain.model.toNote
import com.gulshid.noteapp.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val dao: NoteDao
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> =
        dao.getAllNotes().map { entities -> entities.map { it.toNote() } }

    override fun searchNotes(query: String): Flow<List<Note>> =
        dao.searchNotes(query).map { entities -> entities.map { it.toNote() } }

    override fun getNotesByLabel(label: String): Flow<List<Note>> =
        dao.getNotesByLabel(label).map { entities -> entities.map { it.toNote() } }

    override fun getPinnedNotes(): Flow<List<Note>> =
        dao.getPinnedNotes().map { entities -> entities.map { it.toNote() } }

    override suspend fun getNoteById(id: Int): Note? =
        dao.getNoteById(id)?.toNote()

    override suspend fun insertNote(note: Note): Long =
        dao.insertNote(note.toEntity())

    override suspend fun updateNote(note: Note) =
        dao.updateNote(note.toEntity())

    override suspend fun deleteNote(note: Note) =
        dao.deleteNote(note.toEntity())

    override suspend fun deleteNoteById(id: Int) =
        dao.deleteNoteById(id)

    override suspend fun updatePinStatus(id: Int, isPinned: Boolean) =
        dao.updatePinStatus(id, isPinned)
}
