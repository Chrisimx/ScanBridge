package io.github.chrisimx.scanbridge.db.typeconverters

import io.github.chrisimx.scanbridge.model.ScanSettingsEnterableDataV0
import io.github.chrisimx.scanbridge.model.ScanSettingsEnterableDataV1

class ScanSettingsUiDataTypeConverterV0 : JsonSerializationTypeConverter<ScanSettingsEnterableDataV0>(
    ScanSettingsEnterableDataV0.serializer()
)

class ScanSettingsUiDataTypeConverterV1 : JsonSerializationTypeConverter<ScanSettingsEnterableDataV1>(
    ScanSettingsEnterableDataV1.serializer()
)
