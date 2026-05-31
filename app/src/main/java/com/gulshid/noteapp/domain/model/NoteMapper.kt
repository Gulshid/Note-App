package com.gulshid.noteapp.domain.model

import com.gulshid.noteapp.data.local.entity.NoteEntity

fun NoteEntity.toNote(): Note = Note(
    id = id,
    title = title,
    content = content,
    color = color,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    priority = priority,
    label = label
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    color = color,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    priority = priority,
    label = label
)
