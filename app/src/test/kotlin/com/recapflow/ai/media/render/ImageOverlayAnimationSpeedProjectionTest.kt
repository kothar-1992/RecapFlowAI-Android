package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.ImageOverlayAnimationPreset
import com.recapflow.ai.media.edit.ImageOverlayAnimationSettings
import com.recapflow.ai.media.edit.ImageOverlaySettings
import com.recapflow.ai.media.edit.OverlaySettings
import com.recapflow.ai.media.edit.TransformSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageOverlayAnimationSpeedProjectionTest {

    @Test
    fun twoTimesSpeedProjectsWindowDurationPeriodAndPhaseOffsetTogether() {
        val overlays = OverlaySettings(
            enabled = true,
            image = ImageOverlaySettings(
                enabled = true,
                startMs = 2_000L,
                endMs = 10_000L,
                animation = ImageOverlayAnimationSettings(
                    preset = ImageOverlayAnimationPreset.PULSE,
                    loopEnabled = true,
                    durationMs = 800L,
                    periodMs = 2_400L,
                    phaseOffsetMs = 3_000L,
                ),
            ),
        )
        val speed = TransformSettings(enabled = true, speedEnabled = true, speed = 2f)

        val projected = CompositionPreviewTimelinePolicy.projectOverlayWindowsToPresentationTime(
            overlays = overlays,
            settings = speed,
        ).image

        assertEquals(1_000L, projected.startMs)
        assertEquals(5_000L, projected.endMs)
        assertEquals(400L, projected.animation.durationMs)
        assertEquals(1_200L, projected.animation.periodMs)
        assertEquals(1_500L, projected.animation.phaseOffsetMs)
    }

    @Test
    fun neutralSpeedLeavesAnimationTimingUnchanged() {
        val overlays = OverlaySettings(
            enabled = true,
            image = ImageOverlaySettings(
                enabled = true,
                startMs = 2_000L,
                endMs = 10_000L,
                animation = ImageOverlayAnimationSettings(
                    preset = ImageOverlayAnimationPreset.FADE,
                    durationMs = 700L,
                    periodMs = 2_000L,
                ),
            ),
        )

        val projected = CompositionPreviewTimelinePolicy.projectOverlayWindowsToPresentationTime(
            overlays = overlays,
            settings = TransformSettings(),
        )

        assertEquals(overlays, projected)
    }
}
