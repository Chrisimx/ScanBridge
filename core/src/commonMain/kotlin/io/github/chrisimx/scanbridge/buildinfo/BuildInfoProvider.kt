package io.github.chrisimx.scanbridge.buildinfo

import io.github.chrisimx.scanbridge.model.Platform
import io.github.chrisimx.scanbridge.BuildKonfig

interface BuildInfoProvider {
    val versionName: String
        get() = BuildKonfig.scanbridgeVersion
    val versionCode: Int
        get() = BuildKonfig.scanbridgeVersionCode
    val commit: String
        get() = BuildKonfig.gitCommitHash
    val edition: ScanBridgeEdition
    val isDebugBuild: Boolean
    val platform: Platform
}
