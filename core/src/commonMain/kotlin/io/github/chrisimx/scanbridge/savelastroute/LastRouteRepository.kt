package io.github.chrisimx.scanbridge.savelastroute

interface LastRouteRepository {
    suspend fun getLastRoute(): String?
    suspend fun setLastRoute(route: String?)
}
