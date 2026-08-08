package io.github.chrisimx.scanbridge.savelastusedscansettings

import io.github.chrisimx.scanbridge.db.entities.LastUsedScanSettings

interface D2RLastUsedScanSettingsMigrationSource {
    suspend fun getLastUsedScanSettings(): LastUsedScanSettings?
}
