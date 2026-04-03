package io.github.chrisimx.scanbridge.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsStateData
import kotlin.uuid.Uuid

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey
    val sessionId: Uuid,
    val currentScanSettings: ScanSettings?,
    @ColumnInfo(defaultValue = "null")
    val currentSettingsUIData: ScanSettingsStateData?,
    val currentPage: Int = 0
)
