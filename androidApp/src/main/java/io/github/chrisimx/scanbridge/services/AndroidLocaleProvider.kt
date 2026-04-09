package io.github.chrisimx.scanbridge.services

import android.annotation.SuppressLint
import io.github.chrisimx.scanbridge.Locale
import io.github.chrisimx.scanbridge.LocaleProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class AndroidLocaleProvider : LocaleProvider {
    @SuppressLint("ConstantLocale") // Constant local is not correct because update is called on Activity recreation
    private val _locale = MutableStateFlow(getCurrentLocale())
    override val locale: StateFlow<Locale> = _locale.asStateFlow()

    private fun getCurrentLocale(): Locale = Locale(java.util.Locale.getDefault().country)

    internal fun update() {
        Timber.d("Locale updated to $locale")
        _locale.value = getCurrentLocale()
    }
}
