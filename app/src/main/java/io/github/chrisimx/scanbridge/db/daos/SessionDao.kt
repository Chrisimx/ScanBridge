package io.github.chrisimx.scanbridge.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.chrisimx.scanbridge.db.entities.Session
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions")
    fun getAllFlow(): Flow<List<Session>>

    @Query("SELECT * FROM sessions")
    suspend fun getAll(): List<Session>

    @Query("SELECT * FROM sessions WHERE sessionId = :id")
    fun getSessionFlowById(id: Uuid): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE sessionId = :id")
    suspend fun getSessionById(id: Uuid): Session?

    @Insert
    suspend fun insertAll(vararg sessions: Session)

    @Update
    suspend fun update(vararg sessions: Session)

    @Delete
    suspend fun delete(session: Session)

    @Query("DELETE FROM sessions WHERE sessionId = :id")
    suspend fun deleteById(id: Uuid)
}

