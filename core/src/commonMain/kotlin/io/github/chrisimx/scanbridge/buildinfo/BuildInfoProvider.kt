package io.github.chrisimx.scanbridge.buildinfo

import io.github.chrisimx.scanbridge.model.Platform

interface BuildInfoProvider {
    val versionName: String
    val versionCode: Int
    val commit: String
    val edition: ScanBridgeEdition
    val isDebugBuild: Boolean
    val platform: Platform
}
