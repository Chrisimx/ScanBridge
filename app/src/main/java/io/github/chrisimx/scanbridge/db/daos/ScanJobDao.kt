package io.github.chrisimx.scanbridge.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import io.github.chrisimx.scanbridge.db.entities.ScanJob
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanJobDao {
    @Query("SELECT * FROM scanjobs")
    fun getAllFlow(): Flow<List<ScanJob>>

    @Query("SELECT * FROM scanjobs")
    suspend fun getAll(): List<ScanJob>

    @Query("SELECT * FROM scanjobs WHERE ownerSessionId = :session")
    fun getAllForSession(session: Uuid): Flow<List<ScanJob>>

    @Insert
    suspend fun insertAll(vararg jobs: ScanJob)

    @Delete
    suspend fun delete(jobs: ScanJob)
}
