package io.github.chrisimx.scanbridge.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.chrisimx.scanbridge.db.entities.ExecutedMigrationToRoom
import kotlinx.coroutines.runBlocking

@Dao
interface ExecutedMigrationsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markAsExecuted(migration: ExecutedMigrationToRoom)

    @Query("SELECT * FROM executedmigrationtoroom WHERE migrationId = :id")
    suspend fun getExecutedMigration(id: String): ExecutedMigrationToRoom?

    fun isAlreadyDone(id: String): Boolean = runBlocking {
        getExecutedMigration(id) != null
    }
}
