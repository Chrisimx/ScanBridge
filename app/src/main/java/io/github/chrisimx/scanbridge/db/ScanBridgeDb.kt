package io.github.chrisimx.scanbridge.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.chrisimx.scanbridge.db.daos.CustomScannerDao
import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import io.github.chrisimx.scanbridge.db.typeconverters.UrlTypeConverter
import io.github.chrisimx.scanbridge.db.typeconverters.UuidTypeConverter

@Database(entities = [CustomScanner::class], version = 1)
@TypeConverters(
    UuidTypeConverter::class,
    UrlTypeConverter::class
)
abstract class ScanBridgeDb : RoomDatabase() {
    abstract fun customScannerDao(): CustomScannerDao
}
