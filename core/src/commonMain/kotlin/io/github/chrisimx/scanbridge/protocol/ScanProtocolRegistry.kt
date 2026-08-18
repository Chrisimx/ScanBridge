package io.github.chrisimx.scanbridge.protocol

import io.github.chrisimx.scanbridge.protocol.escl.EsclScanningProtocol
import io.github.chrisimx.scanbridge.protocol.wsd.WsdScanningProtocol
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_SCAN_PROTOCOLS = module {
    single<EsclScanningProtocol>() bind ScanningProtocol::class
    single<WsdScanningProtocol>() bind ScanningProtocol::class
}
