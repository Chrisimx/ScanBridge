package io.github.chrisimx.scanbridge.repositories

import io.github.chrisimx.scanbridge.LastRouteRepository
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.LastRoute
import io.github.chrisimx.scanbridge.migrations.Migration

class RoomLastRouteRepository(db: ScanBridgeDb) : LastRouteRepository {
    val lastRouteDao = db.lastRouteDao()
    override suspend fun getLastRoute(): String? = lastRouteDao.getLastRoute()?.route

    override suspend fun setLastRoute(route: String?) {
        if (route != null) {
            lastRouteDao.setLastRoute(LastRoute(route))
        } else {
            lastRouteDao.clearLastRoute()
        }
    }
}

class LastRouteRepoAutoMigration(val a: LastRouteRepository, val b: LastRouteRepository, override val migrationId: String) : Migration {
    override suspend fun migrate(db: ScanBridgeDb): Boolean {
        val toBeMigrated = a.getLastRoute()
        b.setLastRoute(toBeMigrated)
        return true
    }
}
