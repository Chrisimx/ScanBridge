package io.github.chrisimx.scanbridge.export

import io.github.chrisimx.scanbridge.scanning.FileSaveType

interface ExportCapabilitiesProvider {
    val supportedFileSaveTypes: Set<FileSaveType>
}
