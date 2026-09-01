package io.github.chrisimx.scanbridge.appsettings

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val KOIN_MODULE_APP_SETTINGS = module {
    single<RoomAppSettingsRepository>() bind AppSettingsRepository::class
    viewModel<AppSettingsViewModel>()
}
