package io.github.chrisimx.localization

import io.github.chrisimx.scanbridge.localization.NumberFormatter
import java.text.NumberFormat

class JvmNumberFormatter : NumberFormatter {
    override fun parseDouble(value: String): Double? = runCatching {
        NumberFormat.getInstance().parse(value).toDouble()
    }.getOrNull()

    override fun formatDouble(value: Double): String = NumberFormat.getInstance().format(value)
}
