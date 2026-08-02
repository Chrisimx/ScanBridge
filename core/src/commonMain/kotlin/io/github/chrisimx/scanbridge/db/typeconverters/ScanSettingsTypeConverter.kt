package io.github.chrisimx.scanbridge.db.typeconverters

import io.github.chrisimx.esclkt.ScanSettings

class ScanSettingsTypeConverter : JsonSerializationTypeConverter<ScanSettings>(ScanSettings.serializer())
