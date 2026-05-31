package com.gulshid.noteapp.domain.model

import com.gulshid.noteapp.data.local.entity.Priority

data class Note(
    val id: Int = 0,
    val title: String,
    val content: String,
    val color: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val priority: Priority = Priority.NONE,
    val label: String = ""
) {
    val isEmpty: Boolean get() = title.isBlank() && content.isBlank()
    val preview: String get() = content.take(120).ifBlank { "No additional text" }
    val wordCount: Int get() = content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
}

sealed class InvalidNoteException(message: String) : Exception(message) {
    class EmptyTitle : InvalidNoteException("Title cannot be empty")
    class EmptyContent : InvalidNoteException("Content cannot be empty")
}
