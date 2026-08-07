package io.github.chrisimx.scanbridge.startupmessages

import io.github.chrisimx.scanbridge.buildinfo.ScanBridgeEdition

enum class StartupMessage(val editions: Set<ScanBridgeEdition>) {
    THANKS_FOR_PURCHASE(setOf(ScanBridgeEdition.PLAYSTORE))
}
