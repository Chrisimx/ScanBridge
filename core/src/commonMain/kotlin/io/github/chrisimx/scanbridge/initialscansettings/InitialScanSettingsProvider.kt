package io.github.chrisimx.scanbridge.initialscansettings

import io.github.chrisimx.anyscan.CommonScanSettingsEditor
import io.github.chrisimx.anyscan.CommonScannerCapabilities

interface InitialScanSettingsProvider {
    suspend fun applyDefaults(editor: CommonScanSettingsEditor, capabilities: CommonScannerCapabilities)
}
