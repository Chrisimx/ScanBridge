package io.github.chrisimx.scanbridge.migrations

import io.github.chrisimx.scanbridge.repositories.DatastoreLastRouteRepository
import io.github.chrisimx.scanbridge.repositories.LastRouteRepoAutoMigration
import io.github.chrisimx.scanbridge.repositories.RoomLastRouteRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val migrationsModule = module {
    single<LastRouteRepoAutoMigration> {
        LastRouteRepoAutoMigration(
            get<DatastoreLastRouteRepository>(),
            get<RoomLastRouteRepository>(),
            "LastRouteDataStoreToRoom"
        )
    } bind Migration::class
}
