package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorCompilerTest {

    @Test
    fun transformOffOmitsRememberedColor() {
        val settings = TransformSettings(
            enabled = false,
            color = ColorSettings(enabled = true, brightness = 25f),
        )

        assertNull(ColorCompiler.compile(settings))
    }

    @Test
    fun colorOffAndNeutralColorAreOmitted() {
        assertNull(
            ColorCompiler.compile(
                TransformSettings(
                    enabled = true,
                    color = ColorSettings(enabled = false, contrast = 20f),
                ),
            ),
        )
        assertNull(
            ColorCompiler.compile(
                TransformSettings(
                    enabled = true,
                    color = ColorSettings(enabled = true),
                ),
            ),
        )
    }

    @Test
    fun compilesUiUnitsIntoMedia3Units() {
        val compiled = ColorCompiler.compile(
            TransformSettings(
                enabled = true,
                color = ColorSettings(
                    enabled = true,
                    brightness = 25f,
                    contrast = -30f,
                    saturation = 40f,
                    temperature = 20f,
                ),
            ),
        )

        assertEquals(
            CompiledColor(
                brightness = 0.25f,
                contrast = -0.30f,
                saturationAdjustment = 40f,
                redScale = 1.10f,
                blueScale = 0.90f,
            ),
            compiled,
        )
    }

    @Test
    fun validatesSupportedUiRanges() {
        assertTrue(
            ColorSettings(
                brightness = -50f,
                contrast = 50f,
                saturation = -100f,
                temperature = 50f,
            ).isValid(),
        )
        assertFalse(ColorSettings(brightness = 55f).isValid())
        assertFalse(ColorSettings(saturation = -110f).isValid())
    }
}
