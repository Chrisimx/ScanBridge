package io.github.chrisimx.scanbridge.util

const val DEFAULT_SCAN_DPI_MAX = 0u

val defaultScanDpiPresets = listOf(150u, 300u, 600u, DEFAULT_SCAN_DPI_MAX)

fun normalizeDefaultScanDpiPreference(value: UInt?): UInt {
    val resolvedValue = value ?: 300u
    if (resolvedValue == DEFAULT_SCAN_DPI_MAX) {
        return DEFAULT_SCAN_DPI_MAX
    }

    return defaultScanDpiPresets
        .filterNot { it == DEFAULT_SCAN_DPI_MAX }
        .minBy { kotlin.math.abs(it.toLong() - resolvedValue.toLong()) }
}

fun resolveDefaultScanDpiPreference(value: UInt?): UInt? {
    val normalizedValue = normalizeDefaultScanDpiPreference(value)
    return if (normalizedValue == DEFAULT_SCAN_DPI_MAX) {
        null
    } else {
        normalizedValue
    }
}
