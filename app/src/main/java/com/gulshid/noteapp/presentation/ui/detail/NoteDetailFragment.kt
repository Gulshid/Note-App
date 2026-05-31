package com.gulshid.noteapp.presentation.ui.detail

import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.gulshid.noteapp.R
import com.gulshid.noteapp.data.local.entity.Priority
import com.gulshid.noteapp.databinding.FragmentNoteDetailBinding
import com.gulshid.noteapp.presentation.viewmodel.NoteDetailEvent
import com.gulshid.noteapp.presentation.viewmodel.NoteDetailViewModel
import com.gulshid.noteapp.utils.DateUtils
import com.gulshid.noteapp.utils.NoteColorUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NoteDetailFragment : Fragment() {

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NoteDetailViewModel by viewModels()
    private val args: NoteDetailFragmentArgs by navArgs()

    private val noteColors by lazy { NoteColorUtils.getNoteColors(requireContext()) }
    private var isNewNote = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isNewNote = args.noteId == -1
        setupToolbar()
        setupColorPicker()
        setupPriorityChips()
        observeUiState()
        observeEvents()
        if (isNewNote) enableEditing()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            if (viewModel.uiState.value.isEditing) {
                viewModel.saveNote()
            } else {
                findNavController().navigateUp()
            }
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> { enableEditing(); true }
                R.id.action_save -> { viewModel.saveNote(); true }
                R.id.action_pin -> { viewModel.onPinToggle(); true }
                R.id.action_delete -> { showDeleteConfirmation(); true }
                R.id.action_share -> { shareNote(); true }
                else -> false
            }
        }
    }

    private fun setupColorPicker() {
        binding.colorPickerLayout.removeAllViews()
        noteColors.forEachIndexed { index, color ->
            val view = View(requireContext()).apply {
                layoutParams = ViewGroup.MarginLayoutParams(80, 80).apply {
                    setMargins(8, 0, 8, 0)
                }
                setBackgroundResource(R.drawable.bg_color_circle)
                backgroundTintList = android.content.res.ColorStateList.valueOf(color)
                setOnClickListener { viewModel.onColorChange(index) }
            }
            binding.colorPickerLayout.addView(view)
        }
    }

    private fun setupPriorityChips() {
        Priority.values().filter { it != Priority.NONE }.forEach { priority ->
            val chip = Chip(requireContext()).apply {
                text = priority.name
                isCheckable = true
                setOnCheckedChangeListener { _, checked ->
                    if (checked) viewModel.onPriorityChange(priority)
                    else viewModel.onPriorityChange(Priority.NONE)
                }
            }
            binding.priorityChipGroup.addView(chip)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    // Title
                    if (binding.etTitle.text.toString() != state.title) {
                        binding.etTitle.setText(state.title)
                    }
                    // Content
                    if (binding.etContent.text.toString() != state.content) {
                        binding.etContent.setText(state.content)
                    }

                    // Date
                    binding.tvMetadata.text = "Last edited ${DateUtils.formatRelative(state.updatedAt)}"

                    // Background color
                    if (state.color < noteColors.size) {
                        binding.root.setBackgroundColor(noteColors[state.color])
                    }

                    // Pin state
                    val pinIcon = if (state.isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin
                    binding.toolbar.menu.findItem(R.id.action_pin)?.setIcon(pinIcon)

                    // Edit/view mode
                    val isEditing = state.isEditing
                    binding.etTitle.isEnabled = isEditing
                    binding.etContent.isEnabled = isEditing
                    binding.colorPickerCard.isVisible = isEditing
                    binding.priorityCard.isVisible = isEditing
                    binding.labelCard.isVisible = isEditing
                    binding.toolbar.menu.findItem(R.id.action_edit)?.isVisible = !isEditing && !isNewNote
                    binding.toolbar.menu.findItem(R.id.action_save)?.isVisible = isEditing

                    // Word count
                    val wordCount = state.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                    binding.tvWordCount.text = "$wordCount words • ${state.content.length} chars"

                    // Saving state
                    binding.progressSaving.isVisible = state.isSaving
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is NoteDetailEvent.ShowMessage ->
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                        is NoteDetailEvent.NoteSaved ->
                            findNavController().navigateUp()
                        is NoteDetailEvent.NavigateBack ->
                            findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private fun enableEditing() {
        viewModel.setEditing(true)
        binding.etTitle.requestFocus()
        binding.etTitle.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.onTitleChange(s.toString())
            }
        })
        binding.etContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.onContentChange(s.toString())
            }
        })
        binding.etLabel.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.onLabelChange(s.toString())
            }
        })
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Note")
            .setMessage("This note will be permanently deleted.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteNote() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareNote() {
        val state = viewModel.uiState.value
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, state.title)
            putExtra(android.content.Intent.EXTRA_TEXT, "${state.title}\n\n${state.content}")
        }
        startActivity(android.content.Intent.createChooser(intent, "Share Note"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
