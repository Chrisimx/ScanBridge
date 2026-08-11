package io.github.chrisimx.scanbridge.db.migrations

import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_ROOM_MIGRATIONS = module {
    single<RoomMigrationVersion5To6>()
    single<RoomMigrationVersion6To7>()
}
