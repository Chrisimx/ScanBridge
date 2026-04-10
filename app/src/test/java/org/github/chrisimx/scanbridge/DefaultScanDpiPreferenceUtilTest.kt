package org.github.chrisimx.scanbridge

import io.github.chrisimx.scanbridge.util.DEFAULT_SCAN_DPI_MAX
import io.github.chrisimx.scanbridge.util.normalizeDefaultScanDpiPreference
import io.github.chrisimx.scanbridge.util.resolveDefaultScanDpiPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultScanDpiPreferenceUtilTest {

    @Test
    fun normalizeDefaultScanDpiPreference_snapsLegacyValueToNearestPreset() {
        assertEquals(300u, normalizeDefaultScanDpiPreference(400u))
        assertEquals(600u, normalizeDefaultScanDpiPreference(700u))
    }

    @Test
    fun resolveDefaultScanDpiPreference_mapsMaxSentinelToNull() {
        assertNull(resolveDefaultScanDpiPreference(DEFAULT_SCAN_DPI_MAX))
        assertEquals(300u, resolveDefaultScanDpiPreference(300u))
    }
}
