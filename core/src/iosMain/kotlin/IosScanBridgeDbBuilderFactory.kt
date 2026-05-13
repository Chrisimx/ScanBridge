import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.ScanBridgeDbBuilderFactory
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

class IosScanBridgeDbBuilderFactory : ScanBridgeDbBuilderFactory {
    override fun getBuilder(): RoomDatabase.Builder<ScanBridgeDb> {
        val dbFilePath = documentDirectory() + "/scanbridge.db"
        return Room.databaseBuilder<ScanBridgeDb>(
            name = dbFilePath
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        return requireNotNull(documentDirectory?.path)
    }
}
