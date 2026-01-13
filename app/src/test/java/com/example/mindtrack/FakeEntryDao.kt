package com.example.mindtrack.fakes

import com.example.mindtrack.data.local.Entry
import com.example.mindtrack.data.local.EntryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeEntryDao : EntryDao {
    val entries = MutableStateFlow<List<Entry>>(emptyList())

    override fun observeAll(): Flow<List<Entry>> = entries

    override fun observe(id: Long): Flow<Entry?> = entries.map { list ->
        list.firstOrNull { it.id == id }
    }

    override suspend fun upsert(entry: Entry): Long {
        val updatedList = entries.value.filterNot { it.id == entry.id } + entry
        entries.value = updatedList
        return entry.id
    }

    override suspend fun deleteById(id: Long) {
        entries.update { list -> list.filterNot { it.id == id } }
    }
}
