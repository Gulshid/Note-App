package com.gulshid.noteapp.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.gulshid.noteapp.R
import com.gulshid.noteapp.data.local.entity.Priority

object NoteColorUtils {

    private val colorResIds = listOf(
        R.color.note_color_default,
        R.color.note_color_red,
        R.color.note_color_orange,
        R.color.note_color_yellow,
        R.color.note_color_green,
        R.color.note_color_teal,
        R.color.note_color_blue,
        R.color.note_color_purple,
        R.color.note_color_pink
    )

    fun getNoteColors(context: Context): List<Int> =
        colorResIds.map { ContextCompat.getColor(context, it) }

    fun getColor(context: Context, colorIndex: Int): Int {
        val index = colorIndex.coerceIn(0, colorResIds.size - 1)
        return ContextCompat.getColor(context, colorResIds[index])
    }

    fun getPriorityColor(context: Context, priority: Priority): Int = when (priority) {
        Priority.HIGH -> ContextCompat.getColor(context, R.color.priority_high)
        Priority.MEDIUM -> ContextCompat.getColor(context, R.color.priority_medium)
        Priority.LOW -> ContextCompat.getColor(context, R.color.priority_low)
        Priority.NONE -> ContextCompat.getColor(context, android.R.color.transparent)
    }
}

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
