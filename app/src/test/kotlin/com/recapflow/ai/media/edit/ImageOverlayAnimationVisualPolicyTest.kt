package com.recapflow.ai.media.edit

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageOverlayAnimationVisualPolicyTest {

    @Test
    fun noneIsAlwaysIdentity() {
        val state = ImageOverlayAnimationVisualPolicy.resolve(
            settings = settings(ImageOverlayAnimationPreset.NONE),
            phase = phase(progress = 0.25f),
        )

        assertEquals(ImageOverlayAnimationVisualPolicy.IDENTITY, state)
    }

    @Test
    fun settledIntervalIsIdentityForEveryAnimatedPreset() {
        ImageOverlayAnimationPreset.entries
            .filterNot { it == ImageOverlayAnimationPreset.NONE }
            .forEach { preset ->
                assertEquals(
                    ImageOverlayAnimationVisualPolicy.IDENTITY,
                    ImageOverlayAnimationVisualPolicy.resolve(
                        settings = settings(preset),
                        phase = phase(progress = 1f, animating = false),
                    ),
                )
            }
    }

    @Test
    fun fadeStartsTransparentAndSettlesOpaque() {
        val start = visual(ImageOverlayAnimationPreset.FADE, 0f)
        val middle = visual(ImageOverlayAnimationPreset.FADE, 0.5f)
        val end = visual(ImageOverlayAnimationPreset.FADE, 1f)

        assertEquals(0f, start.opacityMultiplier)
        assertTrue(middle.opacityMultiplier in 0.49f..0.51f)
        assertEquals(1f, end.opacityMultiplier)
    }

    @Test
    fun fadeScaleStartsSmallerAndEndsAtBaseGeometry() {
        val start = visual(ImageOverlayAnimationPreset.FADE_SCALE, 0f)
        val end = visual(ImageOverlayAnimationPreset.FADE_SCALE, 1f)

        assertEquals(0.82f, start.scaleMultiplier)
        assertEquals(1f, end.scaleMultiplier)
        assertEquals(0f, start.opacityMultiplier)
    }

    @Test
    fun popUsesBoundedOvershootAndReturnsToBaseScale() {
        val middle = visual(ImageOverlayAnimationPreset.POP, 0.70f)
        val end = visual(ImageOverlayAnimationPreset.POP, 1f)

        assertTrue(middle.scaleMultiplier > 1f)
        assertTrue(middle.scaleMultiplier < 1.10f)
        assertEquals(1f, end.scaleMultiplier)
    }

    @Test
    fun slideApproachesFromOnePointTwoOverlayWidthsToTheRight() {
        val start = visual(ImageOverlayAnimationPreset.SLIDE, 0f)
        val end = visual(ImageOverlayAnimationPreset.SLIDE, 1f)

        assertEquals(1.20f, start.translateXInOverlayWidths)
        assertEquals(0f, end.translateXInOverlayWidths)
    }

    @Test
    fun pulseAndFloatAreLoopSeamSafe() {
        val pulseQuarter = visual(ImageOverlayAnimationPreset.PULSE, 0.25f)
        val floatQuarter = visual(ImageOverlayAnimationPreset.FLOAT, 0.25f)
        val pulseEnd = visual(ImageOverlayAnimationPreset.PULSE, 1f)
        val floatEnd = visual(ImageOverlayAnimationPreset.FLOAT, 1f)

        assertTrue(pulseQuarter.scaleMultiplier > 1.07f)
        assertTrue(floatQuarter.translateYInOverlayHeights < -0.19f)
        assertTrue(abs(pulseEnd.scaleMultiplier - 1f) < 0.0001f)
        assertTrue(abs(floatEnd.translateYInOverlayHeights) < 0.0001f)
    }

    @Test
    fun rotateAndBounceReturnToBasePoseAtCycleEnd() {
        val rotateMiddle = visual(ImageOverlayAnimationPreset.ROTATE, 0.5f)
        val bounceMiddle = visual(ImageOverlayAnimationPreset.BOUNCE, 0.5f)
        val bounceEnd = visual(ImageOverlayAnimationPreset.BOUNCE, 1f)

        assertEquals(180f, rotateMiddle.rotationDegrees)
        assertTrue(bounceMiddle.translateYInOverlayHeights < -0.34f)
        assertTrue(abs(bounceEnd.translateYInOverlayHeights) < 0.0001f)
    }

    private fun visual(preset: ImageOverlayAnimationPreset, progress: Float) =
        ImageOverlayAnimationVisualPolicy.resolve(
            settings = settings(preset),
            phase = phase(progress),
        )

    private fun settings(preset: ImageOverlayAnimationPreset) = ImageOverlayAnimationSettings(
        preset = preset,
        durationMs = 1_000L,
        periodMs = 2_000L,
    )

    private fun phase(
        progress: Float,
        animating: Boolean = true,
    ) = ImageOverlayAnimationPhase(
        progress = progress,
        cycleIndex = 0L,
        animating = animating,
    )
}
