import io.github.chrisimx.db.JvmScanBridgeDbBuilderFactory
import io.github.chrisimx.scanbridge.StartupTabsProvider
import io.github.chrisimx.scanbridge.buildinfo.BuildInfoProvider
import io.github.chrisimx.scanbridge.cropfeature.ImageCropService
import io.github.chrisimx.scanbridge.db.ScanBridgeDbBuilderFactory
import io.github.chrisimx.scanbridge.export.ExportCapabilitiesProvider
import io.github.chrisimx.scanbridge.imagerotation.ImageRotationService
import io.github.chrisimx.scanbridge.ports.MdnsDiscoverService
import io.github.chrisimx.scanbridge.ports.multicast.MulticastLockHandler
import io.github.chrisimx.scanbridge.scanning.ScanExecutionEntryPoint
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_JVM_PLATFORM = module {
    single<JvmScanBridgeDbBuilderFactory>() bind ScanBridgeDbBuilderFactory::class
    single<DesktopJvmBuildInfoProvider>() bind BuildInfoProvider::class

    single<DesktopJvmMulticastLockHandler>() bind MulticastLockHandler::class
    factory<JvmMdnsDiscoverService>() bind MdnsDiscoverService::class

    single<DesktopJvmScanExecutionEntryPoint>() bind ScanExecutionEntryPoint::class

    single<DesktopJvmImageRotationService>() bind ImageRotationService::class

    single<DesktopJvmStartupTabsProvider>() bind StartupTabsProvider::class

    single<DesktopJvmExportCapabilitiesProvider>() bind ExportCapabilitiesProvider::class

    single<JvmImageCropService>() bind ImageCropService::class
}
