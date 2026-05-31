package com.gulshid.noteapp.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.gulshid.noteapp.domain.model.Note
import com.gulshid.noteapp.domain.usecase.*
import com.gulshid.noteapp.presentation.viewmodel.HomeViewModel
import com.gulshid.noteapp.presentation.viewmodel.SortOrder
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: HomeViewModel
    private val getAllNotes: GetAllNotesUseCase = mockk()
    private val searchNotes: SearchNotesUseCase = mockk()
    private val insertNote: InsertNoteUseCase = mockk()
    private val updateNote: UpdateNoteUseCase = mockk()
    private val deleteNote: DeleteNoteUseCase = mockk()
    private val getNoteById: GetNoteByIdUseCase = mockk()
    private val togglePin: TogglePinNoteUseCase = mockk()
    private val getPinnedNotes: GetPinnedNotesUseCase = mockk()

    private val testNotes = listOf(
        Note(id = 1, title = "Note 1", content = "Content 1"),
        Note(id = 2, title = "Note 2", content = "Content 2", isPinned = true),
        Note(id = 3, title = "Note 3", content = "Content 3")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getAllNotes() } returns flowOf(testNotes)

        val useCases = NoteUseCases(
            getAllNotes = getAllNotes,
            getNoteById = getNoteById,
            searchNotes = searchNotes,
            insertNote = insertNote,
            updateNote = updateNote,
            deleteNote = deleteNote,
            togglePin = togglePin,
            getPinnedNotes = getPinnedNotes
        )
        viewModel = HomeViewModel(useCases)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads notes correctly`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.notes.isNotEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `pinned notes appear first`() = runTest {
        advanceUntilIdle()
        val notes = viewModel.uiState.value.notes
        assertTrue(notes.first().isPinned)
    }

    @Test
    fun `sort order DATE_ASC sorts oldest first`() = runTest {
        advanceUntilIdle()
        viewModel.setSortOrder(SortOrder.DATE_ASC)
        val notes = viewModel.uiState.value.notes
        val unpinned = notes.filter { !it.isPinned }
        assertTrue(unpinned.zipWithNext().all { (a, b) -> a.updatedAt <= b.updatedAt })
    }

    @Test
    fun `sort order TITLE_ASC sorts alphabetically`() = runTest {
        advanceUntilIdle()
        viewModel.setSortOrder(SortOrder.TITLE_ASC)
        val notes = viewModel.uiState.value.notes
        val unpinned = notes.filter { !it.isPinned }
        assertTrue(unpinned.zipWithNext().all { (a, b) -> a.title <= b.title })
    }

    @Test
    fun `toggle search updates isSearchActive`() = runTest {
        assertFalse(viewModel.uiState.value.isSearchActive)
        viewModel.toggleSearch()
        assertTrue(viewModel.uiState.value.isSearchActive)
        viewModel.toggleSearch()
        assertFalse(viewModel.uiState.value.isSearchActive)
    }

    @Test
    fun `delete note stores recently deleted for undo`() = runTest {
        advanceUntilIdle()
        val note = testNotes[0]
        coEvery { deleteNote(note) } just runs
        viewModel.deleteNote(note)
        advanceUntilIdle()
        coVerify { deleteNote(note) }
    }

    @Test
    fun `restore note re-inserts deleted note`() = runTest {
        advanceUntilIdle()
        val note = testNotes[0]
        coEvery { deleteNote(note) } just runs
        coEvery { insertNote(note) } returns 1L
        viewModel.deleteNote(note)
        advanceUntilIdle()
        viewModel.restoreNote()
        advanceUntilIdle()
        coVerify { insertNote(note) }
    }
}
