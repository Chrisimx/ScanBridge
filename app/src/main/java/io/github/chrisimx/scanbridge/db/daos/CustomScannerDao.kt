package io.github.chrisimx.scanbridge.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.flow.Flow

@Dao
@OptIn(ExperimentalUuidApi::class)
interface CustomScannerDao {
    @Query("SELECT * FROM customscanners")
    fun getAllFlow(): Flow<List<CustomScanner>>

    @Query("SELECT * FROM customscanners")
    suspend fun getAll(): List<CustomScanner>

    @Insert
    suspend fun insertAll(vararg scanners: CustomScanner)

    @Update
    suspend fun update(vararg users: CustomScanner)

    @Delete
    suspend fun delete(scanner: CustomScanner)
}
