package io.github.chrisimx.scanbridge.db.typeconverters

import io.github.chrisimx.anyscan.CommonScanSettings

class CommonScanSettingsTypeConverter : JsonSerializationTypeConverter<CommonScanSettings>(
    CommonScanSettings.serializer()
)
