package io.github.chrisimx.scanbridge.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.chrisimx.scanbridge.db.entities.TempFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface TempFileDao {
    @Query("SELECT * FROM tempfiles")
    fun getAllFlow(): Flow<List<TempFile>>

    @Query("SELECT * FROM tempfiles")
    suspend fun getAll(): List<TempFile>

    @Query("SELECT * FROM tempfiles WHERE ownerSessionId = :sessionId")
    fun getFilesFlowBySessionId(sessionId: Uuid): Flow<List<TempFile>>

    @Query("SELECT * FROM tempfiles WHERE ownerSessionId = :sessionId")
    suspend fun getFilesBySessionId(sessionId: Uuid): List<TempFile>

    @Insert
    suspend fun insertAll(vararg files: TempFile)

    @Insert
    suspend fun insertAllList(files: List<TempFile>)

    @Update
    suspend fun update(vararg files: TempFile)

    @Delete
    suspend fun delete(session: TempFile)
}

