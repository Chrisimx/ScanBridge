package org.github.chrisimx.scanbridge

import io.github.chrisimx.esclkt.DiscreteResolution
import io.github.chrisimx.scanbridge.util.pickClosestResolution
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanResolutionPreferenceUtilTest {

    @Test
    fun pickClosestResolution_returnsExactMatchWhenAvailable() {
        val resolutions = listOf(
            DiscreteResolution(200u, 200u),
            DiscreteResolution(300u, 300u),
            DiscreteResolution(600u, 600u)
        )

        assertEquals(
            DiscreteResolution(300u, 300u),
            pickClosestResolution(resolutions, 300u)
        )
    }

    @Test
    fun pickClosestResolution_prefersNearestSupportedValue() {
        val resolutions = listOf(
            DiscreteResolution(200u, 200u),
            DiscreteResolution(600u, 600u)
        )

        assertEquals(
            DiscreteResolution(200u, 200u),
            pickClosestResolution(resolutions, 300u)
        )
    }
}
