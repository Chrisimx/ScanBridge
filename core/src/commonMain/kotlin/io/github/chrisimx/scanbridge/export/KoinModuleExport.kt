package io.github.chrisimx.scanbridge.export

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_EXPORT = module {
    single<KoinExportModuleManager>() bind ExportModuleManager::class
}
