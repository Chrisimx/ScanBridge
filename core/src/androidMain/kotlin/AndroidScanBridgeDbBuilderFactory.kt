import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.ScanBridgeDbBuilderFactory

class AndroidScanBridgeDbBuilderFactory(private val context: Context) : ScanBridgeDbBuilderFactory {
    override fun getBuilder(): RoomDatabase.Builder<ScanBridgeDb> {
        val databaseFile = context.getDatabasePath("scanbridge")
        val builder = Room.databaseBuilder<ScanBridgeDb>(context, databaseFile.absolutePath)
        return builder
    }
}
