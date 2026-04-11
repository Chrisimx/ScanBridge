package io.github.chrisimx.scanbridge.db.typeconverters

import androidx.room.TypeConverter
import io.github.chrisimx.scanbridge.ScanSettingsJson
import io.github.chrisimx.scanbridge.model.ScanSettingsEnterableData

class ScanSettingsUiDataTypeConverter {

    @TypeConverter
    fun fromScanSettingsString(scanSettings: String): ScanSettingsEnterableData? = if (scanSettings == "null") {
        null
    } else {
        ScanSettingsJson.json.decodeFromString<ScanSettingsEnterableData>(scanSettings)
    }

    @TypeConverter
    fun toScanSettingsString(scanSettings: ScanSettingsEnterableData?): String = if (scanSettings == null) {
        "null"
    } else {
        ScanSettingsJson.json.encodeToString(scanSettings)
    }
}
