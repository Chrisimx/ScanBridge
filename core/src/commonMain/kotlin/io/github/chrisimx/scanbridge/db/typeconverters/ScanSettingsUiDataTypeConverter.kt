package io.github.chrisimx.scanbridge.db.typeconverters

import androidx.room.TypeConverter
import io.github.chrisimx.scanbridge.ScanSettingsJson
import io.github.chrisimx.scanbridge.model.ScanSettingsStateData

class ScanSettingsUiDataTypeConverter {

    @TypeConverter
    fun fromScanSettingsString(scanSettings: String): ScanSettingsStateData? = if (scanSettings == "null") {
        null
    } else {
        ScanSettingsJson.json.decodeFromString<ScanSettingsStateData>(scanSettings)
    }

    @TypeConverter
    fun toScanSettingsString(scanSettings: ScanSettingsStateData?): String = if (scanSettings == null) {
        "null"
    } else {
        ScanSettingsJson.json.encodeToString(scanSettings)
    }
}
