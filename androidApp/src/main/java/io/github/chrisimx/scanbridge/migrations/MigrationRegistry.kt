package io.github.chrisimx.scanbridge.migrations

import io.github.chrisimx.scanbridge.repositories.DatastoreLastRouteRepository
import io.github.chrisimx.scanbridge.repositories.LastRouteRepoAutoMigration
import io.github.chrisimx.scanbridge.repositories.RoomLastRouteRepository
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create

fun createLastRouteRepoAutoMigration(datastoreLastRouteRepository: DatastoreLastRouteRepository, roomLastRouteRepository: RoomLastRouteRepository): LastRouteRepoAutoMigration {
    return LastRouteRepoAutoMigration(
        datastoreLastRouteRepository,
        roomLastRouteRepository,
        "LastRouteDataStoreToRoom"
    )
}

val migrationsModule = module {
    single<LastRouteRepoAutoMigration> {
        create(::createLastRouteRepoAutoMigration)
    } bind Migration::class
}
