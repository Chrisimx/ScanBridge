package io.github.chrisimx.scanbridge.export

import io.github.chrisimx.scanbridge.export.zip.KmpNativeZipBasedZipService
import io.github.chrisimx.scanbridge.export.zip.ZipExportModule
import io.github.chrisimx.scanbridge.export.zip.ZipService
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_EXPORT = module {



    // ZIP export module and dependencies
    single<KmpNativeZipBasedZipService>() bind ZipService::class
    single<ZipExportModule>() bind ExportModule::class

    // Export file management
    single<DefaultExportDirectoryProvider>() bind ExportDirectoryProvider::class
    single<DefaultExportFileManager>() bind ExportFileManager::class

    // Provides the export modules to the app
    single<KoinExportModuleManager>() bind ExportModuleManager::class
}
