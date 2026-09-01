package io.github.chrisimx.scanbridge

import io.github.chrisimx.scanbridge.buildinfo.BuildInfoProvider
import io.github.chrisimx.scanbridge.buildinfo.ScanBridgeEdition
import io.github.chrisimx.scanbridge.model.Platform

class AndroidBuildInfoProvider : BuildInfoProvider {
    override val edition: ScanBridgeEdition = when (BuildConfig.FLAVOR) {
        "play" -> ScanBridgeEdition.PLAYSTORE
        "fdroid" -> ScanBridgeEdition.FDROID
        else -> throw IllegalStateException("Unknown build flavor: ${BuildConfig.FLAVOR}")
    }
    override val isDebugBuild: Boolean = BuildConfig.DEBUG
    override val platform = Platform.ANDROID
}
