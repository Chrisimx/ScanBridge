package io.github.chrisimx.scanbridge.services

import android.annotation.SuppressLint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale

class AndroidLocaleProvider : LocaleProvider {
    @SuppressLint("ConstantLocale")
    private val _locale = MutableStateFlow(Locale.getDefault())
    override val locale: StateFlow<Locale> = _locale.asStateFlow()

    internal fun update() {
        val locale = Locale.getDefault()
        Timber.Forest.d("Locale updated to $locale")
        _locale.value = locale
    }
}
