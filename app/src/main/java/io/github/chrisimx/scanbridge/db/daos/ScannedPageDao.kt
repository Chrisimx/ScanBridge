package io.github.chrisimx.scanbridge.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

@Dao
interface ScannedPageDao {
    @Query("SELECT * FROM scannedpages ORDER BY orderIndex ASC")
    fun getAllFlow(): Flow<List<ScannedPage>>

    @Query("SELECT * FROM scannedpages ORDER BY orderIndex ASC")
    suspend fun getAll(): List<ScannedPage>

    @Query("SELECT * FROM scannedpages WHERE ownerSessionId = :session ORDER BY orderIndex ASC")
    fun getAllForSessionFlow(session: Uuid): Flow<List<ScannedPage>>

    @Query("SELECT * FROM scannedpages WHERE ownerSessionId = :session ORDER BY orderIndex ASC")
    suspend fun getAllForSession(session: Uuid): List<ScannedPage>

    @Query("SELECT * FROM scannedpages WHERE scanId = :id")
    fun getByScanIdFlow(id: Uuid): Flow<ScannedPage>

    @Query("SELECT * FROM scannedpages WHERE scanId = :id")
    suspend fun getByScanId(id: Uuid): ScannedPage?

    @Query("SELECT MAX(orderIndex) FROM scannedpages WHERE ownerSessionId = :session")
    suspend fun getHighestIdxForSession(session: Uuid): Int?

    @Transaction
    suspend fun swapPages(page1: ScannedPage, page2: ScannedPage) {
        val page1 = getByScanId(page1.scanId)
        val page2 = getByScanId(page2.scanId)

        if (page1 == null || page2 == null) {
            Timber.e("Could not swap pages: $page1, $page2")
            return
        }

        updateOrderIndex(page1.scanId, -1)
        updateOrderIndex(page2.scanId, page1.orderIndex)
        updateOrderIndex(page1.scanId, page2.orderIndex)
    }

    @Query("UPDATE scannedpages SET orderIndex = :newIndex WHERE scanId = :scanId")
    suspend fun updateOrderIndex(scanId: Uuid, newIndex: Int)

    @Insert
    suspend fun insertAll(vararg pages: ScannedPage)

    @Insert
    suspend fun insertAllList(pages: List<ScannedPage>)

    @Update
    suspend fun update(vararg pages: ScannedPage)

    @Delete
    suspend fun delete(page: ScannedPage)
}
