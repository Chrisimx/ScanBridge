package io.github.chrisimx.scanbridge

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.room.Room
import io.github.chrisimx.scanbridge.data.ui.CustomScannerViewModel
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsComposableStateHolder
import io.github.chrisimx.scanbridge.data.ui.ScanningScreenViewModel
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.proto.ScanBridgeSettings
import io.github.chrisimx.scanbridge.services.AndroidLocaleProvider
import io.github.chrisimx.scanbridge.services.DebugLogService
import io.github.chrisimx.scanbridge.services.FileDebugLogService
import io.github.chrisimx.scanbridge.services.LocaleProvider
import io.github.chrisimx.scanbridge.stores.LegacyCustomScannerStore.migrateLegacyCustomScanners
import java.util.concurrent.Executors
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel
import timber.log.Timber

val appModule = module {
    single<DataStore<ScanBridgeSettings>> {
        get<Context>().appSettingsStore
    }
    single<AndroidLocaleProvider>() bind LocaleProvider::class
    single<FileDebugLogService>() bind DebugLogService::class
    single<ScanBridgeDb>() {
        Room.databaseBuilder(
            get(),
            ScanBridgeDb::class.java, "scanbridge"
        ).setQueryCallback(
            { sqlQuery, bindArgs ->
                Timber.tag("RoomDebug").d("SQL: $sqlQuery, args: $bindArgs")
            },
            Executors.newSingleThreadExecutor() // callback runs here
        )
            .build()
            .migrateLegacyCustomScanners(get())
    }
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
    }
}
