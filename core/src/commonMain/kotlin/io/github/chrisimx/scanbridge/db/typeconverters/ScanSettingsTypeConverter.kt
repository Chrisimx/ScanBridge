package io.github.chrisimx.scanbridge.db.typeconverters

import androidx.room.TypeConverter
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.scanbridge.ScanSettingsJson

class ScanSettingsTypeConverter : JsonSerializationTypeConverter<ScanSettings>(ScanSettings.serializer())
