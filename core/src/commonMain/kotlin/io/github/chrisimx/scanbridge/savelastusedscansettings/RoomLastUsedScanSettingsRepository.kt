package io.github.chrisimx.scanbridge.savelastusedscansettings

import io.github.chrisimx.scanbridge.appsettings.AppSettingsRepository
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.LastUsedScanSettings

class RoomLastUsedScanSettingsRepository(scanBridgeDb: ScanBridgeDb, private val appSettingsRepository: AppSettingsRepository) :
    LastUsedScanSettingsRepository {
    private val lastUsedScanSettingsDao = scanBridgeDb.lastUsedScanSettingsDao()

    override suspend fun getLastUsedScanSettings(): LastUsedScanSettings? {
        val currentAppSettings = appSettingsRepository.getAppSettings()
        if (!currentAppSettings.rememberScanSettings) return null

        return lastUsedScanSettingsDao.getLastUsedScanSettings()
    }

    override suspend fun setLastUsedScanSettings(scanSettings: LastUsedScanSettings?) {
        if (scanSettings != null) {
            lastUsedScanSettingsDao.setLastUsedScanSettings(scanSettings)
        } else {
            lastUsedScanSettingsDao.clearLastUsedScanSettings()
        }
    }
}
