package io.github.chrisimx.scanbridge.migrations.ds2room

import io.github.chrisimx.scanbridge.appsettings.D2RAppSettingsMigrationDataSource
import io.github.chrisimx.scanbridge.savelastusedscansettings.D2RLastUsedScanSettingsMigrationSource
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val DATASTORE_TO_ROOM_MIGRATION_DATA_SOURCES = module {
    single<D2RAppSettingsMigrationDataSourceImpl>() bind D2RAppSettingsMigrationDataSource::class
    single<D2RLastUsedScanSettingsMigrationDataSourceImpl>() bind D2RLastUsedScanSettingsMigrationSource::class
}
