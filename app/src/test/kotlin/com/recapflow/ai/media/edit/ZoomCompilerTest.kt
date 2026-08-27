package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZoomCompilerTest {

    @Test
    fun transformOffOmitsRememberedZoom() {
        val settings = TransformSettings(
            enabled = false,
            zoom = ZoomSettings(enabled = true, mode = ZoomMode.ALTERNATE),
        )

        assertNull(ZoomCompiler.compile(settings))
    }

    @Test
    fun zoomOffAndExplicitOffModeAreNoOps() {
        assertNull(
            ZoomCompiler.compile(
                TransformSettings(
                    enabled = true,
                    zoom = ZoomSettings(enabled = false, mode = ZoomMode.IN),
                ),
            ),
        )
        assertNull(
            ZoomCompiler.compile(
                TransformSettings(
                    enabled = true,
                    zoom = ZoomSettings(enabled = true, mode = ZoomMode.OFF),
                ),
            ),
        )
    }

    @Test
    fun staticModesCompileToExpectedCenteredScale() {
        val zoomIn = ZoomCompiler.compile(
            TransformSettings(
                enabled = true,
                zoom = ZoomSettings(enabled = true, mode = ZoomMode.IN),
            ),
        )
        val zoomOut = ZoomCompiler.compile(
            TransformSettings(
                enabled = true,
                zoom = ZoomSettings(enabled = true, mode = ZoomMode.OUT),
            ),
        )

        assertEquals(ZoomCompiler.IN_SCALE, zoomIn?.scaleAt(1_000_000L))
        assertEquals(ZoomCompiler.OUT_SCALE, zoomOut?.scaleAt(1_000_000L))
    }

    @Test
    fun alternateModeUsesRepeatableFourSecondCycle() {
        val compiled = ZoomCompiler.compile(
            TransformSettings(
                enabled = true,
                zoom = ZoomSettings(enabled = true, mode = ZoomMode.ALTERNATE),
            ),
        )
        requireNotNull(compiled)

        assertNear(1.00f, compiled.scaleAt(0L))
        assertNear(1.10f, compiled.scaleAt(1_000_000L))
        assertNear(1.00f, compiled.scaleAt(2_000_000L))
        assertNear(0.90f, compiled.scaleAt(3_000_000L))
        assertNear(1.00f, compiled.scaleAt(4_000_000L))
    }

    private fun assertNear(expected: Float, actual: Float) {
        assertTrue(kotlin.math.abs(expected - actual) < 0.0001f)
    }
}
