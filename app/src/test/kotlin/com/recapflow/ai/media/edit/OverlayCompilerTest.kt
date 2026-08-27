package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OverlayCompilerTest {

    @Test
    fun masterOffOmitsRememberedSourceBlur() {
        assertNull(
            OverlayCompiler.compile(
                OverlaySettings(
                    enabled = false,
                    sourceSubtitleBlur = validBlur(enabled = true),
                ),
            ),
        )
    }

    @Test
    fun sourceBlurOffIsOmitted() {
        assertNull(
            OverlayCompiler.compile(
                OverlaySettings(
                    enabled = true,
                    sourceSubtitleBlur = validBlur(enabled = false),
                ),
            ),
        )
    }

    @Test
    fun enabledSourceBlurRetainsRectangleStrengthAndAbsoluteSourceTime() {
        val compiled = requireNotNull(
            OverlayCompiler.compile(
                OverlaySettings(
                    enabled = true,
                    sourceSubtitleBlur = validBlur(enabled = true),
                ),
            ),
        )

        assertEquals(0.12f, compiled.rectangle.left)
        assertEquals(18f, compiled.strength)
        assertTrue(compiled.isActiveAt(2_000L))
        assertFalse(compiled.isActiveAt(5_000L))
        assertTrue(compiled.intersects(TrimRange(4_500L, 6_000L)))
        assertFalse(compiled.intersects(TrimRange(5_000L, 6_000L)))
    }

    @Test
    fun masterOffOmitsRememberedImageOverlay() {
        assertNull(
            OverlayCompiler.compileImage(
                OverlaySettings(enabled = false, image = validImage(enabled = true)),
            ),
        )
    }

    @Test
    fun imageItemOffIsOmitted() {
        assertNull(
            OverlayCompiler.compileImage(
                OverlaySettings(enabled = true, image = validImage(enabled = false)),
            ),
        )
    }

    @Test
    fun enabledImageRetainsAssetGeometryOpacityAndAbsoluteSourceTime() {
        val compiled = requireNotNull(
            OverlayCompiler.compileImage(
                OverlaySettings(enabled = true, image = validImage(enabled = true)),
            ),
        )

        assertEquals("logo.png", compiled.asset.displayName)
        assertEquals(0.82f, compiled.centerX)
        assertEquals(0.24f, compiled.widthFraction)
        assertEquals(0.7f, compiled.opacity)
        assertTrue(compiled.isActiveAt(1_000L))
        assertFalse(compiled.isActiveAt(8_000L))
        assertTrue(compiled.intersects(TrimRange(7_500L, 9_000L)))
        assertFalse(compiled.intersects(TrimRange(8_000L, 9_000L)))
    }


    @Test
    fun projectToRangeKeepsFullBlurActiveAcrossLateClippedItem() {
        val settings = OverlaySettings(
            enabled = true,
            sourceSubtitleBlur = SourceSubtitleBlurSettings(
                enabled = true,
                rectangle = BlurRectangle(),
                strength = 18f,
                startMs = 0L,
                endMs = 300_000L,
            ),
        )

        val projected = OverlayCompiler.projectToRange(
            settings,
            TrimRange(150_000L, 300_000L),
        )
        val blur = requireNotNull(OverlayCompiler.compile(projected))

        assertEquals(0L, blur.startMs)
        assertEquals(150_000L, blur.endMs)
        assertTrue(blur.isActiveAt(149_999L))
    }

    @Test
    fun projectToRangeConvertsPartialAbsoluteWindowToLocalTime() {
        val settings = OverlaySettings(
            enabled = true,
            sourceSubtitleBlur = SourceSubtitleBlurSettings(
                enabled = true,
                rectangle = BlurRectangle(),
                strength = 18f,
                startMs = 170_000L,
                endMs = 190_000L,
            ),
        )

        val projected = OverlayCompiler.projectToRange(
            settings,
            TrimRange(150_000L, 210_000L),
        )
        val blur = requireNotNull(OverlayCompiler.compile(projected))

        assertEquals(20_000L, blur.startMs)
        assertEquals(40_000L, blur.endMs)
        assertFalse(blur.isActiveAt(19_999L))
        assertTrue(blur.isActiveAt(20_000L))
        assertFalse(blur.isActiveAt(40_000L))
    }

    @Test
    fun projectToRangeDisablesNonIntersectingBlurAndImage() {
        val settings = OverlaySettings(
            enabled = true,
            sourceSubtitleBlur = validBlur(enabled = true),
            image = validImage(enabled = true),
        )

        val projected = OverlayCompiler.projectToRange(settings, TrimRange(9_000L, 10_000L))

        assertNull(OverlayCompiler.compile(projected))
        assertNull(OverlayCompiler.compileImage(projected))
    }

    @Test
    fun rangeHelpersRetainEitherBlurOrImageOperation() {
        val settings = OverlaySettings(enabled = true, image = validImage(enabled = true))

        assertTrue(OverlayCompiler.hasOperationIntersecting(settings, TrimRange(2_000L, 3_000L)))
        assertTrue(OverlayCompiler.hasOperationActiveAt(settings, 2_000L))
        assertFalse(OverlayCompiler.hasOperationActiveAt(settings, 9_000L))
    }

    private fun validBlur(enabled: Boolean) = SourceSubtitleBlurSettings(
        enabled = enabled,
        rectangle = BlurRectangle(left = 0.12f, top = 0.70f, right = 0.88f, bottom = 0.92f),
        strength = 18f,
        startMs = 1_000L,
        endMs = 5_000L,
    )

    private fun validImage(enabled: Boolean) = ImageOverlaySettings(
        enabled = enabled,
        asset = ImageOverlayAsset(
            workingFilePath = "/private/logo.png",
            displayName = "logo.png",
            mimeType = "image/png",
            pixelWidth = 512,
            pixelHeight = 256,
            fileSizeBytes = 16_000L,
        ),
        centerX = 0.82f,
        centerY = 0.18f,
        widthFraction = 0.24f,
        opacity = 0.7f,
        startMs = 1_000L,
        endMs = 8_000L,
    )
}
