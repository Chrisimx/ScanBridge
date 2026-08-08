package io.github.chrisimx.scanbridge.appsettings

import io.github.chrisimx.scanbridge.db.entities.AppSettings

interface D2RAppSettingsMigrationDataSource {
    suspend fun getAppSettings(): AppSettings
}
