package io.github.chrisimx.scanbridge.ports

import io.github.chrisimx.anyscan.CommonScanSettingsEditor
import io.github.chrisimx.anyscan.CommonScannerCapabilities

interface InitialScanSettingsProvider {
    fun applyDefaults(editor: CommonScanSettingsEditor, capabilities: CommonScannerCapabilities)
}
