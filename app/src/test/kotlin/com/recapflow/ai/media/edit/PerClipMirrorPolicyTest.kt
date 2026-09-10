package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PerClipMirrorPolicyTest {

    @Test
    fun transformOffAlwaysOmitsMirror() {
        val settings = TransformSettings(
            enabled = false,
            mirrorEnabled = true,
            randomMirrorPerClipEnabled = true,
        )
        assertFalse(
            PerClipMirrorPolicy.shouldMirror(settings, TrimRange(0L, 3_000L), 0, 4),
        )
    }

    @Test
    fun globalMirrorKeepsExistingBehavior() {
        val settings = TransformSettings(enabled = true, mirrorEnabled = true)
        assertTrue(
            PerClipMirrorPolicy.shouldMirror(settings, TrimRange(3_000L, 6_000L), 1, 4),
        )
    }

    @Test
    fun randomModeIsInactiveForSingleClip() {
        val settings = TransformSettings(enabled = true, randomMirrorPerClipEnabled = true)
        assertFalse(
            PerClipMirrorPolicy.shouldMirror(settings, TrimRange(0L, 5_000L), 0, 1),
        )
    }

    @Test
    fun randomModeIsDeterministicForPreviewAndExport() {
        val settings = TransformSettings(enabled = true, randomMirrorPerClipEnabled = true)
        val ranges = listOf(
            TrimRange(0L, 3_000L),
            TrimRange(4_000L, 7_000L),
            TrimRange(8_000L, 11_000L),
            TrimRange(12_000L, 15_000L),
            TrimRange(16_000L, 19_000L),
            TrimRange(20_000L, 23_000L),
        )
        val first = ranges.mapIndexed { index, range ->
            PerClipMirrorPolicy.shouldMirror(settings, range, index, ranges.size)
        }
        val second = ranges.mapIndexed { index, range ->
            PerClipMirrorPolicy.shouldMirror(settings, range, index, ranges.size)
        }
        assertEquals(first, second)
        assertTrue(first.any { it })
        assertTrue(first.any { !it })
    }

    @Test
    fun resolvedSettingsConsumesRandomModeIntoPerItemMirror() {
        val settings = TransformSettings(enabled = true, randomMirrorPerClipEnabled = true)
        val resolved = PerClipMirrorPolicy.resolvedSettings(
            settings = settings,
            range = TrimRange(4_000L, 7_000L),
            rangeIndex = 1,
            clipCount = 3,
        )
        assertFalse(resolved.randomMirrorPerClipEnabled)
        assertEquals(
            PerClipMirrorPolicy.shouldMirror(settings, TrimRange(4_000L, 7_000L), 1, 3),
            resolved.mirrorEnabled,
        )
    }
}
