package io.github.chrisimx.scanbridge.services

import android.annotation.SuppressLint
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class AndroidLocaleProvider : LocaleProvider {
    @SuppressLint("ConstantLocale")
    private val _locale = MutableStateFlow(Locale.getDefault())
    override val locale: StateFlow<Locale> = _locale.asStateFlow()

    internal fun update() {
        val locale = Locale.getDefault()
        Timber.d("Locale updated to $locale")
        _locale.value = locale
    }
}
