package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpeedCompilerTest {
    @Test
    fun masterOffAndSpeedOffAreNoOps() {
        assertNull(
            SpeedCompiler.compile(
                TransformSettings(enabled = false, speedEnabled = true, speed = 1.5f),
            ),
        )
        assertNull(
            SpeedCompiler.compile(
                TransformSettings(enabled = true, speedEnabled = false, speed = 1.5f),
            ),
        )
    }

    @Test
    fun neutralSpeedIsAnExplicitNoOp() {
        assertNull(
            SpeedCompiler.compile(
                TransformSettings(enabled = true, speedEnabled = true, speed = 1f),
            ),
        )
    }

    @Test
    fun supportedSpeedCompilesAndChangesPlannedDuration() {
        val compiled = SpeedCompiler.compile(
            TransformSettings(enabled = true, speedEnabled = true, speed = 1.25f),
        )

        assertEquals(1.25f, compiled?.multiplier)
        assertEquals(8_000L, compiled?.outputDurationMs(10_000L))
    }

    @Test
    fun outOfRangeSpeedDoesNotCompile() {
        assertNull(
            SpeedCompiler.compile(
                TransformSettings(enabled = true, speedEnabled = true, speed = 2.5f),
            ),
        )
    }
}
