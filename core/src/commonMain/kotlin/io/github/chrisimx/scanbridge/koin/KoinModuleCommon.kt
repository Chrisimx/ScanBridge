package io.github.chrisimx.scanbridge.koin

import io.github.chrisimx.scanbridge.appsettings.KOIN_MODULE_APP_SETTINGS
import io.github.chrisimx.scanbridge.cropfeature.KOIN_MODULE_CROP_FEAT
import io.github.chrisimx.scanbridge.protocol.KOIN_MODULE_SCAN_PROTOCOLS
import io.github.chrisimx.scanbridge.db.migrations.KOIN_MODULE_ROOM_MIGRATIONS
import io.github.chrisimx.scanbridge.export.KOIN_MODULE_EXPORT
import io.github.chrisimx.scanbridge.filesystem.KOIN_MODULE_FILESYSTEM
import io.github.chrisimx.scanbridge.logging.KOIN_MODULE_LOGGING
import io.github.chrisimx.scanbridge.savelastroute.KOIN_MODULE_LAST_ROUTE_STORE
import io.github.chrisimx.scanbridge.scanning.KOIN_MODULE_SCANNING_USE_CASES
import io.github.chrisimx.scanbridge.db.KOIN_MODULE_DATABASE
import io.github.chrisimx.scanbridge.initialscansettings.KOIN_MODULE_INITIAL_SCAN_SETTINGS
import io.github.chrisimx.scanbridge.paperformat.KOIN_MODULE_PAPER_FORMAT
import io.github.chrisimx.scanbridge.protocol.KOIN_MODULE_SCANNING_PROTOCOL_FEAT
import io.github.chrisimx.scanbridge.savelastusedscansettings.KOIN_MODULE_LAST_USED_SCAN_SETTINGS_STORE
import io.github.chrisimx.scanbridge.scannerdiscovery.KOIN_MODULE_SCANNER_DISCOVERY
import io.github.chrisimx.scanbridge.scanning.KOIN_MODULE_SCANNING_SCREEN
import io.github.chrisimx.scanbridge.scanning.KOIN_MODULE_SCAN_JOB_MANAGEMENT
import io.github.chrisimx.scanbridge.startupmessages.KOIN_MODULE_STARTUP_MESSAGES
import org.koin.dsl.module

val KOIN_MODULE_COMMON = module {
    includes(
        KOIN_MODULE_SCAN_JOB_MANAGEMENT,
        KOIN_MODULE_SCANNING_SCREEN,
        KOIN_MODULE_SCANNING_USE_CASES,
        KOIN_MODULE_EXPORT,
        KOIN_MODULE_SCANNING_PROTOCOL_FEAT,
        KOIN_MODULE_SCAN_PROTOCOLS,
        KOIN_MODULE_ROOM_MIGRATIONS,
        KOIN_MODULE_FILESYSTEM,
        KOIN_MODULE_LOGGING,
        KOIN_MODULE_LAST_ROUTE_STORE,
        KOIN_MODULE_LAST_USED_SCAN_SETTINGS_STORE,
        KOIN_MODULE_DATABASE,
        KOIN_MODULE_STARTUP_MESSAGES,
        KOIN_MODULE_APP_SETTINGS,
        KOIN_MODULE_SCANNER_DISCOVERY,
        KOIN_MODULE_PAPER_FORMAT,
        KOIN_MODULE_INITIAL_SCAN_SETTINGS,
        KOIN_MODULE_CROP_FEAT
    )
}
