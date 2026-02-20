package io.github.chrisimx.scanbridge.util

import android.annotation.SuppressLint
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

object LocaleProvider {
    @SuppressLint("ConstantLocale")
    private val _locale = MutableStateFlow(Locale.getDefault())
    val locale: StateFlow<Locale> = _locale.asStateFlow()

    internal fun update() {
        val locale = Locale.getDefault()
        Timber.d("Locale updated to $locale")
        _locale.value = locale
    }
}
