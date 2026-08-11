package io.github.chrisimx.scanbridge.export

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.scope.Scope

class KoinExportModuleManager(
    koinScope: Scope
) : ExportModuleManager {
    val exportModules = koinScope.getAll<ExportModule>()
    val exportModuleById = exportModules.associateBy { it.type }

    val exportModulesStateFlow = MutableStateFlow(exportModules).asStateFlow()

    override fun getExportModuleByType(type: ExportModuleType): ExportModule? {
        return exportModuleById[type]?.let { return it }
    }

    override fun getAllExportModulesFlow(): StateFlow<List<ExportModule>> {
        return exportModulesStateFlow
    }
}
