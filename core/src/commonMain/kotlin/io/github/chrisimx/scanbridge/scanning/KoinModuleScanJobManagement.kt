package io.github.chrisimx.scanbridge.scanning

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_SCAN_JOB_MANAGEMENT = module {
    single<ScanJobRepository>()
    single<CommonScanExecutor>() bind ScanExecutor::class
}
