package io.github.chrisimx.scanbridge.logging

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_LOGGING = module {
    single<KmLogScanBridgeLoggerFactory>() bind ScanBridgeLoggerFactory::class
    single<KmLogScanBridgeLogger>() bind ScanBridgeLogger::class
}
