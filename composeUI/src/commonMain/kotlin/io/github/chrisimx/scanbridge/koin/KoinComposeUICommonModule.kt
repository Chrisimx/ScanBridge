package io.github.chrisimx.scanbridge.koin

import io.github.chrisimx.scanbridge.createScannerIconImageLoader
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create

val KOIN_MODULE_COMPOSE_UI_COMMON = module {
    single(named("scannerIconImageLoader")) {
        create(::createScannerIconImageLoader)
    }
}
