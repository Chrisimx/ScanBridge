package io.github.chrisimx.scanbridge.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.chrisimx.scanbridge.db.daos.CustomScannerDao
import io.github.chrisimx.scanbridge.db.daos.ScannedPageDao
import io.github.chrisimx.scanbridge.db.daos.SessionDao
import io.github.chrisimx.scanbridge.db.daos.TempFileDao
import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.db.entities.Session
import io.github.chrisimx.scanbridge.db.entities.TempFile
import io.github.chrisimx.scanbridge.db.typeconverters.ScanSettingsTypeConverter
import io.github.chrisimx.scanbridge.db.typeconverters.UrlTypeConverter
import io.github.chrisimx.scanbridge.db.typeconverters.UuidTypeConverter

@Database(entities = [CustomScanner::class, ScannedPage::class, Session::class, TempFile::class], version = 1)
@TypeConverters(
    UuidTypeConverter::class,
    UrlTypeConverter::class,
    ScanSettingsTypeConverter::class
)
abstract class ScanBridgeDb : RoomDatabase() {
    abstract fun customScannerDao(): CustomScannerDao
    abstract fun scannedPageDao(): ScannedPageDao
    abstract fun sessionDao(): SessionDao
    abstract fun tmpFileDao(): TempFileDao
}
