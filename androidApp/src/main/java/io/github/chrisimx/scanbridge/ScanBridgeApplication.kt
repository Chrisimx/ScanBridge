package io.github.chrisimx.scanbridge

import KOIN_MODULE_JVM_AND_ANDROID_PLATFORM
import android.app.Application
import io.github.chrisimx.scanbridge.buildinfo.BuildInfoProvider
import io.github.chrisimx.scanbridge.cropfeature.AndroidImageCropService
import io.github.chrisimx.scanbridge.cropfeature.ImageCropService
import io.github.chrisimx.scanbridge.db.ScanBridgeDbBuilderFactory
import io.github.chrisimx.scanbridge.export.ExportCapabilitiesProvider
import io.github.chrisimx.scanbridge.imagerotation.AndroidImageRotationService
import io.github.chrisimx.scanbridge.imagerotation.ImageRotationService
import io.github.chrisimx.scanbridge.koin.KOIN_MODULE_COMMON
import io.github.chrisimx.scanbridge.koin.KOIN_MODULE_COMPOSE_UI_COMMON
import io.github.chrisimx.scanbridge.migrations.MigrationExecutor
import io.github.chrisimx.scanbridge.migrations.RoomBackedMigrationExecutor
import io.github.chrisimx.scanbridge.migrations.ds2room.DATASTORE_TO_ROOM_MIGRATION_DATA_SOURCES
import io.github.chrisimx.scanbridge.migrations.migrationsModule
import io.github.chrisimx.scanbridge.ports.MdnsDiscoverService
import io.github.chrisimx.scanbridge.ports.multicast.MulticastLockHandler
import io.github.chrisimx.scanbridge.proto.ShownMessages
import io.github.chrisimx.scanbridge.repositories.DatastoreLastRouteRepository
import io.github.chrisimx.scanbridge.repositories.DatastoreShownMessagesRepository
import io.github.chrisimx.scanbridge.scan.AndroidScanExecutionEntryPoint
import io.github.chrisimx.scanbridge.scanning.ScanExecutionEntryPoint
import io.github.chrisimx.scanbridge.startupmessages.ShownStartupMessagesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.single
import timber.log.Timber

val androidPlatformModule = module {
    single<AndroidCrashHandler>() bind Thread.UncaughtExceptionHandler::class
    single<RoomBackedMigrationExecutor>() bind MigrationExecutor::class
    includes(migrationsModule)
    single<AndroidScanBridgeDbBuilderFactory>() bind ScanBridgeDbBuilderFactory::class

    factory<AndroidMdnsDiscoverService>() bind MdnsDiscoverService::class
    single<DatastoreLastRouteRepository>()
    single<DatastoreShownMessagesRepository>(named("legacyDatastoreShownMessages")) {
        DatastoreShownMessagesRepository(
            get(named<ShownMessages>()),
            get()
        )
    } bind ShownStartupMessagesRepository::class

    single<AndroidMulticastLockHandler>() bind MulticastLockHandler::class
    single<AndroidScanExecutionEntryPoint>() bind ScanExecutionEntryPoint::class

    single<AndroidImageRotationService>() bind ImageRotationService::class

    single<AndroidBuildInfoProvider>() bind BuildInfoProvider::class

    single<StartupTabsProvider> {
        STARTUP_TABS_PROVIDER
    }

    single<AndroidExportCapabilitiesProvider>() bind ExportCapabilitiesProvider::class

    single<AndroidImageCropService>() bind ImageCropService::class

    includes(DATASTORE_TO_ROOM_MIGRATION_DATA_SOURCES)
}

class ScanBridgeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ScanBridgeApplication)
            modules(KOIN_MODULE_COMMON, androidPlatformModule, KOIN_MODULE_JVM_AND_ANDROID_PLATFORM, KOIN_MODULE_COMPOSE_UI_COMMON)
        }

        Timber.plant(Timber.DebugTree())

        runMigrations()
    }

    fun runMigrations() {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val migrationExecutor = getKoin().get<MigrationExecutor>()
        coroutineScope.launch {
            migrationExecutor.runMigrations()
        }
    }
}
