package io.github.chrisimx.scanbridge.filesystem

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_FILESYSTEM = module {
    single<FileKitStandardDirectoryProvider>() bind StandardDirectoryProvider::class
}
