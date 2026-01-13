package com.example.mindtrack.data.repo

import com.example.mindtrack.data.local.Entry
import com.example.mindtrack.fakes.FakeEntryDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryRepositoryTest {

    @Test
    fun `upsert refreshes updatedAt before delegating to dao`() = runTest {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao)

        val base = Entry(
            id = 1,
            title = "Title",
            body = "Body",
            mood = 3,
            tagsCsv = "home,work",
            photoUri = null,
            createdAt = 100L,
            updatedAt = 200L
        )

        val returnedId = repository.upsert(base)

        val stored = dao.entries.value.single()
        assertEquals(base.id, returnedId)
        assertEquals("Stored entry should match DAO return", stored.id, returnedId)
        assertTrue("updatedAt should be refreshed", stored.updatedAt >= base.updatedAt)
        assertEquals(base.copy(updatedAt = stored.updatedAt), stored)
    }

    @Test
    fun `delete forwards call to dao`() = runTest {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao)
        val entry = Entry(1, "Title", "Body", 4, "tag", null, 10L, 10L)
        dao.upsert(entry)

        repository.delete(entry.id)

        assertTrue(dao.entries.value.isEmpty())
    }

    @Test
    fun `entries flow mirrors dao emissions`() = runTest {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao)
        val first = Entry(1, "A", "B", 1, "x", null, 1L, 1L)
        val second = Entry(2, "C", "D", 2, "y", null, 2L, 2L)

        val emissions = mutableListOf<List<Entry>>()
        val job = launch { repository.entries.collect { emissions.add(it) } }

        dao.entries.value = listOf(first)
        dao.entries.value = listOf(first, second)
        advanceUntilIdle()

        val filteredEmissions = emissions.filter { it.isNotEmpty() }
        assertEquals(listOf(first, second), filteredEmissions.last())

        job.cancel()
    }

    @Test
    fun `entry returns matching item`() = runTest {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao)
        val entry = Entry(5, "Title", "Body", 4, "tag", null, 10L, 10L)
        dao.entries.value = listOf(entry)

        val result = repository.entry(entry.id).first()

        assertEquals(entry, result)
    }

    @Test
    fun `entry emits null when item missing`() = runTest {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao)

        val result = repository.entry(99).first()

        assertEquals(null, result)
    }
}
