package io.github.chrisimx.scanbridge.cropfeature

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val KOIN_MODULE_CROP_FEAT = module {
    viewModel<CropScreenViewModel>()
    single<FinishCropUseCase>()
}
