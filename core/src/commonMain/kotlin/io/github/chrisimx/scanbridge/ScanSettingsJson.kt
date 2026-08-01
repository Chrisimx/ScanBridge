package io.github.chrisimx.scanbridge

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

object ScanSettingsJson {
    val json = Json {
        ignoreUnknownKeys = false
        serializersModule = SerializersModule {}
        allowStructuredMapKeys = true
        classDiscriminator = "type"
        prettyPrint = false
    }
}
