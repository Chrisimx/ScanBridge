package io.github.chrisimx.scanbridge.savelastroute

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.LastRoute

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
