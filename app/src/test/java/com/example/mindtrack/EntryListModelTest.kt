package com.example.mindtrack.ui.list

import com.example.mindtrack.MainDispatcherRule
import com.example.mindtrack.data.local.Entry
import com.example.mindtrack.data.repo.EntryRepository
import com.example.mindtrack.fakes.FakeEntryDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntryListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `query filters entries across all searchable fields`() = runTest(mainDispatcherRule.testDispatcher) {
        val dao = FakeEntryDao()
        val repo = EntryRepository(dao)
        dao.entries.value = listOf(
            Entry(1, "Morning Run", "Jogged around the park", 5, "fitness,outdoors", null, 1L, 1L),
            Entry(2, "Mindfulness", "Practiced breathing", 4, "calm,focus", null, 2L, 2L),
            Entry(3, "Reading", "Finished sci-fi novel", 3, "books,fiction", null, 3L, 3L)
        )

        val viewModel = EntryListViewModel(repo)
        val observed = mutableListOf<List<Entry>>()
        val job = launch { viewModel.entries.collect { observed.add(it) } }

        advanceUntilIdle()
        assertEquals("Initial emission should include all entries", 3, observed.last().size)

        viewModel.setQuery("park")
        advanceUntilIdle()
        assertEquals(listOf(dao.entries.value[0]), observed.last())

        viewModel.setQuery("CALM")
        advanceUntilIdle()
        assertEquals(listOf(dao.entries.value[1]), observed.last())

        viewModel.setQuery("fiction")
        advanceUntilIdle()
        assertEquals(listOf(dao.entries.value[2]), observed.last())

        viewModel.setQuery("")
        advanceUntilIdle()
        assertEquals(dao.entries.value, observed.last())

        job.cancel()
    }

    @Test
    fun `entries emit all items when query blank`() = runTest(mainDispatcherRule.testDispatcher) {
        val dao = FakeEntryDao()
        val repo = EntryRepository(dao)
        val items = listOf(
            Entry(1, "One", "Body", 3, "alpha", null, 1L, 1L),
            Entry(2, "Two", "Body", 3, "beta", null, 2L, 2L)
        )
        dao.entries.value = items

        val viewModel = EntryListViewModel(repo)
        val observed = mutableListOf<List<Entry>>()
        val job = launch { viewModel.entries.collect { observed.add(it) } }

        advanceUntilIdle()

        assertEquals(items, observed.last())

        job.cancel()
    }

    @Test
    fun `filtered entries update when source changes`() = runTest(mainDispatcherRule.testDispatcher) {
        val dao = FakeEntryDao()
        val repo = EntryRepository(dao)
        dao.entries.value = listOf(Entry(1, "Morning", "Body", 2, "calm", null, 1L, 1L))

        val viewModel = EntryListViewModel(repo)
        val observed = mutableListOf<List<Entry>>()
        val job = launch { viewModel.entries.collect { observed.add(it) } }

        viewModel.setQuery("run")
        advanceUntilIdle()

        dao.entries.value = dao.entries.value + Entry(2, "Run Club", "Evening jog", 4, "fitness", null, 2L, 2L)
        advanceUntilIdle()

        assertEquals(listOf(dao.entries.value.last()), observed.last())

        job.cancel()
    }

    @Test
    fun `whitespace query behaves as blank`() = runTest(mainDispatcherRule.testDispatcher) {
        val dao = FakeEntryDao()
        val repo = EntryRepository(dao)
        dao.entries.value = listOf(
            Entry(1, "Mindfulness", "Calm", 4, "focus", null, 1L, 1L)
        )

        val viewModel = EntryListViewModel(repo)
        val observed = mutableListOf<List<Entry>>()
        val job = launch { viewModel.entries.collect { observed.add(it) } }

        viewModel.setQuery("   ")
        advanceUntilIdle()

        assertEquals(dao.entries.value, observed.last())

        job.cancel()
    }
}
