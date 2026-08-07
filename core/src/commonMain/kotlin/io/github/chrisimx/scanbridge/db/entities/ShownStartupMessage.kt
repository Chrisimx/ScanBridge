package io.github.chrisimx.scanbridge.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.chrisimx.scanbridge.startupmessages.StartupMessage

@Entity(tableName = "shownstartupmessages")
data class ShownStartupMessage(
    @PrimaryKey
    val message: StartupMessage
)
