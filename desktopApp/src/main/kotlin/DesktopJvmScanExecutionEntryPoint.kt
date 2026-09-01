import io.github.chrisimx.scanbridge.scanning.ScanExecutionEntryPoint
import io.github.chrisimx.scanbridge.scanning.ScanExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DesktopJvmScanExecutionEntryPoint(
    private val scanExecutor: ScanExecutor
) : ScanExecutionEntryPoint {
    val scanExecutionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun start() {
        scanExecutionScope.launch {
            scanExecutor.executeScans()
        }
    }
}
