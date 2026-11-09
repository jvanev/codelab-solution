package com.sap.codelab

import com.sap.codelab.model.Memo
import com.sap.codelab.repository.IMemoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeMemoRepository : IMemoRepository {

    private val memos = MutableStateFlow<List<Memo>>(emptyList())
    private var nextId = 1L

    var failOnSave = false

    var saveCount = 0
        private set

    val savedMemos get() = memos.value

    override suspend fun saveMemo(memo: Memo): Long {
        saveCount++

        if (failOnSave) {
            throw IllegalStateException("Database failure")
        }

        val id = if (memo.id == 0L) nextId++ else memo.id
        memos.value = memos.value.filter { it.id != id } + memo.copy(id = id)

        return id
    }

    override fun getAll(): Flow<List<Memo>> = memos

    override fun getOpen(): Flow<List<Memo>> = memos.map { memosList ->
        memosList.filter { memo -> !memo.isDone }
    }

    override suspend fun getMemoById(id: Long): Memo? = memos.value.find { memo ->
        memo.id == id
    }
}