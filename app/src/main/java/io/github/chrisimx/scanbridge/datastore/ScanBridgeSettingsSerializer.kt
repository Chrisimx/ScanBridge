package io.github.chrisimx.scanbridge.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.BoolValue
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.UInt32Value
import io.github.chrisimx.scanbridge.proto.ScanBridgeSettings
import io.github.chrisimx.scanbridge.proto.scanBridgeSettings
import java.io.InputStream
import java.io.OutputStream

object ScanBridgeSettingsSerializer : Serializer<ScanBridgeSettings> {
    override val defaultValue: ScanBridgeSettings = scanBridgeSettings {
            scanningResponseTimeout = UInt32Value.of(25)
            rememberScanSettings = BoolValue.of(true)
            chunkSizePdfExport = UInt32Value.of(50)
        }

    override suspend fun readFrom(input: InputStream): ScanBridgeSettings {
        try {
            return ScanBridgeSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: ScanBridgeSettings, output: OutputStream) {
        return t.writeTo(output)
    }
}
