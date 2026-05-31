package com.gulshid.noteapp.presentation.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gulshid.noteapp.R
import com.gulshid.noteapp.data.local.entity.Priority
import com.gulshid.noteapp.databinding.ItemNoteGridBinding
import com.gulshid.noteapp.databinding.ItemNoteListBinding
import com.gulshid.noteapp.domain.model.Note
import com.gulshid.noteapp.presentation.viewmodel.ViewMode
import com.gulshid.noteapp.utils.DateUtils
import com.gulshid.noteapp.utils.NoteColorUtils

class NotesAdapter(
    private var viewMode: ViewMode = ViewMode.GRID,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit,
    private val onPinClick: (Note) -> Unit
) : ListAdapter<Note, RecyclerView.ViewHolder>(NoteDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_GRID = 0
        private const val VIEW_TYPE_LIST = 1
    }

    fun updateViewMode(mode: ViewMode) {
        viewMode = mode
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (viewMode == ViewMode.GRID) VIEW_TYPE_GRID else VIEW_TYPE_LIST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_GRID -> GridNoteViewHolder(
                ItemNoteGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> ListNoteViewHolder(
                ItemNoteListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val note = getItem(position)
        when (holder) {
            is GridNoteViewHolder -> holder.bind(note)
            is ListNoteViewHolder -> holder.bind(note)
        }
    }

    inner class GridNoteViewHolder(
        private val binding: ItemNoteGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.apply {
                tvTitle.text = note.title
                tvPreview.text = note.preview
                tvDate.text = DateUtils.formatDate(note.updatedAt)
                cardNote.setCardBackgroundColor(NoteColorUtils.getColor(root.context, note.color))
                ivPin.isActivated = note.isPinned

                priorityIndicator.visibility = if (note.priority != Priority.NONE)
                    android.view.View.VISIBLE else android.view.View.GONE
                priorityIndicator.setBackgroundColor(
                    NoteColorUtils.getPriorityColor(root.context, note.priority)
                )

                if (note.label.isNotBlank()) {
                    tvLabel.visibility = android.view.View.VISIBLE
                    tvLabel.text = note.label
                } else {
                    tvLabel.visibility = android.view.View.GONE
                }

                root.setOnClickListener { onNoteClick(note) }
                root.setOnLongClickListener { onNoteLongClick(note); true }
                ivPin.setOnClickListener { onPinClick(note) }
            }
        }
    }

    inner class ListNoteViewHolder(
        private val binding: ItemNoteListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.apply {
                tvTitle.text = note.title
                tvPreview.text = note.preview
                tvDate.text = DateUtils.formatDate(note.updatedAt)
                tvWordCount.text = "${note.wordCount} words"
                cardNote.setCardBackgroundColor(NoteColorUtils.getColor(root.context, note.color))
                ivPin.isActivated = note.isPinned

                priorityChip.visibility = if (note.priority != Priority.NONE)
                    android.view.View.VISIBLE else android.view.View.GONE
                if (note.priority != Priority.NONE) {
                    priorityChip.text = note.priority.name
                }

                if (note.label.isNotBlank()) {
                    tvLabel.visibility = android.view.View.VISIBLE
                    tvLabel.text = note.label
                } else {
                    tvLabel.visibility = android.view.View.GONE
                }

                root.setOnClickListener { onNoteClick(note) }
                root.setOnLongClickListener { onNoteLongClick(note); true }
                ivPin.setOnClickListener { onPinClick(note) }
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
    }
}
