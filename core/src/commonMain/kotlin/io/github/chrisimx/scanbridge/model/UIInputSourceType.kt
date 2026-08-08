package io.github.chrisimx.scanbridge.model

import io.github.chrisimx.anyscan.CommonInputSourceType

enum class UIInputSourceType {
    PLATEN,
    ADF
}

fun CommonInputSourceType.toUIInputSourceType(): UIInputSourceType = when (this) {
    CommonInputSourceType.PLATEN -> UIInputSourceType.PLATEN
    CommonInputSourceType.ADF_SIMPLEX -> UIInputSourceType.ADF
    CommonInputSourceType.ADF_DUPLEX -> UIInputSourceType.ADF
}
