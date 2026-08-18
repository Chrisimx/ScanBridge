package io.github.chrisimx.scanbridge.migrations

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.savelastroute.LastRouteRepository

class LastRouteRepoAutoMigration(val a: LastRouteRepository, val b: LastRouteRepository, override val migrationId: String) : Migration {
    override suspend fun migrate(db: ScanBridgeDb): Boolean {
        val toBeMigrated = a.getLastRoute()
        b.setLastRoute(toBeMigrated)
        return true
    }
}
