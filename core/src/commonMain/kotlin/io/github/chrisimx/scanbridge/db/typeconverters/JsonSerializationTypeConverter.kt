package io.github.chrisimx.scanbridge.db.typeconverters

import androidx.room.TypeConverter
import io.github.chrisimx.scanbridge.ScanSettingsJson
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer

abstract class JsonSerializationTypeConverter<T : Any>(
    private val serializer: KSerializer<T>
) {
    @TypeConverter
    fun fromSerializedString(serialized: String): T? = if (serialized == "null") {
        null
    } else {
        ScanSettingsJson.json.decodeFromString(serializer, serialized)
    }

    @TypeConverter
    fun toSerializedString(scanSettings: T?): String = if (scanSettings == null) {
        "null"
    } else {
        ScanSettingsJson.json.encodeToString(serializer, scanSettings)
    }
}
