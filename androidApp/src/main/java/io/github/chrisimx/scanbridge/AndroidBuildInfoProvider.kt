package io.github.chrisimx.scanbridge

import io.github.chrisimx.scanbridge.buildinfo.BuildInfoProvider
import io.github.chrisimx.scanbridge.buildinfo.ScanBridgeEdition
import io.github.chrisimx.scanbridge.model.Platform

class AndroidBuildInfoProvider : BuildInfoProvider {
    override val versionName: String = BuildConfig.VERSION_NAME
    override val versionCode: Int = BuildConfig.VERSION_CODE
    override val commit: String = BuildConfig.GIT_COMMIT_HASH
    override val edition: ScanBridgeEdition = when (BuildConfig.FLAVOR) {
        "play" -> ScanBridgeEdition.PLAYSTORE
        "fdroid" -> ScanBridgeEdition.FDROID
        else -> throw IllegalStateException("Unknown build flavor: ${BuildConfig.FLAVOR}")
    }
    override val isDebugBuild: Boolean = BuildConfig.DEBUG
    override val platform = Platform.ANDROID
}
