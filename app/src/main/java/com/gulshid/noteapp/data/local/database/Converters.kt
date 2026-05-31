package com.gulshid.noteapp.data.local.database

import androidx.room.TypeConverter
import com.gulshid.noteapp.data.local.entity.Priority

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)
}
