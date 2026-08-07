package io.github.chrisimx.scanbridge.db.migrations

import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val ROOM_MIGRATIONS = module {
    single<RoomMigrationV5To6>()
}
