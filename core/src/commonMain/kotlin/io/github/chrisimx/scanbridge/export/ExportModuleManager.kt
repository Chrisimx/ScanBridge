package io.github.chrisimx.scanbridge.export

import kotlinx.coroutines.flow.StateFlow

interface ExportModuleManager {
    fun getExportModuleByType(type: ExportModuleType): ExportModule?
    fun getAllExportModulesFlow(): StateFlow<List<ExportModule>>
}
