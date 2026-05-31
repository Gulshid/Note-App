package com.gulshid.noteapp.data.local.dao

import androidx.room.*
import com.gulshid.noteapp.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY is_pinned DESC, updated_at DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteEntity?

    @Query("""
        SELECT * FROM notes 
        WHERE title LIKE '%' || :query || '%' 
        OR content LIKE '%' || :query || '%'
        ORDER BY is_pinned DESC, updated_at DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE label = :label ORDER BY is_pinned DESC, updated_at DESC")
    fun getNotesByLabel(label: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE priority = :priority ORDER BY updated_at DESC")
    fun getNotesByPriority(priority: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE is_pinned = 1 ORDER BY updated_at DESC")
    fun getPinnedNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNotesCount(): Int

    @Query("UPDATE notes SET is_pinned = :isPinned WHERE id = :id")
    suspend fun updatePinStatus(id: Int, isPinned: Boolean)

    @Transaction
    suspend fun upsertNote(note: NoteEntity): Long {
        return insertNote(note)
    }
}
