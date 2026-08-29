package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.BlurRectangle
import com.recapflow.ai.media.edit.ClipTransitionEasing
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
    fun crossfadeSourceMappingUsesOverlappedPresentationStarts() {
        val topology = crossfadeTopology()

        assertEquals(2_000L, CompositionPreviewTimelinePolicy.sourceToOutputMs(2_000L, topology))
        assertEquals(3_700L, CompositionPreviewTimelinePolicy.sourceToOutputMs(5_000L, topology))
        assertEquals(3_850L, CompositionPreviewTimelinePolicy.sourceToOutputMs(5_150L, topology))
        assertEquals(7_700L, CompositionPreviewTimelinePolicy.sourceToOutputMs(9_000L, topology))
    }

    @Test
    fun crossfadeOutputMappingUsesVisuallyDominantClipInOverlap() {
        val topology = crossfadeTopology()

        // Linear lane-1 alpha is ~0.33 here, so outgoing lane 0 still owns the source cursor.
        assertEquals(3_800L, CompositionPreviewTimelinePolicy.outputToSourceMs(3_800L, topology))
        // Linear lane-1 alpha is ~0.67 here, so incoming lane 1 owns the source cursor.
        assertEquals(5_200L, CompositionPreviewTimelinePolicy.outputToSourceMs(3_900L, topology))
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

    private fun crossfadeTopology() = Media3CrossfadeTopology(
        slots = listOf(
            Media3CrossfadeClipSlot(
                rangeIndex = 0,
                lane = 0,
                sourceRange = TrimRange(0L, 4_000L),
                presentationStartUs = 0L,
                presentationDurationUs = 4_000_000L,
                fadeIn = null,
                fadeOut = Media3CrossfadeEnvelope(
                    startUs = 3_700_000L,
                    durationUs = 300_000L,
                    easing = ClipTransitionEasing.LINEAR,
                ),
            ),
            Media3CrossfadeClipSlot(
                rangeIndex = 1,
                lane = 1,
                sourceRange = TrimRange(5_000L, 9_000L),
                presentationStartUs = 3_700_000L,
                presentationDurationUs = 4_000_000L,
                fadeIn = Media3CrossfadeEnvelope(
                    startUs = 3_700_000L,
                    durationUs = 300_000L,
                    easing = ClipTransitionEasing.LINEAR,
                ),
                fadeOut = null,
            ),
        ),
        freezeDurationUs = 0L,
        totalDurationUs = 7_700_000L,
    )
}
