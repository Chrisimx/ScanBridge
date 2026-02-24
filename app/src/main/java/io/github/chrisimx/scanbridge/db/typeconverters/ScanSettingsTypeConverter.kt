package io.github.chrisimx.scanbridge.db.typeconverters

import androidx.room.TypeConverter
import io.github.chrisimx.esclkt.Inches
import io.github.chrisimx.esclkt.LengthUnit
import io.github.chrisimx.esclkt.Millimeters
import io.github.chrisimx.esclkt.Points
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ThreeHundredthsOfInch
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class ScanSettingsTypeConverter {
    private val json = Json {
        ignoreUnknownKeys = false
        serializersModule = SerializersModule {
            polymorphic(LengthUnit::class) {
                subclass(Inches::class)
                subclass(Millimeters::class)
                subclass(ThreeHundredthsOfInch::class)
                subclass(Points::class)
            }
        }
        classDiscriminator = "type"
        prettyPrint = false
    }

    @TypeConverter
    fun fromScanSettingsString(scanSettings: String): ScanSettings? = if (scanSettings == "null") {
        null
    } else {
        json.decodeFromString<ScanSettings>(scanSettings)
    }

    @TypeConverter
    fun toScanSettingsString(scanSettings: ScanSettings?): String {
        return if (scanSettings == null) {
            return "null"
        } else {
            json.encodeToString(scanSettings)
        }
    }
}
