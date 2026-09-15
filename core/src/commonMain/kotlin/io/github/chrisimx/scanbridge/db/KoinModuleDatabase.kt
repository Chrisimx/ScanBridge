package io.github.chrisimx.scanbridge.db

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.single

fun createScanBridgeDb(factory: ScanBridgeDbFactory): ScanBridgeDb = factory.createInstance()

val KOIN_MODULE_DATABASE = module {
    single<DefaultScanBridgeDbFactory>() bind ScanBridgeDbFactory::class
    single<ScanBridgeDb> {
        create(::createScanBridgeDb)
    }
}
