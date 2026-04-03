package io.github.chrisimx.scanbridge.db.typeconverters

import androidx.room.TypeConverter
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.scanbridge.util.ScanSettingsJson

class ScanSettingsTypeConverter {

    @TypeConverter
    fun fromScanSettingsString(scanSettings: String): ScanSettings? = if (scanSettings == "null") {
        null
    } else {
        ScanSettingsJson.json.decodeFromString<ScanSettings>(scanSettings)
    }

    @TypeConverter
    fun toScanSettingsString(scanSettings: ScanSettings?): String = if (scanSettings == null) {
        "null"
    } else {
        ScanSettingsJson.json.encodeToString(scanSettings)
    }
}
