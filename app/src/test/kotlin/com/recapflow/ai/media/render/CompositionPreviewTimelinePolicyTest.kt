package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.BlurRectangle
import com.recapflow.ai.media.edit.ImageOverlaySettings
import com.recapflow.ai.media.edit.OverlaySettings
import com.recapflow.ai.media.edit.SourceSubtitleBlurSettings
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TrimRange
import kotlin.test.Test
import kotlin.test.assertEquals

class CompositionPreviewTimelinePolicyTest {

    @Test
    fun mapsAdaptiveSourcePositionToConcatenatedOutputAtNormalSpeed() {
        val ranges = listOf(TrimRange(10_000L, 20_000L), TrimRange(40_000L, 50_000L))
        assertEquals(
            15_000L,
            CompositionPreviewTimelinePolicy.sourceToOutputMs(
                sourcePositionMs = 45_000L,
                ranges = ranges,
                settings = TransformSettings(),
            ),
        )
        assertEquals(
            45_000L,
            CompositionPreviewTimelinePolicy.outputToSourceMs(
                outputPositionMs = 15_000L,
                ranges = ranges,
                settings = TransformSettings(),
            ),
        )
    }

    @Test
    fun mapsAdaptiveTimelineWithTwoTimesSpeed() {
        val settings = TransformSettings(enabled = true, speedEnabled = true, speed = 2f)
        val ranges = listOf(TrimRange(10_000L, 20_000L), TrimRange(40_000L, 50_000L))
        assertEquals(
            7_500L,
            CompositionPreviewTimelinePolicy.sourceToOutputMs(45_000L, ranges, settings),
        )
        assertEquals(
            45_000L,
            CompositionPreviewTimelinePolicy.outputToSourceMs(7_500L, ranges, settings),
        )
    }

    @Test
    fun sourceInsideCutGapClampsToNearestSelectedBoundary() {
        val ranges = listOf(TrimRange(0L, 10_000L), TrimRange(30_000L, 40_000L))
        assertEquals(
            10_000L,
            CompositionPreviewTimelinePolicy.nearestSelectedSourcePosition(14_000L, ranges),
        )
        assertEquals(
            30_000L,
            CompositionPreviewTimelinePolicy.nearestSelectedSourcePosition(26_000L, ranges),
        )
    }

    @Test
    fun timedOverlaysAreProjectedIntoPostSpeedPresentationTime() {
        val overlays = OverlaySettings(
            enabled = true,
            sourceSubtitleBlur = SourceSubtitleBlurSettings(
                enabled = true,
                rectangle = BlurRectangle(),
                startMs = 2_000L,
                endMs = 8_000L,
            ),
            image = ImageOverlaySettings(
                enabled = true,
                startMs = 4_000L,
                endMs = 10_000L,
            ),
        )
        val settings = TransformSettings(enabled = true, speedEnabled = true, speed = 2f)
        val result = CompositionPreviewTimelinePolicy.projectOverlayWindowsToPresentationTime(
            overlays,
            settings,
        )
        assertEquals(1_000L, result.sourceSubtitleBlur.startMs)
        assertEquals(4_000L, result.sourceSubtitleBlur.endMs)
        assertEquals(2_000L, result.image.startMs)
        assertEquals(5_000L, result.image.endMs)
    }
}
