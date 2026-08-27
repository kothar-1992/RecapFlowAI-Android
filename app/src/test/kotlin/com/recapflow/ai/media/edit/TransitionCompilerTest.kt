package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransitionCompilerTest {
    @Test
    fun disabledStatesAreNoOps() {
        assertNull(
            TransitionCompiler.compile(
                TransformSettings(
                    enabled = false,
                    transition = TransitionSettings(enabled = true),
                ),
                sourceDurationMs = 10_000L,
            ),
        )
        assertNull(
            TransitionCompiler.compile(
                TransformSettings(
                    enabled = true,
                    transition = TransitionSettings(enabled = false),
                ),
                sourceDurationMs = 10_000L,
            ),
        )
    }

    @Test
    fun fadeInOutUsesBlackEndpointsAndFullGainInMiddle() {
        val compiled = checkNotNull(
            TransitionCompiler.compile(
                TransformSettings(
                    enabled = true,
                    transition = TransitionSettings(
                        enabled = true,
                        mode = TransitionMode.FADE_IN_OUT,
                        durationMs = 1_000L,
                    ),
                ),
                sourceDurationMs = 10_000L,
            ),
        )

        assertEquals(0f, compiled.gainAt(0L))
        assertEquals(1f, compiled.gainAt(5_000_000L))
        assertEquals(0f, compiled.gainAt(10_000_000L))
    }

    @Test
    fun speedScalesSourceSpanToKeepOutputDurationStable() {
        val compiled = checkNotNull(
            TransitionCompiler.compile(
                TransformSettings(
                    enabled = true,
                    speedEnabled = true,
                    speed = 2f,
                    transition = TransitionSettings(
                        enabled = true,
                        mode = TransitionMode.FADE_IN,
                        durationMs = 1_000L,
                    ),
                ),
                sourceDurationMs = 10_000L,
            ),
        )

        assertEquals(2_000_000L, compiled.sourceFadeDurationUs)
        assertTrue(compiled.gainAt(1_000_000L) in 0.49f..0.51f)
        assertEquals(1f, compiled.gainAt(2_000_000L))
    }

    @Test
    fun unsupportedDurationDoesNotCompile() {
        assertNull(
            TransitionCompiler.compile(
                TransformSettings(
                    enabled = true,
                    transition = TransitionSettings(
                        enabled = true,
                        mode = TransitionMode.FADE_OUT,
                        durationMs = 750L,
                    ),
                ),
                sourceDurationMs = 10_000L,
            ),
        )
    }
}
