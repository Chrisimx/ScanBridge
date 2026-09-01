package io.github.chrisimx.scanbridge.scannerdiscovery

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val KOIN_MODULE_SCANNER_DISCOVERY = module {
    single<RoomBackedCustomScannerRepository>() bind CustomScannerRepository::class
    single<DiscoveryUsecase>()
    viewModel<ScannerDiscoveryScreenViewModel>()
}
