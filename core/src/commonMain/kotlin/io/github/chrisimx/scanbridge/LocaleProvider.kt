package io.github.chrisimx.scanbridge

import kotlinx.coroutines.flow.StateFlow

data class Locale(
    /**
     * The two character country code in uppercase
     */
    val country: String
)

/**
 * Provides the currently selected [Locale] as a reactive stream.
 */
interface LocaleProvider {
    val locale: StateFlow<Locale>
}
