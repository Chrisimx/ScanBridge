package io.github.chrisimx.scanbridge.migrations.appsettingsstore

import androidx.datastore.core.DataMigration
import com.google.protobuf.StringValue
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.anyscancompat.CommonAbstractionConversionResult
import io.github.chrisimx.esclkt.anyscancompat.toCommonAbstraction
import io.github.chrisimx.scanbridge.ScanSettingsJson
import io.github.chrisimx.scanbridge.model.ScanSettingsEnterableDataV0
import io.github.chrisimx.scanbridge.model.ScanSettingsEnterableDataV1
import io.github.chrisimx.scanbridge.proto.ScanBridgeSettings
import io.github.chrisimx.scanbridge.proto.copy
import io.github.chrisimx.scanbridge.proto.lastUsedScanSettingsUiStateOrNull
import timber.log.Timber

// V0 to V1, change type of serialized ScanSettings and ScanSettingsEnterableData
object AppSettingsMigrationV0To1 : DataMigration<ScanBridgeSettings> {
    override suspend fun shouldMigrate(currentData: ScanBridgeSettings): Boolean {
        return currentData.appSettingsVersion == 0
    }

    override suspend fun migrate(currentData: ScanBridgeSettings): ScanBridgeSettings {
        val json = ScanSettingsJson.json
        val oldScanSettingsString = currentData.lastUsedScanSettings?.value
        val oldScanSettingsParsed = oldScanSettingsString?.let {
            json.decodeFromString<ScanSettings>(it)
        }

        val oldEnterableData = currentData.lastUsedScanSettingsUiStateOrNull?.value
        val oldEnterableDataParsed = oldEnterableData?.let {
            json.decodeFromString<ScanSettingsEnterableDataV0>(it)
        }

        val newScanSettings = oldScanSettingsParsed?.let {
            json.encodeToString(oldScanSettingsParsed.toCommonAbstraction())
        }

        val newEnterableData = oldEnterableDataParsed?.let {
            val commonCapsResult = it.capabilities.toCommonAbstraction()

            val commonCaps = when (commonCapsResult) {
                is CommonAbstractionConversionResult.Success -> {
                    commonCapsResult.value
                }

                is CommonAbstractionConversionResult.Failure -> {
                    Timber.e("Could not convert enterable data in Migration of LastUsedScanSettings V0 to V1")
                    return@let null
                }
            }

            json.encodeToString(
                ScanSettingsEnterableDataV1(
                    commonCaps,
                    it.paperFormats,
                    it.customMenuEnabled,
                    it.widthString,
                    it.heightString,
                    it.maximumSize
                )
            )
        }


        return currentData.copy {
            appSettingsVersion = 1
            if (newScanSettings != null) {
                lastUsedScanSettings = StringValue.of(newScanSettings)
            }
            if (newEnterableData != null) {
                lastUsedScanSettingsUiState = StringValue.of(newEnterableData)
            }
        }
    }

    override suspend fun cleanUp() {
        return
    }
}
