package io.github.chrisimx.scanbridge.initialscansettings

import io.github.chrisimx.anyscan.CommonScanSettingsEditor
import io.github.chrisimx.anyscan.CommonScannerCapabilities
import io.github.chrisimx.scanbridge.appsettings.AppSettingsRepository

class DefaultInitialScanSettingsProvider(val appSettingsRepos: AppSettingsRepository) : InitialScanSettingsProvider {
    override suspend fun applyDefaults(editor: CommonScanSettingsEditor, capabilities: CommonScannerCapabilities) {
        val preferredInitialScanSettings = appSettingsRepos.getPreferredInitialScanSettings()

        editor.setSettings(preferredInitialScanSettings)
    }
}
