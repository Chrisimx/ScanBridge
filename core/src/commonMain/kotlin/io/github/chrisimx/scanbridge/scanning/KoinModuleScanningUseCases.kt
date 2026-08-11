package io.github.chrisimx.scanbridge.scanning

import io.github.chrisimx.scanbridge.imagerotation.RotateScanUseCase
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_SCANNING_USE_CASES = module {
    single<StartScanUseCase>()
    single<DeleteScanUseCase>()
    single<DeleteSessionUseCase>()
    single<SwapPagesUseCase>()
    single<RotateScanUseCase>()
    single<ExportAllPagesUseCase>()
}
