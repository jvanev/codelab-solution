package com.sap.codelab.repository

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.sap.codelab.model.Memo
import kotlinx.coroutines.flow.Flow

private const val DATABASE_NAME: String = "codelab"

/**
 * The repository is used to retrieve data from a data source.
 */
internal object MemoRepository : IMemoRepository {

    private lateinit var database: Database

    fun initialize(applicationContext: Context) {
        database = Room.databaseBuilder(applicationContext, Database::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    override suspend fun saveMemo(memo: Memo): Long {
        return database.getMemoDao().insert(memo)
    }

    override fun getOpen(): Flow<List<Memo>> = database.getMemoDao().getOpen()

    override fun getAll(): Flow<List<Memo>> = database.getMemoDao().getAll()

    override suspend fun getMemoById(id: Long): Memo? = database.getMemoDao().getMemoById(id)

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE memo_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    reminderDate INTEGER NOT NULL,
                    reminderLatitude REAL NOT NULL,
                    reminderLongitude REAL NOT NULL,
                    isDone INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                INSERT INTO memo_new (
                    id,
                    title,
                    description,
                    reminderDate,
                    reminderLatitude,
                    reminderLongitude,
                    isDone
                )
                SELECT
                    id,
                    title,
                    description,
                    reminderDate,
                    CAST(reminderLatitude AS REAL),
                    CAST(reminderLongitude AS REAL),
                    isDone
                FROM memo
                """.trimIndent(),
            )
            connection.execSQL("DROP TABLE memo")
            connection.execSQL("ALTER TABLE memo_new RENAME TO memo")
        }
    }
}