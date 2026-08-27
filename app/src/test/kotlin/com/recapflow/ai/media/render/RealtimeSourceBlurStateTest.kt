package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.BlurRectangle
import com.recapflow.ai.media.edit.CompiledSourceSubtitleBlur
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RealtimeSourceBlurStateTest {

    @Test
    fun latestSnapshotReplacesStaleBlurGeometryAndStrength() {
        val state = RealtimeSourceBlurState()
        state.update(compiled(left = 0.10f, strength = 8f))
        state.update(compiled(left = 0.22f, strength = 20f))

        assertEquals(0.22f, state.snapshot()?.rectangle?.left)
        assertEquals(20f, state.snapshot()?.strength)
    }

    @Test
    fun disabledBlurCannotLeakThroughRetainedShader() {
        val state = RealtimeSourceBlurState()
        state.update(compiled(left = 0.10f, strength = 14f))

        state.update(null)

        assertNull(state.snapshot())
    }

    private fun compiled(left: Float, strength: Float) = CompiledSourceSubtitleBlur(
        rectangle = BlurRectangle(left = left, top = 0.70f, right = 0.90f, bottom = 0.90f),
        strength = strength,
        startMs = 500L,
        endMs = 5_000L,
    )
}
