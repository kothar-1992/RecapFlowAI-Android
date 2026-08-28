package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TrimRange
import com.recapflow.ai.media.edit.VideoOverlayAsset
import com.recapflow.ai.media.edit.VideoOverlaySettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoOverlayTimelinePolicyTest {

    @Test
    fun projectsOneAbsoluteWindowAcrossAdaptiveCuts() {
        val segments = VideoOverlayTimelinePolicy.project(
            settings = overlay(startMs = 2_000L, endMs = 12_000L, assetDurationMs = 20_000L),
            selectedRanges = listOf(
                TrimRange(0L, 5_000L),
                TrimRange(10_000L, 15_000L),
            ),
            transform = TransformSettings(),
        )

        assertEquals(2, segments.size)
        assertEquals(2_000L, segments[0].presentationStartMs)
        assertEquals(5_000L, segments[0].presentationEndMs)
        assertEquals(5_000L, segments[1].presentationStartMs)
        assertEquals(7_000L, segments[1].presentationEndMs)
        assertEquals(8_000L, segments[1].overlayMediaStartMs)
        assertEquals(10_000L, segments[1].overlayMediaEndMs)
    }

    @Test
    fun appliesMainSpeedOnlyToPresentationTime() {
        val segments = VideoOverlayTimelinePolicy.project(
            settings = overlay(startMs = 2_000L, endMs = 8_000L, assetDurationMs = 10_000L),
            selectedRanges = listOf(TrimRange(0L, 10_000L)),
            transform = TransformSettings(enabled = true, speedEnabled = true, speed = 2f),
        )

        assertEquals(1, segments.size)
        assertEquals(1_000L, segments.single().presentationStartMs)
        assertEquals(4_000L, segments.single().presentationEndMs)
        assertEquals(0L, segments.single().overlayMediaStartMs)
        assertEquals(6_000L, segments.single().overlayMediaEndMs)
    }

    @Test
    fun shortOverlayEndsNaturallyInsteadOfLooping() {
        val segments = VideoOverlayTimelinePolicy.project(
            settings = overlay(startMs = 4_000L, endMs = 14_000L, assetDurationMs = 3_000L),
            selectedRanges = listOf(TrimRange(0L, 20_000L)),
            transform = TransformSettings(),
        )

        assertEquals(1, segments.size)
        assertEquals(4_000L, segments.single().sourceStartMs)
        assertEquals(7_000L, segments.single().sourceEndMs)
        assertEquals(3_000L, segments.single().overlayMediaEndMs)
    }

    @Test
    fun windowOutsideSelectedRangesProducesNoOverlaySegments() {
        val segments = VideoOverlayTimelinePolicy.project(
            settings = overlay(startMs = 20_000L, endMs = 25_000L, assetDurationMs = 10_000L),
            selectedRanges = listOf(TrimRange(0L, 10_000L)),
            transform = TransformSettings(),
        )

        assertTrue(segments.isEmpty())
    }

    private fun overlay(
        startMs: Long,
        endMs: Long,
        assetDurationMs: Long,
    ): VideoOverlaySettings = VideoOverlaySettings(
        enabled = true,
        asset = VideoOverlayAsset(
            workingFilePath = "/data/user/0/com.recapflow.ai/files/overlay.mp4",
            displayName = "overlay.mp4",
            mimeType = "video/mp4",
            pixelWidth = 1080,
            pixelHeight = 1920,
            durationMs = assetDurationMs,
            fileSizeBytes = 1_000_000L,
        ),
        startMs = startMs,
        endMs = endMs,
    )
}
