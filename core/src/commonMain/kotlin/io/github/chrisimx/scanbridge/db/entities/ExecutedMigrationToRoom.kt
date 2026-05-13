package io.github.chrisimx.scanbridge.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "executedmigrationtoroom")
data class ExecutedMigrationToRoom(
    @PrimaryKey
    val migrationId: String
)
