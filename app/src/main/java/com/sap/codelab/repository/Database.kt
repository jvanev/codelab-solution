package com.sap.codelab.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sap.codelab.model.Memo

/**
 * That database that is used to store information.
 */
@Database(entities = [Memo::class], version = 2, exportSchema = false)
internal abstract class Database : RoomDatabase() {

    abstract fun getMemoDao(): MemoDao
}