package io.github.chrisimx.scanbridge.db

import ROOM_MIGRATION_4_5
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.chrisimx.scanbridge.db.migrations.RoomMigrationV5To6
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

interface ScanBridgeDbFactory {
    fun createInstance(): ScanBridgeDb
}

class DefaultScanBridgeDbFactory(val builderFactory: ScanBridgeDbBuilderFactory, val migrationV5To6: RoomMigrationV5To6) :
    ScanBridgeDbFactory {
    override fun createInstance(): ScanBridgeDb {
        val dbBuilder = builderFactory.getBuilder()
        return dbBuilder
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(ROOM_MIGRATION_4_5, migrationV5To6)
            .build()
    }
}
