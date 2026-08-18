package io.github.chrisimx.scanbridge

import io.github.chrisimx.scanbridge.export.ExportCapabilitiesProvider
import io.github.chrisimx.scanbridge.scanning.FileSaveType

class AndroidExportCapabilitiesProvider : ExportCapabilitiesProvider {
    override val supportedFileSaveTypes: Set<FileSaveType> = setOf(FileSaveType.Save, FileSaveType.Share)
}
