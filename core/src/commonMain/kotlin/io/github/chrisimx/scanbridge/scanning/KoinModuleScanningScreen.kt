package io.github.chrisimx.scanbridge.scanning

import io.github.chrisimx.scanbridge.ScanSettingsComposableStateHolder
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.viewModel

val KOIN_MODULE_SCANNING_SCREEN = module {
    factory<ScanSettingsComposableStateHolder>()
    viewModel<ScanningScreenViewModel>()
}
