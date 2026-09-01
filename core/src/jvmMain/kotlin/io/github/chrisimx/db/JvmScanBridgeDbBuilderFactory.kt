package io.github.chrisimx.db

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.ScanBridgeDbBuilderFactory
import io.github.chrisimx.scanbridge.filesystem.StandardDirectoryProvider
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath

class JvmScanBridgeDbBuilderFactory(
    val standardDirProvider: StandardDirectoryProvider
) : ScanBridgeDbBuilderFactory {
    override fun getBuilder(): RoomDatabase.Builder<ScanBridgeDb> {
        val databasePath = PlatformFile(standardDirProvider.databaseDirectory, "scanbridge.sqlite3")
        return Room.databaseBuilder<ScanBridgeDb>(databasePath.absolutePath())
    }
}
