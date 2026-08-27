package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FreezeCompilerTest {
    @Test
    fun masterOffAndFreezeOffAreNoOps() {
        assertNull(
            FreezeCompiler.compile(
                TransformSettings(
                    enabled = false,
                    freeze = FreezeSettings(enabled = true, durationMs = 2_000L),
                ),
            ),
        )
        assertNull(
            FreezeCompiler.compile(
                TransformSettings(
                    enabled = true,
                    freeze = FreezeSettings(enabled = false, durationMs = 2_000L),
                ),
            ),
        )
    }

    @Test
    fun supportedDurationCompiles() {
        val compiled = FreezeCompiler.compile(
            TransformSettings(
                enabled = true,
                freeze = FreezeSettings(enabled = true, durationMs = 3_000L),
            ),
        )

        assertEquals(3_000L, compiled?.durationMs)
    }

    @Test
    fun unsupportedDurationDoesNotCompile() {
        assertNull(
            FreezeCompiler.compile(
                TransformSettings(
                    enabled = true,
                    freeze = FreezeSettings(enabled = true, durationMs = 1_500L),
                ),
            ),
        )
    }
}
