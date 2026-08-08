package io.github.chrisimx.scanbridge.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.chrisimx.scanbridge.db.daos.AppSettingsDao
import io.github.chrisimx.scanbridge.db.daos.CustomScannerDao
import io.github.chrisimx.scanbridge.db.daos.ExecutedMigrationsDao
import io.github.chrisimx.scanbridge.db.daos.LastRouteDao
import io.github.chrisimx.scanbridge.db.daos.LastUsedScanSettingsDao
import io.github.chrisimx.scanbridge.db.daos.ScannedPageDao
import io.github.chrisimx.scanbridge.db.daos.SessionDao
import io.github.chrisimx.scanbridge.db.daos.ShownStartupMessageDao
import io.github.chrisimx.scanbridge.db.daos.TempFileDao
import io.github.chrisimx.scanbridge.db.entities.AppSettings
import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import io.github.chrisimx.scanbridge.db.entities.ExecutedMigrationToRoom
import io.github.chrisimx.scanbridge.db.entities.LastRoute
import io.github.chrisimx.scanbridge.db.entities.LastUsedScanSettings
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.db.entities.Session
import io.github.chrisimx.scanbridge.db.entities.ShownStartupMessage
import io.github.chrisimx.scanbridge.db.entities.TempFile
import io.github.chrisimx.scanbridge.db.typeconverters.CommonScanSettingsTypeConverter
import io.github.chrisimx.scanbridge.db.typeconverters.ScanSettingsTypeConverter
import io.github.chrisimx.scanbridge.db.typeconverters.ScanSettingsUiDataTypeConverterV0
import io.github.chrisimx.scanbridge.db.typeconverters.ScanSettingsUiDataTypeConverterV1
import io.github.chrisimx.scanbridge.db.typeconverters.UrlTypeConverter
import io.github.chrisimx.scanbridge.db.typeconverters.UuidTypeConverter

@Database(
    entities = [
        CustomScanner::class, ScannedPage::class,
        Session::class, TempFile::class,
        LastRoute::class, ExecutedMigrationToRoom::class,
        ShownStartupMessage::class, AppSettings::class,
        LastUsedScanSettings::class
    ],
    version = 7,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2
        ),
        AutoMigration(
            from = 2,
            to = 3
        ),
        AutoMigration(
            from = 3,
            to = 4
        )
    ]
)
@TypeConverters(
    UuidTypeConverter::class,
    UrlTypeConverter::class,
    ScanSettingsTypeConverter::class,
    CommonScanSettingsTypeConverter::class,
    ScanSettingsUiDataTypeConverterV0::class,
    ScanSettingsUiDataTypeConverterV1::class,
    CommonScanSettingsTypeConverter::class
)
abstract class ScanBridgeDb : RoomDatabase() {
    abstract fun customScannerDao(): CustomScannerDao
    abstract fun scannedPageDao(): ScannedPageDao
    abstract fun sessionDao(): SessionDao
    abstract fun tmpFileDao(): TempFileDao
    abstract fun lastRouteDao(): LastRouteDao
    abstract fun executedMigrationsDao(): ExecutedMigrationsDao
    abstract fun shownStartupMessageDao(): ShownStartupMessageDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun lastUsedScanSettingsDao(): LastUsedScanSettingsDao
}
