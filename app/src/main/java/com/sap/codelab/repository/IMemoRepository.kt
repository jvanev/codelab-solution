package com.sap.codelab.repository

import com.sap.codelab.model.Memo
import kotlinx.coroutines.flow.Flow

/**
 * Interface for a repository offering memo related CRUD operations.
 */
internal interface IMemoRepository {

    /**
     * Saves the given memo to the database.
     *
     * @return the id of the new database entry.
     */
    suspend fun saveMemo(memo: Memo): Long

    /**
     * @return all memos currently in the database.
     */
    fun getAll(): Flow<List<Memo>>

    /**
     * @return all memos currently in the database, except those that have been marked as "done".
     */
    fun getOpen(): Flow<List<Memo>>

    /**
     * @return the memo whose id matches the given id.
     */
    suspend fun getMemoById(id: Long): Memo?
}