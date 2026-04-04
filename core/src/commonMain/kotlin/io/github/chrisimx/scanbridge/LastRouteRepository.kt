package io.github.chrisimx.scanbridge

interface LastRouteRepository {
    suspend fun getLastRoute(): String?
    suspend fun setLastRoute(route: String?)
}
