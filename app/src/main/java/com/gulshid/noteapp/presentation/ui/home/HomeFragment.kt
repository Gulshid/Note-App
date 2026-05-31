package com.gulshid.noteapp.presentation.ui.home

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.gulshid.noteapp.R
import com.gulshid.noteapp.databinding.FragmentHomeBinding
import com.gulshid.noteapp.domain.model.Note
import com.gulshid.noteapp.presentation.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var notesAdapter: NotesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        setupToolbar()
        observeUiState()
        observeEvents()
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            onNoteClick = { note -> navigateToDetail(note.id) },
            onNoteLongClick = { note -> showNoteOptionsDialog(note) },
            onPinClick = { note -> viewModel.togglePin(note) }
        )
        binding.rvNotes.apply {
            adapter = notesAdapter
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            setHasFixedSize(true)
        }
    }

    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            navigateToDetail(-1)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    viewModel.toggleSearch()
                    true
                }
                R.id.action_view_mode -> {
                    viewModel.toggleViewMode()
                    true
                }
                R.id.action_sort -> {
                    showSortDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is HomeEvent.ShowSnackbar -> showSnackbar(event.message)
                        is HomeEvent.NavigateToDetail -> navigateToDetail(event.noteId)
                        is HomeEvent.NoteDeleted -> showUndoSnackbar()
                    }
                }
            }
        }
    }

    private fun renderState(state: HomeUiState) {
        // Loading
        binding.progressBar.isVisible = state.isLoading

        // Empty state
        binding.layoutEmpty.isVisible = state.notes.isEmpty() && !state.isLoading

        // Notes list
        binding.rvNotes.isVisible = state.notes.isNotEmpty()
        notesAdapter.submitList(state.notes)
        notesAdapter.updateViewMode(state.viewMode)

        // Note count
        binding.tvNoteCount.text = "${state.notes.size} notes"

        // View mode icon
        val viewModeIcon = if (state.viewMode == ViewMode.GRID) R.drawable.ic_list else R.drawable.ic_grid
        binding.toolbar.menu.findItem(R.id.action_view_mode)?.setIcon(viewModeIcon)

        // Layout manager
        val layoutManager = if (state.viewMode == ViewMode.GRID) {
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        } else {
            LinearLayoutManager(requireContext())
        }
        binding.rvNotes.layoutManager = layoutManager

        // Search bar
        binding.searchBar.isVisible = state.isSearchActive
        if (!state.isSearchActive) binding.searchBar.setQuery("", false)

        binding.searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.onSearchQueryChange(newText ?: "")
                return true
            }
        })
    }

    private fun showNoteOptionsDialog(note: Note) {
        val options = arrayOf(
            if (note.isPinned) "Unpin note" else "Pin note",
            "Delete note"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(note.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.togglePin(note)
                    1 -> showDeleteConfirmation(note)
                }
            }
            .show()
    }

    private fun showDeleteConfirmation(note: Note) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete \"${note.title}\"?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteNote(note) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf("Newest first", "Oldest first", "Title A-Z", "Title Z-A")
        val currentSort = viewModel.uiState.value.sortOrder.ordinal
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sort Notes")
            .setSingleChoiceItems(sortOptions, currentSort) { dialog, which ->
                viewModel.setSortOrder(SortOrder.values()[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showUndoSnackbar() {
        Snackbar.make(binding.root, "Note deleted", Snackbar.LENGTH_LONG)
            .setAction("Undo") { viewModel.restoreNote() }
            .show()
    }

    private fun navigateToDetail(noteId: Int) {
        val action = HomeFragmentDirections.actionHomeToDetail(noteId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
