package io.github.chrisimx.scanbridge.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.chrisimx.esclkt.ScanSettings
import kotlin.uuid.Uuid

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey
    val sessionId: Uuid,
    val currentScanSettings: ScanSettings?,
)

