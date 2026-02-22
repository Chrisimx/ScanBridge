package io.github.chrisimx.scanbridge.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import com.google.protobuf.BoolValue
import com.google.protobuf.StringValue
import com.google.protobuf.UInt32Value
import io.github.chrisimx.scanbridge.proto.LastRoute
import io.github.chrisimx.scanbridge.proto.ScanBridgeSettings
import io.github.chrisimx.scanbridge.proto.lastRouteOrNull

val Context.appSettingsStore: DataStore<ScanBridgeSettings> by dataStore(
    fileName = "settings.pb",
    serializer = ScanBridgeSettingsSerializer,
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(context, "scanbridge") { sharedPrefs, currentSettings ->
                currentSettings.toBuilder().apply {
                    autoCleanup = sharedPrefs.getBoolean("auto_cleanup", false)
                    disableCertChecks = sharedPrefs.getBoolean("disable_cert_checks", false)
                    sharedPrefs.getString("last_used_scan_settings", null).let {
                        if (it != null) {
                            lastUsedScanSettings = StringValue.of(it)
                        } else {
                            clearLastUsedScanSettings()
                        }
                    }
                    writeDebug = sharedPrefs.getBoolean("write_debug", false)
                    scanningResponseTimeout = UInt32Value.of(sharedPrefs.getInt("scanning_response_timeout", 25))
                    chunkSizePdfExport = UInt32Value.of(sharedPrefs.getInt("chunk_size_pdf_export", 50))
                    rememberScanSettings = BoolValue.of(sharedPrefs.getBoolean("remember_scan_settings", true))
                }.build()
            }
        )
    }
)

val Context.lastRouteStore: DataStore<LastRoute> by dataStore(
    fileName = "route_store.pb",
    serializer = LastRouteStoreSerializer,
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(context, "route_store") { sharedPrefs, currentSettings ->
                currentSettings.toBuilder().apply {
                    sharedPrefs.getString("last_route", null).let {
                        if (it != null) {
                            lastRoute = StringValue.of(it)
                        } else {
                            clearLastRoute()
                        }
                    }
                }.build()
            }
        )
    }
)
