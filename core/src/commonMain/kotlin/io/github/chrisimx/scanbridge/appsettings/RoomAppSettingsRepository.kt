package io.github.chrisimx.scanbridge.appsettings

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAppSettingsRepository(scanBridgeDb: ScanBridgeDb) : AppSettingsRepository {
    private val appSettingsDao = scanBridgeDb.appSettingsDao()

    override suspend fun getAppSettings(): AppSettings = appSettingsDao.getAppSettings() ?: AppSettings()

    override fun getAppSettingsFlow(): Flow<AppSettings> = appSettingsDao.getAppSettingsFlow().map {
        it ?: AppSettings()
    }

    override suspend fun setAppSettings(appSettings: AppSettings) = appSettingsDao.setAppSettings(appSettings)

    override suspend fun updateAppSettings(updateOperation: AppSettings.() -> AppSettings) =
        appSettingsDao.updateAppSettings(updateOperation)
}
