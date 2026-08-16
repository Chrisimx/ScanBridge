package io.github.chrisimx.scanbridge

import io.github.chrisimx.scanbridge.export.ExportCapabilitiesProvider

class AndroidExportCapabilitiesProvider : ExportCapabilitiesProvider {
    override val supportedFileSaveTypes: Set<FileSaveType> = setOf(FileSaveType.Save, FileSaveType.Share)
}
