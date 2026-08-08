package io.github.chrisimx.scanbridge.db

import ROOM_MIGRATION_4_5
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.chrisimx.scanbridge.db.migrations.ROOM_MIGRATION_7_8
import io.github.chrisimx.scanbridge.db.migrations.RoomMigrationVersion5To6
import io.github.chrisimx.scanbridge.db.migrations.RoomMigrationVersion6To7
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

interface ScanBridgeDbFactory {
    fun createInstance(): ScanBridgeDb
}

class DefaultScanBridgeDbFactory(
    val builderFactory: ScanBridgeDbBuilderFactory,
    val migrationV5To6: RoomMigrationVersion5To6,
    val migrationV6To7: RoomMigrationVersion6To7
) : ScanBridgeDbFactory {
    override fun createInstance(): ScanBridgeDb {
        val dbBuilder = builderFactory.getBuilder()
        return dbBuilder
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(
                ROOM_MIGRATION_4_5,
                migrationV5To6,
                migrationV6To7,
                ROOM_MIGRATION_7_8
            )
            .build()
    }
}
