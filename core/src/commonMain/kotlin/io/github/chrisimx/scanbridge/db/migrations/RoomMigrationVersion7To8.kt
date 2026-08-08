package io.github.chrisimx.scanbridge.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import io.github.chrisimx.scanbridge.ScanSettingsJson
import io.github.chrisimx.scanbridge.db.entities.AppSettings

val ROOM_MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        // Entity: AppSettings
        // + preferredInitialScanSettings: CommonScanSettings
        val defaultAppSettings = AppSettings()
        val defaultPreferredInitialScanSettings = defaultAppSettings.preferredInitialScanSettings

        val serializedDefaultPreferredInitialScanSettings = ScanSettingsJson.json
            .encodeToString(defaultPreferredInitialScanSettings)

        val statement = connection.prepare("ALTER TABLE appsettings ADD COLUMN preferredInitialScanSettings TEXT NOT NULL DEFAULT ?")
        statement.bindText(1, serializedDefaultPreferredInitialScanSettings)
        statement.step()
        statement.close()
    }
}
