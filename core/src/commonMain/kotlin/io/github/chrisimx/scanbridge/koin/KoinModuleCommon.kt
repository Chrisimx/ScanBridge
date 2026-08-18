package io.github.chrisimx.scanbridge.koin

import io.github.chrisimx.scanbridge.protocol.KOIN_MODULE_SCAN_PROTOCOLS
import io.github.chrisimx.scanbridge.db.migrations.KOIN_MODULE_ROOM_MIGRATIONS
import io.github.chrisimx.scanbridge.export.KOIN_MODULE_EXPORT
import io.github.chrisimx.scanbridge.filesystem.KOIN_MODULE_FILESYSTEM
import io.github.chrisimx.scanbridge.logging.KOIN_MODULE_LOGGING
import io.github.chrisimx.scanbridge.savelastroute.KOIN_MODULE_LAST_ROUTE_STORE
import io.github.chrisimx.scanbridge.scanning.KOIN_MODULE_SCANNING_USE_CASES
import org.koin.dsl.module

val KOIN_MODULE_COMMON = module {
    includes(
        KOIN_MODULE_SCANNING_USE_CASES,
        KOIN_MODULE_EXPORT,
        KOIN_MODULE_SCAN_PROTOCOLS,
        KOIN_MODULE_ROOM_MIGRATIONS,
        KOIN_MODULE_FILESYSTEM,
        KOIN_MODULE_LOGGING,
        KOIN_MODULE_LAST_ROUTE_STORE
    )
}
