package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CropCompilerTest {

    @Test
    fun masterOffOmitsRememberedCrop() {
        val settings = TransformSettings(
            enabled = false,
            crop = CropSettings(enabled = true),
        )

        assertNull(CropCompiler.compile(settings))
    }

    @Test
    fun cropOffOmitsRememberedRectangle() {
        val settings = TransformSettings(
            enabled = true,
            crop = CropSettings(enabled = false),
        )

        assertNull(CropCompiler.compile(settings))
    }

    @Test
    fun compilesTopLeftCoordinatesToMedia3Ndc() {
        val settings = TransformSettings(
            enabled = true,
            crop = CropSettings(
                enabled = true,
                rectangle = CropRectangle(
                    left = 0.10f,
                    top = 0.20f,
                    right = 0.80f,
                    bottom = 0.90f,
                ),
            ),
        )

        val compiled = requireNotNull(CropCompiler.compile(settings))
        assertEquals(-0.8f, compiled.leftNdc, absoluteTolerance = 0.0001f)
        assertEquals(0.6f, compiled.rightNdc, absoluteTolerance = 0.0001f)
        assertEquals(-0.8f, compiled.bottomNdc, absoluteTolerance = 0.0001f)
        assertEquals(0.6f, compiled.topNdc, absoluteTolerance = 0.0001f)
    }
}
