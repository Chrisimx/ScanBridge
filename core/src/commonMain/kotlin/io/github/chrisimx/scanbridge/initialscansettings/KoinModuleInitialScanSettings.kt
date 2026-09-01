package io.github.chrisimx.scanbridge.initialscansettings

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_INITIAL_SCAN_SETTINGS = module {
    single<DefaultInitialScanSettingsProvider>() bind InitialScanSettingsProvider::class
}
