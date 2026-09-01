package io.github.chrisimx.scanbridge.protocol

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_SCANNING_PROTOCOL_FEAT = module {
    single<KoinBasedScanningProtocolManager>() bind ScanningProtocolManager::class
}
