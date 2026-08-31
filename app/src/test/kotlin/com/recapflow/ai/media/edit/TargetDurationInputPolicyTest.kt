package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TargetDurationInputPolicyTest {
    @Test
    fun parsesArabicDigitMinutesAndSeconds() {
        assertEquals(60_000L, TargetDurationInputPolicy.parseDurationMs("1", "0"))
        assertEquals(125_000L, TargetDurationInputPolicy.parseDurationMs("2", "5"))
    }

    @Test
    fun acceptsBlankPartAsZero() {
        assertEquals(45_000L, TargetDurationInputPolicy.parseDurationMs("", "45"))
        assertEquals(120_000L, TargetDurationInputPolicy.parseDurationMs("2", ""))
    }

    @Test
    fun rejectsInvalidOrTooShortValues() {
        assertNull(TargetDurationInputPolicy.parseDurationMs("x", "10"))
        assertNull(TargetDurationInputPolicy.parseDurationMs("1", "60"))
        assertNull(TargetDurationInputPolicy.parseDurationMs("0", "1"))
        assertNull(TargetDurationInputPolicy.parseDurationMs("1000", "0"))
    }

    @Test
    fun splitsDurationForUiFields() {
        assertEquals(2, TargetDurationInputPolicy.minutesPart(125_000L))
        assertEquals(5, TargetDurationInputPolicy.secondsPart(125_000L))
    }

    @Test
    fun clampsSourceKeepPercent() {
        assertEquals(0, TargetDurationInputPolicy.sourceKeepPercent(-0.2))
        assertEquals(33, TargetDurationInputPolicy.sourceKeepPercent(0.333))
        assertEquals(100, TargetDurationInputPolicy.sourceKeepPercent(1.2))
    }
}
