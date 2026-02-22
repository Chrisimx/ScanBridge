package io.github.chrisimx.scanbridge.datastore

import androidx.datastore.core.DataStore
import com.google.protobuf.BoolValue
import com.google.protobuf.UInt32Value
import io.github.chrisimx.scanbridge.proto.ScanBridgeSettings
import io.github.chrisimx.scanbridge.proto.ScanBridgeSettingsKt
import io.github.chrisimx.scanbridge.proto.copy

suspend fun DataStore<ScanBridgeSettings>.setDisableCertCheck(value: Boolean) {
    this.updateSettings {
        disableCertChecks = value
    }
}

suspend fun DataStore<ScanBridgeSettings>.setRememberScanSettings(value: Boolean) {
    this.updateSettings {
        rememberScanSettings = BoolValue.of(value)
    }
}

suspend fun DataStore<ScanBridgeSettings>.setTimeoutSetting(value: UInt) {
    this.updateSettings {
        scanningResponseTimeout = UInt32Value.of(value.toInt())
    }
}

suspend fun DataStore<ScanBridgeSettings>.setChunkSize(value: UInt) {
    this.updateSettings {
        chunkSizePdfExport = UInt32Value.of(value.toInt())
    }
}

suspend fun DataStore<ScanBridgeSettings>.setWriteDebugLog(value: Boolean) {
    this.updateSettings {
        writeDebug = value
    }
}


suspend fun DataStore<ScanBridgeSettings>.updateSettings(
    set: ScanBridgeSettingsKt.Dsl.() -> Unit
) {
    this.updateData { current ->
        current.copy(set)
    }
}
