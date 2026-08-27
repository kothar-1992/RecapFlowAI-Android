package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageOverlayLayoutPolicyTest {

    @Test
    fun landscapeLogoKeepsPixelAspectOnPortraitOutput() {
        val bounds = ImageOverlayLayoutPolicy.resolve(
            imageWidth = 400,
            imageHeight = 200,
            frameWidth = 1_080,
            frameHeight = 1_920,
            centerX = 0.5f,
            centerY = 0.5f,
            requestedWidthFraction = 0.2f,
        )

        assertEquals(0.2f, bounds.width, 0.0001f)
        assertEquals(0.05625f, bounds.height, 0.0001f)
        assertEquals(0.4f, bounds.left, 0.0001f)
    }

    @Test
    fun sameLogoKeepsPixelAspectOnLandscapeOutput() {
        val bounds = ImageOverlayLayoutPolicy.resolve(
            imageWidth = 400,
            imageHeight = 200,
            frameWidth = 1_920,
            frameHeight = 1_080,
            centerX = 0.5f,
            centerY = 0.5f,
            requestedWidthFraction = 0.2f,
        )

        assertEquals(0.2f, bounds.width, 0.0001f)
        assertEquals(0.17777778f, bounds.height, 0.0001f)
    }

    @Test
    fun edgePresetClampsTheWholeLogoInsideTheFrame() {
        val bounds = ImageOverlayLayoutPolicy.resolve(
            imageWidth = 200,
            imageHeight = 200,
            frameWidth = 1_920,
            frameHeight = 1_080,
            centerX = 1f,
            centerY = 1f,
            requestedWidthFraction = 0.8f,
        )

        assertTrue(bounds.left >= 0f)
        assertTrue(bounds.top >= 0f)
        assertTrue(bounds.right <= 1f)
        assertTrue(bounds.bottom <= 1f)
        assertEquals(0.96f, bounds.height, 0.0001f)
        assertEquals(1f, bounds.bottom, 0.0001f)
    }

    @Test
    fun unusuallyTallLogoStillKeepsItsPixelAspect() {
        val bounds = ImageOverlayLayoutPolicy.resolve(
            imageWidth = 1,
            imageHeight = 8_192,
            frameWidth = 1_920,
            frameHeight = 1_080,
            centerX = 0.5f,
            centerY = 0.5f,
            requestedWidthFraction = 0.08f,
        )

        val normalizedPixelAspect = bounds.width * 1_920f / (bounds.height * 1_080f)
        assertEquals(1f / 8_192f, normalizedPixelAspect, 0.000001f)
        assertEquals(0.96f, bounds.height, 0.0001f)
    }
}
