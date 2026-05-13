package io.github.chrisimx.scanbridge.ports

import io.github.chrisimx.scanbridge.model.Locale
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides the currently selected [io.github.chrisimx.scanbridge.model.Locale] as a reactive stream.
 */
interface LocaleProvider {
    val locale: StateFlow<Locale>
}
