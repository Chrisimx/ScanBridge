package io.github.chrisimx.scanbridge.savelastusedscansettings

import io.github.chrisimx.scanbridge.db.entities.LastUsedScanSettings

interface LastUsedScanSettingsRepository {
    suspend fun getLastUsedScanSettings(): LastUsedScanSettings?
    suspend fun setLastUsedScanSettings(scanSettings: LastUsedScanSettings?)
}
