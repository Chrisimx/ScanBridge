package io.github.chrisimx.scanbridge.migrations

import io.github.chrisimx.scanbridge.db.ScanBridgeDb

interface Migration {
    /**
     * A unique identifier of the migration that will be used to determine if it has already run
     */
    val migrationId: String

    /**
     * Execute the migration
     * @return Whether the migration was successful
     */
    suspend fun migrate(db: ScanBridgeDb): Boolean
}
