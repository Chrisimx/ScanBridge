package io.github.chrisimx.scanbridge.services

import java.util.Locale
import kotlinx.coroutines.flow.StateFlow

interface LocaleProvider {
    val locale: StateFlow<Locale>
}

