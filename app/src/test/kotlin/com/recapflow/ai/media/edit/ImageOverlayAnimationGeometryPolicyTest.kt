package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageOverlayAnimationGeometryPolicyTest {

    @Test
    fun identityKeepsResolvedCenterAndScale() {
        val bounds = ImageOverlayFrameBounds(0.70f, 0.10f, 0.90f, 0.20f)
        val result = ImageOverlayAnimationGeometryPolicy.resolve(
            bounds,
            ImageOverlayAnimationVisualPolicy.IDENTITY,
        )

        assertEquals(0.80f, result.centerX)
        assertEquals(0.15f, result.centerY)
        assertEquals(1f, result.scaleMultiplier)
        assertEquals(0f, result.rotationRadians)
    }

    @Test
    fun slideNearRightEdgeIsClampedInsideFrame() {
        val bounds = ImageOverlayFrameBounds(0.76f, 0.08f, 0.96f, 0.18f)
        val result = ImageOverlayAnimationGeometryPolicy.resolve(
            bounds,
            ImageOverlayAnimationVisualState(translateXInOverlayWidths = 1.2f),
        )

        assertTrue(result.centerX + result.rotatedHalfWidth <= 1.0001f)
        assertTrue(result.centerX - result.rotatedHalfWidth >= -0.0001f)
    }

    @Test
    fun rotatedCornerLogoStaysFullyInsideFrame() {
        val bounds = ImageOverlayFrameBounds(0.76f, 0.04f, 0.96f, 0.14f)
        val result = ImageOverlayAnimationGeometryPolicy.resolve(
            bounds,
            ImageOverlayAnimationVisualState(rotationDegrees = 45f),
        )

        assertTrue(result.centerX + result.rotatedHalfWidth <= 1.0001f)
        assertTrue(result.centerX - result.rotatedHalfWidth >= -0.0001f)
        assertTrue(result.centerY + result.rotatedHalfHeight <= 1.0001f)
        assertTrue(result.centerY - result.rotatedHalfHeight >= -0.0001f)
    }

    @Test
    fun oversizedPopIsReducedOnlyAsMuchAsNeededForFrameSafety() {
        val bounds = ImageOverlayFrameBounds(0.02f, 0.02f, 0.98f, 0.98f)
        val result = ImageOverlayAnimationGeometryPolicy.resolve(
            bounds,
            ImageOverlayAnimationVisualState(scaleMultiplier = 1.08f),
        )

        assertTrue(result.scaleMultiplier < 1.08f)
        assertTrue(result.centerX + result.rotatedHalfWidth <= 1.0001f)
        assertTrue(result.centerY + result.rotatedHalfHeight <= 1.0001f)
    }
}
