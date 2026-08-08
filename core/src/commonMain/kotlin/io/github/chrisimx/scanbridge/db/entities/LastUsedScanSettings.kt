package io.github.chrisimx.scanbridge.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.scanbridge.model.ScanSettingsEnterableDataV1

@Entity(tableName = "lastusedscansettings")
data class LastUsedScanSettings(
    @PrimaryKey
    val id: Int = 1,
    val lastUsedScanSettings: CommonScanSettings,
    val lastUsedScanSettingsEnterable: ScanSettingsEnterableDataV1
)
