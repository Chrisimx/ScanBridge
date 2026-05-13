package io.github.chrisimx.scanbridge.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.chrisimx.scanbridge.db.entities.ExecutedMigrationToRoom

@Dao
interface ExecutedMigrationsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun markAsExecuted(migration: ExecutedMigrationToRoom)

    @Query("SELECT * FROM executedmigrationtoroom WHERE migrationId = :id")
    fun getExecutedMigration(id: String): ExecutedMigrationToRoom?

    fun isAlreadyDone(id: String): Boolean = getExecutedMigration(id) != null
}
