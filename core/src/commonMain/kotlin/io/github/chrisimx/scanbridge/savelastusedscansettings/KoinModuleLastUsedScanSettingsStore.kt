package io.github.chrisimx.scanbridge.savelastusedscansettings

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_LAST_USED_SCAN_SETTINGS_STORE = module {
    single<RoomLastUsedScanSettingsRepository>() bind LastUsedScanSettingsRepository::class
}
