import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.chrisimx.scanbridge.ScanBridgeApp
import io.github.chrisimx.scanbridge.koin.KOIN_MODULE_COMMON
import io.github.vinceglb.filekit.FileKit
import java.awt.Dimension
import org.koin.core.context.startKoin

fun main() {
    FileKit.init("io.github.chrisimx.scanbridge")
    startKoin {
        modules(KOIN_MODULE_JVM_PLATFORM, KOIN_MODULE_JVM_AND_ANDROID_PLATFORM, KOIN_MODULE_COMMON)
    }

    application {
        Window(
            title = "ScanBridge",
            state = rememberWindowState(width = 800.dp, height = 600.dp),
            onCloseRequest = ::exitApplication
        ) {
            window.minimumSize = Dimension(350, 600)
            ScanBridgeApp()
        }
    }
}
