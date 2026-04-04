package io.github.chrisimx.scanbridge.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lastroute")
data class LastRoute (
    val route: String,
    @PrimaryKey val id: Int = 1
)
