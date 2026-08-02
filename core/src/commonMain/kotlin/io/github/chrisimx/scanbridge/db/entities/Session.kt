package io.github.chrisimx.scanbridge.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.scanbridge.model.ScanSettingsEnterableDataV1
import kotlin.uuid.Uuid

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey
    val sessionId: Uuid,
    val currentScanSettings: CommonScanSettings?,
    @ColumnInfo(defaultValue = "null")
    val currentSettingsUIData: ScanSettingsEnterableDataV1?,
    val currentPage: Int = 0
)
