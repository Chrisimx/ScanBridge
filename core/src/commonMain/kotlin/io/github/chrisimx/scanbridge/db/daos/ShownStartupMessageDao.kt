package io.github.chrisimx.scanbridge.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.chrisimx.scanbridge.db.entities.ShownStartupMessage
import io.github.chrisimx.scanbridge.startupmessages.StartupMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ShownStartupMessageDao {
    @Query("SELECT * FROM shownstartupmessages")
    fun getAllFlow(): Flow<List<ShownStartupMessage>>

    @Query(
        """
    SELECT EXISTS(
        SELECT 1
        FROM shownstartupmessages
        WHERE message = :message
    )
"""
    )
    fun exists(message: StartupMessage): Flow<Boolean>

    @Query("SELECT * FROM shownstartupmessages")
    suspend fun getAll(): List<ShownStartupMessage>

    @Delete
    suspend fun delete(startupMessage: ShownStartupMessage)

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insertAll(vararg startupMessages: ShownStartupMessage)
}
