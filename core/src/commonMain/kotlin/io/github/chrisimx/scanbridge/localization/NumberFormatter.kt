package io.github.chrisimx.scanbridge.localization

interface NumberFormatter {
    fun parseDouble(value: String): Double?
    fun formatDouble(value: Double): String
}

fun String.parseDouble(formatter: NumberFormatter): Double? = formatter.parseDouble(this)

fun Double.formatDouble(formatter: NumberFormatter): String = formatter.formatDouble(this)
