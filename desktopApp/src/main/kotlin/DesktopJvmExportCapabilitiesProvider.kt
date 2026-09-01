import io.github.chrisimx.scanbridge.export.ExportCapabilitiesProvider
import io.github.chrisimx.scanbridge.scanning.FileSaveType

class DesktopJvmExportCapabilitiesProvider : ExportCapabilitiesProvider {
    override val supportedFileSaveTypes: Set<FileSaveType> = setOf(FileSaveType.Save)
}
