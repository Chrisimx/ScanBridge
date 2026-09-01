import io.github.chrisimx.scanbridge.buildinfo.BuildInfoProvider
import io.github.chrisimx.scanbridge.buildinfo.ScanBridgeEdition
import io.github.chrisimx.scanbridge.model.Platform

class DesktopJvmBuildInfoProvider : BuildInfoProvider {
    override val edition: ScanBridgeEdition
        get() = ScanBridgeEdition.DESKTOP
    override val isDebugBuild: Boolean
        get() = false
    override val platform: Platform
        get() = Platform.DESKTOP
}
