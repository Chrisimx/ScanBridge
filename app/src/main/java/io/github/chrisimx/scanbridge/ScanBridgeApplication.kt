package io.github.chrisimx.scanbridge

import android.app.Application
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsComposableViewModel
import io.github.chrisimx.scanbridge.data.ui.ScanningScreenViewModel
import io.github.chrisimx.scanbridge.services.AndroidLocaleProvider
import io.github.chrisimx.scanbridge.services.LocaleProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel
import timber.log.Timber

val appModule = module {
    single<AndroidLocaleProvider>() bind LocaleProvider::class
    viewModel<ScanSettingsComposableViewModel>()
    viewModel<ScanningScreenViewModel>()
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
