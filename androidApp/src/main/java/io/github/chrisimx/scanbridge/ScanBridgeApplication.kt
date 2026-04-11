package io.github.chrisimx.scanbridge

import AndroidHttpClientFactory
import AndroidScanBridgeDbBuilderFactory
import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.room.Room
import io.github.chrisimx.scanbridge.data.ui.CustomScannerViewModel
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsComposableStateHolder
import io.github.chrisimx.scanbridge.data.ui.ScanningScreenViewModel
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.datastore.shownMessagesStore
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.migrations.MigrationExecutor
import io.github.chrisimx.scanbridge.migrations.RoomBackedMigrationExecutor
import io.github.chrisimx.scanbridge.migrations.migrationsModule
import io.github.chrisimx.scanbridge.proto.ScanBridgeSettings
import io.github.chrisimx.scanbridge.proto.ShownMessages
import io.github.chrisimx.scanbridge.repositories.DatastoreLastRouteRepository
import io.github.chrisimx.scanbridge.repositories.DatastoreShownMessagesRepository
import io.github.chrisimx.scanbridge.repositories.RoomLastRouteRepository
import io.github.chrisimx.scanbridge.services.AndroidLocaleProvider
import io.github.chrisimx.scanbridge.services.DebugLogService
import io.github.chrisimx.scanbridge.services.FileDebugLogService
import io.github.chrisimx.scanbridge.services.ScanJobRepository
import io.github.chrisimx.scanbridge.stores.LegacyCustomScannerStore.migrateLegacyCustomScanners
import io.github.chrisimx.scanbridge.stores.LegacySessionsStore.migrateLegacySessions
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
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.viewModel
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.chrisimx.scanbridge.db.DefaultScanBridgeDbFactory
import io.github.chrisimx.scanbridge.db.ScanBridgeDbBuilderFactory
import io.github.chrisimx.scanbridge.db.ScanBridgeDbFactory
import io.github.chrisimx.scanbridge.infrastructure.KmLogScanBridgeLogger
import io.github.chrisimx.scanbridge.infrastructure.KmLogScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.ports.HttpClientFactory
import io.github.chrisimx.scanbridge.ports.LocaleProvider
import io.github.chrisimx.scanbridge.ports.ScanBridgeLogger
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import timber.log.Timber

fun createAppSettingsDataStore(context: Context) = context.appSettingsStore
fun createShownMessagesDataStore(context: Context) = context.shownMessagesStore

fun createScanBridgeDb(factory: ScanBridgeDbFactory): ScanBridgeDb {
    return factory.createInstance()
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
        FileDebugLogService(get(named<ScanBridgeSettings>()),get())
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
    single<DatastoreLastRouteRepository>()
    single<RoomLastRouteRepository>() bind LastRouteRepository::class
    single<DatastoreShownMessagesRepository> { (scope: CoroutineScope) ->
        DatastoreShownMessagesRepository(get(named<ShownMessages>()), scope)
    } bind ShownMessagesRepository::class
    factory<ScanSettingsComposableStateHolder>()
    viewModel<ScanningScreenViewModel>()
    viewModel<CustomScannerViewModel>()
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

