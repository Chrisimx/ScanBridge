package io.github.chrisimx.scanbridge

import AndroidHttpClientFactory
import AndroidMdnsDiscoverService
import AndroidScanBridgeDbBuilderFactory
import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.github.chrisimx.scanbridge.adapters.KoinBasedScanningProtocolManager
import io.github.chrisimx.scanbridge.adapters.RoomBackedCustomScannerRepository
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsComposableStateHolder
import io.github.chrisimx.scanbridge.data.ui.ScanningScreenViewModel
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.datastore.shownMessagesStore
import io.github.chrisimx.scanbridge.db.DefaultScanBridgeDbFactory
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.ScanBridgeDbBuilderFactory
import io.github.chrisimx.scanbridge.db.ScanBridgeDbFactory
import io.github.chrisimx.scanbridge.escl.EsclScanningProtocol
import io.github.chrisimx.scanbridge.infrastructure.KmLogScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.migrations.MigrationExecutor
import io.github.chrisimx.scanbridge.migrations.RoomBackedMigrationExecutor
import io.github.chrisimx.scanbridge.migrations.migrationsModule
import io.github.chrisimx.scanbridge.model.HttpClientConfig
import io.github.chrisimx.scanbridge.ports.CustomScannerRepository
import io.github.chrisimx.scanbridge.ports.HttpClientFactory
import io.github.chrisimx.scanbridge.ports.LocaleProvider
import io.github.chrisimx.scanbridge.ports.MdnsDiscoverService
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.ports.ScanningProtocol
import io.github.chrisimx.scanbridge.ports.ScanningProtocolManager
import io.github.chrisimx.scanbridge.proto.ScanBridgeSettings
import io.github.chrisimx.scanbridge.proto.ShownMessages
import io.github.chrisimx.scanbridge.repositories.DatastoreLastRouteRepository
import io.github.chrisimx.scanbridge.repositories.DatastoreShownMessagesRepository
import io.github.chrisimx.scanbridge.repositories.RoomLastRouteRepository
import io.github.chrisimx.scanbridge.scannerdiscovery.DiscoveryUsecase
import io.github.chrisimx.scanbridge.scannerdiscovery.ScannerDiscoveryScreenViewModel
import io.github.chrisimx.scanbridge.services.AndroidLocaleProvider
import io.github.chrisimx.scanbridge.services.DebugLogService
import io.github.chrisimx.scanbridge.services.FileDebugLogService
import io.github.chrisimx.scanbridge.services.ScanJobRepository
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

fun createAppSettingsDataStore(context: Context) = context.appSettingsStore
fun createShownMessagesDataStore(context: Context) = context.shownMessagesStore

fun createScanBridgeDb(factory: ScanBridgeDbFactory): ScanBridgeDb = factory.createInstance()

fun createScannerIconImageLoader(factory: HttpClientFactory, context: Context): ImageLoader {
    val ktorClient: HttpClient = factory.create(
        HttpClientConfig(
            disableCertValidation = true,
            debugLogging = false,
            requestTimeoutInSeconds = 2u,
            connectTimeoutInSeconds = 2u,
            socketTimeoutInSeconds = 2u,
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
    single<DataStore<ShownMessages>>(named<ShownMessages>()) {
        create(::createShownMessagesDataStore)
    }
    single<DataStore<ScanBridgeSettings>>(named<ScanBridgeSettings>()) {
        create(::createAppSettingsDataStore)
    }
    single<CrashHandler>() bind Thread.UncaughtExceptionHandler::class
    single<AndroidLocaleProvider>() bind LocaleProvider::class
    single<FileDebugLogService> {
        FileDebugLogService(get(named<ScanBridgeSettings>()), get())
    } bind DebugLogService::class
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
    single<DatastoreShownMessagesRepository> { (scope: CoroutineScope) ->
        DatastoreShownMessagesRepository(get(named<ShownMessages>()), scope)
    } bind ShownMessagesRepository::class
    single<KoinBasedScanningProtocolManager>() bind ScanningProtocolManager::class
    factory<ScanSettingsComposableStateHolder>()
    viewModel<ScanningScreenViewModel>()
    single<RoomBackedCustomScannerRepository>() bind CustomScannerRepository::class
    single<DiscoveryUsecase>()
    viewModel<ScannerDiscoveryScreenViewModel>()
    single<EsclScanningProtocol>() bind ScanningProtocol::class
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
