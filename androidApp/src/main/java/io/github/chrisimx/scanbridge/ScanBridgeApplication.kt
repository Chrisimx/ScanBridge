package io.github.chrisimx.scanbridge

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.github.chrisimx.scanbridge.cropfeature.AndroidImageCropService
import io.github.chrisimx.localization.JvmNumberFormatter
import io.github.chrisimx.scanbridge.adapters.KoinBasedScanningProtocolManager
import io.github.chrisimx.scanbridge.adapters.RoomBackedCustomScannerRepository
import io.github.chrisimx.scanbridge.appsettings.AppSettingsRepository
import io.github.chrisimx.scanbridge.appsettings.AppSettingsViewModel
import io.github.chrisimx.scanbridge.appsettings.RoomAppSettingsRepository
import io.github.chrisimx.scanbridge.buildinfo.BuildInfoProvider
import io.github.chrisimx.scanbridge.cropfeature.CropScreenViewModel
import io.github.chrisimx.scanbridge.cropfeature.FinishCropUseCase
import io.github.chrisimx.scanbridge.cropfeature.ImageCropService
import io.github.chrisimx.scanbridge.db.DefaultScanBridgeDbFactory
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.ScanBridgeDbBuilderFactory
import io.github.chrisimx.scanbridge.db.ScanBridgeDbFactory
import io.github.chrisimx.scanbridge.export.ExportModule
import io.github.chrisimx.scanbridge.export.PdfNoopExportModule
import io.github.chrisimx.scanbridge.imagerotation.AndroidImageRotationService
import io.github.chrisimx.scanbridge.imagerotation.ImageRotationService
import io.github.chrisimx.scanbridge.infrastructure.KmLogScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.initialscansettings.DefaultInitialScanSettingsProvider
import io.github.chrisimx.scanbridge.initialscansettings.InitialScanSettingsProvider
import io.github.chrisimx.scanbridge.koin.KOIN_MODULE_COMMON
import io.github.chrisimx.scanbridge.localization.NumberFormatter
import io.github.chrisimx.scanbridge.migrations.MigrationExecutor
import io.github.chrisimx.scanbridge.migrations.RoomBackedMigrationExecutor
import io.github.chrisimx.scanbridge.migrations.ds2room.DATASTORE_TO_ROOM_MIGRATION_DATA_SOURCES
import io.github.chrisimx.scanbridge.migrations.migrationsModule
import io.github.chrisimx.scanbridge.model.HttpClientConfig
import io.github.chrisimx.scanbridge.ports.CustomScannerRepository
import io.github.chrisimx.scanbridge.ports.HttpClientFactory
import io.github.chrisimx.scanbridge.localization.LocaleProvider
import io.github.chrisimx.scanbridge.ports.MdnsDiscoverService
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.ports.ScanningProtocolManager
import io.github.chrisimx.scanbridge.ports.multicast.MulticastLockHandler
import io.github.chrisimx.scanbridge.proto.ShownMessages
import io.github.chrisimx.scanbridge.repositories.DatastoreLastRouteRepository
import io.github.chrisimx.scanbridge.repositories.DatastoreShownMessagesRepository
import io.github.chrisimx.scanbridge.repositories.RoomLastRouteRepository
import io.github.chrisimx.scanbridge.savelastusedscansettings.LastUsedScanSettingsRepository
import io.github.chrisimx.scanbridge.savelastusedscansettings.RoomLastUsedScanSettingsRepository
import io.github.chrisimx.scanbridge.scannerdiscovery.DiscoveryUsecase
import io.github.chrisimx.scanbridge.scannerdiscovery.ScannerDiscoveryScreenViewModel
import io.github.chrisimx.scanbridge.services.AndroidLocaleProvider
import io.github.chrisimx.scanbridge.repositories.ScanJobRepository
import io.github.chrisimx.scanbridge.scan.AndroidScanExecutor
import io.github.chrisimx.scanbridge.scanning.ScanExecutor
import io.github.chrisimx.scanbridge.startupmessages.RoomShownStartupMessagesRepository
import io.github.chrisimx.scanbridge.startupmessages.ShownStartupMessagesRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel
import timber.log.Timber

fun createScanBridgeDb(factory: ScanBridgeDbFactory): ScanBridgeDb = factory.createInstance()

fun createScannerIconImageLoader(factory: HttpClientFactory, context: Context): ImageLoader {
    val ktorClient: HttpClient = factory.create(
        HttpClientConfig(
            disableCertValidation = true,
            debugLogging = false,
            requestTimeoutInSeconds = 2u,
            connectTimeoutInSeconds = 2u,
            socketTimeoutInSeconds = 2u
        )
    )
    return ImageLoader.Builder(context)
        .components {
            add(
                KtorNetworkFetcherFactory(
                    httpClient = ktorClient
                )
            )
        }
        .build()
}
val appModule = module {
    single<AndroidCrashHandler>() bind Thread.UncaughtExceptionHandler::class
    single<AndroidLocaleProvider>() bind LocaleProvider::class
    single<RoomAppSettingsRepository>() bind AppSettingsRepository::class
    single<AndroidHttpClientFactory>() bind HttpClientFactory::class
    single<KmLogScanBridgeLoggerFactory>() bind ScanBridgeLoggerFactory::class
    single<ScanJobRepository>()
    single<RoomBackedMigrationExecutor>() bind MigrationExecutor::class
    includes(migrationsModule)
    single<AndroidScanBridgeDbBuilderFactory>() bind ScanBridgeDbBuilderFactory::class
    single<DefaultScanBridgeDbFactory>() bind ScanBridgeDbFactory::class
    single<ScanBridgeDb> {
        create(::createScanBridgeDb)
    }
    single(named("scannerIconImageLoader")) {
        create(::createScannerIconImageLoader)
    }
    factory<AndroidMdnsDiscoverService>() bind MdnsDiscoverService::class
    single<DatastoreLastRouteRepository>()
    single<RoomLastRouteRepository>() bind LastRouteRepository::class
    single<RoomLastUsedScanSettingsRepository>() bind LastUsedScanSettingsRepository::class
    single<DatastoreShownMessagesRepository>(named("legacyDatastoreShownMessages")) {
        DatastoreShownMessagesRepository(
            get(named<ShownMessages>()),
            get()
        )
    } bind ShownStartupMessagesRepository::class
    single<RoomShownStartupMessagesRepository>() bind ShownStartupMessagesRepository::class
    single<KoinBasedScanningProtocolManager>() bind ScanningProtocolManager::class
    single<DefaultInitialScanSettingsProvider>() bind InitialScanSettingsProvider::class
    single<DefaultPaperFormatProvider>() bind PaperFormatProvider::class
    factory<ScanSettingsComposableStateHolder>()
    viewModel<ScanningScreenViewModel>()
    single<RoomBackedCustomScannerRepository>() bind CustomScannerRepository::class
    single<DiscoveryUsecase>()
    viewModel<ScannerDiscoveryScreenViewModel>()
    single<AndroidMulticastLockHandler>() bind MulticastLockHandler::class
    single<AndroidScanExecutor>() bind ScanExecutor::class
    single<AndroidImageRotationService>() bind ImageRotationService::class

    single<AndroidBuildInfoProvider>() bind BuildInfoProvider::class

    viewModel<AppSettingsViewModel>()

    single<JvmNumberFormatter>() bind NumberFormatter::class

    single<StartupTabsProvider> {
        STARTUP_TABS_PROVIDER
    }

    viewModel<CropScreenViewModel>()
    single<AndroidImageCropService>() bind ImageCropService::class
    single<FinishCropUseCase>()

    single<PdfNoopExportModule>() bind ExportModule::class

    includes(
        KOIN_MODULE_COMMON
    )

    includes(DATASTORE_TO_ROOM_MIGRATION_DATA_SOURCES)
}

class ScanBridgeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ScanBridgeApplication)
            modules(appModule)
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
