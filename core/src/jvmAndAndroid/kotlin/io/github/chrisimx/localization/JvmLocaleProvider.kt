package io.github.chrisimx.localization

import io.github.chrisimx.scanbridge.localization.LocaleProvider
import io.github.chrisimx.scanbridge.logging.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.model.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JvmLocaleProvider(
    loggerFactory: ScanBridgeLoggerFactory
) : LocaleProvider {

    private val logger = loggerFactory.withClass(this::class)
    private val _locale = MutableStateFlow(getCurrentLocale())
    override val locale: StateFlow<Locale> = _locale.asStateFlow()

    private fun getCurrentLocale(): Locale = Locale(java.util.Locale.getDefault().country)

    internal fun update() {
        logger.debug { "Locale updated to $locale" }
        _locale.value = getCurrentLocale()
    }
}
