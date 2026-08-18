package io.github.chrisimx.scanbridge.savelastroute

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_LAST_ROUTE_STORE = module {
    single<RoomLastRouteRepository>() bind LastRouteRepository::class
}
