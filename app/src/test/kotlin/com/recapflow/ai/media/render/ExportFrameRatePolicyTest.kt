package com.recapflow.ai.media.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExportFrameRatePolicyTest {
    @Test fun normalizesCommonFractionalRates() {
        assertEquals(24, ExportFrameRatePolicy.forSource(23.976))
        assertEquals(30, ExportFrameRatePolicy.forSource(29.97))
        assertEquals(60, ExportFrameRatePolicy.forSource(59.94))
    }
    @Test fun preservesSourceClassWithoutPromotingNormalFps() {
        assertEquals(25, ExportFrameRatePolicy.forSource(25.0))
        assertEquals(30, ExportFrameRatePolicy.forSource(30.0))
        assertEquals(50, ExportFrameRatePolicy.forSource(50.0))
        assertFalse(ExportFrameRatePolicy.isHighFrameRate(30))
        assertTrue(ExportFrameRatePolicy.isHighFrameRate(50))
    }
    @Test fun capsVeryHighSourceAndFallsBackForInvalidMetadata() {
        assertEquals(60, ExportFrameRatePolicy.forSource(120.0))
        assertEquals(30, ExportFrameRatePolicy.forSource(0.0))
        assertEquals(30, ExportFrameRatePolicy.forSource(Double.NaN))
    }
}
