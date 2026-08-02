package io.github.chrisimx.scanbridge

import io.github.chrisimx.anyscan.CommonScanSettingsEditor
import io.github.chrisimx.anyscan.CommonScannerCapabilities
import io.github.chrisimx.anyscan.FileFormat
import io.github.chrisimx.enumorrawcodegen.AnyScanEnumOrRaw
import io.github.chrisimx.scanbridge.ports.InitialScanSettingsProvider

class DefaultInitialScanSettingsProvider : InitialScanSettingsProvider {
    override fun applyDefaults(editor: CommonScanSettingsEditor, capabilities: CommonScannerCapabilities) {
        editor.setInputSource(capabilities.inputSources.first().inputSourceType)
        editor.setFormat(AnyScanEnumOrRaw.Known(FileFormat.JPEG))
    }
}
