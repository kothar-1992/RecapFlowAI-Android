package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageOverlayAnimationPolicyTest {

    @Test
    fun nonePresetIsAlwaysSettled() {
        val phase = ImageOverlayAnimationPolicy.resolve(
            ImageOverlayAnimationSettings(),
            windowLocalTimeMs = 350L,
        )

        assertEquals(1f, phase.progress)
        assertEquals(0L, phase.cycleIndex)
        assertFalse(phase.animating)
    }

    @Test
    fun nonLoopingPresetPlaysOnceThenHoldsEndState() {
        val settings = ImageOverlayAnimationSettings(
            preset = ImageOverlayAnimationPreset.FADE,
            durationMs = 1_000L,
            periodMs = 2_000L,
        )

        val halfway = ImageOverlayAnimationPolicy.resolve(settings, 500L)
        assertEquals(0.5f, halfway.progress)
        assertTrue(halfway.animating)

        val settled = ImageOverlayAnimationPolicy.resolve(settings, 1_500L)
        assertEquals(1f, settled.progress)
        assertFalse(settled.animating)
        assertEquals(0L, settled.cycleIndex)
    }

    @Test
    fun loopingPresetRepeatsDeterministicallyWithSettledInterval() {
        val settings = ImageOverlayAnimationSettings(
            preset = ImageOverlayAnimationPreset.PULSE,
            loopEnabled = true,
            durationMs = 500L,
            periodMs = 2_000L,
        )

        val first = ImageOverlayAnimationPolicy.resolve(settings, 250L)
        assertEquals(0.5f, first.progress)
        assertEquals(0L, first.cycleIndex)
        assertTrue(first.animating)

        val idle = ImageOverlayAnimationPolicy.resolve(settings, 1_000L)
        assertEquals(1f, idle.progress)
        assertEquals(0L, idle.cycleIndex)
        assertFalse(idle.animating)

        val repeated = ImageOverlayAnimationPolicy.resolve(settings, 2_250L)
        assertEquals(first.progress, repeated.progress)
        assertEquals(1L, repeated.cycleIndex)
        assertTrue(repeated.animating)
    }

    @Test
    fun projectedLateClipResumesSourceAnchoredLoopPhaseInsteadOfRestarting() {
        val settings = OverlaySettings(
            enabled = true,
            image = ImageOverlaySettings(
                enabled = true,
                asset = validAsset(),
                startMs = 1_000L,
                endMs = 12_000L,
                animation = ImageOverlayAnimationSettings(
                    preset = ImageOverlayAnimationPreset.PULSE,
                    loopEnabled = true,
                    durationMs = 1_000L,
                    periodMs = 3_000L,
                ),
            ),
        )

        val projected = OverlayCompiler.projectToRange(
            settings = settings,
            sourceRange = TrimRange(5_000L, 9_000L),
        )
        val compiled = requireNotNull(OverlayCompiler.compileImage(projected))

        assertEquals(0L, compiled.startMs)
        assertEquals(4_000L, compiled.endMs)
        assertEquals(4_000L, compiled.animation.phaseOffsetMs)

        // Source 5s is 4s after the original 1s animation anchor: cycle 1, 1s into the cycle.
        val phase = compiled.animationPhaseAt(0L)
        assertEquals(1L, phase.cycleIndex)
        assertEquals(1f, phase.progress)
        assertFalse(phase.animating)
    }

    @Test
    fun periodMustNotBeShorterThanAnimationDuration() {
        assertFalse(
            ImageOverlayAnimationPolicy.isValid(
                ImageOverlayAnimationSettings(
                    preset = ImageOverlayAnimationPreset.BOUNCE,
                    durationMs = 1_000L,
                    periodMs = 500L,
                ),
            ),
        )
    }

    private fun validAsset() = ImageOverlayAsset(
        workingFilePath = "/private/logo.png",
        displayName = "logo.png",
        mimeType = "image/png",
        pixelWidth = 512,
        pixelHeight = 256,
        fileSizeBytes = 16_000L,
    )
}
