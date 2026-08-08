package io.github.chrisimx.scanbridge.migrations.ds2room

import android.content.Context
import io.github.chrisimx.scanbridge.db.entities.LastUsedScanSettings
import io.github.chrisimx.scanbridge.savelastusedscansettings.D2RLastUsedScanSettingsMigrationSource
import io.github.chrisimx.scanbridge.stores.DatastoreLegacyLastScanSettingsStore

class D2RLastUsedScanSettingsMigrationDataSourceImpl(val context: Context) : D2RLastUsedScanSettingsMigrationSource {

    @Suppress("DEPRECATION")
    override suspend fun getLastUsedScanSettings(): LastUsedScanSettings? {
        val (settings, enterableData) = DatastoreLegacyLastScanSettingsStore.load(context)

        return settings?.let { settings ->
            enterableData?.let { enterableData ->
                LastUsedScanSettings(
                    lastUsedScanSettings = settings,
                    lastUsedScanSettingsEnterable = enterableData
                )
            }
        }
    }
}
