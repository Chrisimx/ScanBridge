package io.github.chrisimx.scanbridge.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import io.github.chrisimx.scanbridge.ScanSettingsJson
import io.github.chrisimx.scanbridge.appsettings.D2RAppSettingsMigrationDataSource
import io.github.chrisimx.scanbridge.buildinfo.BuildInfoProvider
import io.github.chrisimx.scanbridge.model.Platform
import io.github.chrisimx.scanbridge.logging.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.savelastusedscansettings.D2RLastUsedScanSettingsMigrationSource
import kotlinx.coroutines.runBlocking
import org.koin.core.scope.Scope

class RoomMigrationVersion6To7(val buildInfoProvider: BuildInfoProvider, val koinScope: Scope, val loggerFactory: ScanBridgeLoggerFactory) :
    Migration(6, 7) {

    private val logger = loggerFactory.withClass(this::class)

    override fun migrate(connection: SQLiteConnection): Unit = runBlocking {
        val (oldAppSettings, oldLastUsedScanSettings) = if (buildInfoProvider.platform == Platform.ANDROID) {
            // On Android, we need to get the old AppSettings from the DataStore
            logger.debug { "Android: Migration of old app settings and last used scan settings from DataStore to Room" }
            val legacyAppSettingsDatasource = koinScope.get<D2RAppSettingsMigrationDataSource>()
            val legacyLastUsedScanSettingsDatasource = koinScope.get<D2RLastUsedScanSettingsMigrationSource>()

            legacyAppSettingsDatasource.getAppSettings() to legacyLastUsedScanSettingsDatasource.getLastUsedScanSettings()
        } else {
            logger.debug {
                "Other Platform (not Android): Migration of old AppSettings and last used scan settings from DataStore to Room not necessary"
            }
            null to null
        }

        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `appsettings` (`id` INTEGER NOT NULL, `writeDebugLogs` INTEGER NOT NULL, `disableCertValidation` INTEGER NOT NULL, `scanningResponseTimeoutInS` INTEGER NOT NULL, `chunkSizeForPDFExport` INTEGER NOT NULL, `rememberScanSettings` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `lastusedscansettings` (`id` INTEGER NOT NULL, `lastUsedScanSettings` TEXT NOT NULL, `lastUsedScanSettingsEnterable` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )

        oldAppSettings?.let {
            logger.debug { "Inserting old app settings into Room" }
            val appSettingsInsert = connection.prepare(
                "INSERT INTO appsettings (id, writeDebugLogs, disableCertValidation, scanningResponseTimeoutInS, chunkSizeForPDFExport, rememberScanSettings) VALUES (?, ?, ?, ?, ?, ?)"
            )
            appSettingsInsert.bindInt(1, it.id)
            appSettingsInsert.bindBoolean(2, it.writeDebugLogs)
            appSettingsInsert.bindBoolean(3, it.disableCertValidation)
            appSettingsInsert.bindInt(4, it.scanningResponseTimeoutInS)
            appSettingsInsert.bindInt(5, it.chunkSizeForPDFExport)
            appSettingsInsert.bindBoolean(6, it.rememberScanSettings)
            appSettingsInsert.step()
            appSettingsInsert.close()
        }

        oldLastUsedScanSettings?.let {
            logger.debug { "Inserting old last used scan settings into Room" }
            val lastUsedScanSettingsInsert = connection.prepare(
                "INSERT INTO lastusedscansettings (id, lastUsedScanSettings, lastUsedScanSettingsEnterable) VALUES (?, ?, ?)"
            )
            val scanSettingsJson = ScanSettingsJson.json
            lastUsedScanSettingsInsert.bindInt(1, oldLastUsedScanSettings.id)
            lastUsedScanSettingsInsert.bindText(
                2,
                scanSettingsJson.encodeToString(oldLastUsedScanSettings.lastUsedScanSettings)
            )
            lastUsedScanSettingsInsert.bindText(
                3,
                scanSettingsJson.encodeToString(oldLastUsedScanSettings.lastUsedScanSettingsEnterable)
            )
            lastUsedScanSettingsInsert.step()
            lastUsedScanSettingsInsert.close()
        }
    }
}
