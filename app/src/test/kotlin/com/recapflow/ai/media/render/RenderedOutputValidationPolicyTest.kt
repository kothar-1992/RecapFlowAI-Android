package com.recapflow.ai.media.render

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RenderedOutputValidationPolicyTest {
    @Test
    fun exactPortrait2kH264AacOutputPasses() {
        val result = RenderedOutputValidationPolicy.validate(
            metadata = RenderedOutputMetadata(
                width = 1440,
                height = 2560,
                durationMs = 60_080L,
                videoMimeType = "video/avc",
                audioMimeType = "audio/mp4a-latm",
            ),
            preset = RenderPreset.QHD_2K,
            expectedDurationMs = 60_000L,
            expectedAudio = true,
            expectedWidth = 1440,
            expectedHeight = 2560,
            requestedVideoBitrate = 20_000_000,
            averageVideoBitrate = 19_000_000,
        )

        assertTrue(result.isValid)
    }

    @Test
    fun rotatedPortraitTrackUsesDisplayGeometryForExact1080pValidation() {
        val result = RenderedOutputValidationPolicy.validate(
            metadata = RenderedOutputMetadata(
                width = 1920,
                height = 1080,
                rotationDegrees = 90,
                durationMs = 46_000L,
                videoMimeType = "video/avc",
                audioMimeType = "audio/mp4a-latm",
            ),
            preset = RenderPreset.FULL_HD_1080P,
            expectedDurationMs = 46_000L,
            expectedAudio = true,
            expectedWidth = 1080,
            expectedHeight = 1920,
            requestedVideoBitrate = 12_000_000,
            averageVideoBitrate = 12_000_000,
        )

        assertTrue(result.isValid)
    }

    @Test
    fun rotatedPortraitTrackUsesDisplayShortSideWhenAspectIsOriginal() {
        val result = RenderedOutputValidationPolicy.validate(
            metadata = RenderedOutputMetadata(
                width = 1280,
                height = 720,
                rotationDegrees = 270,
                durationMs = 46_000L,
                videoMimeType = "video/avc",
                audioMimeType = "audio/mp4a-latm",
            ),
            preset = RenderPreset.HD_720P,
            expectedDurationMs = 46_000L,
            expectedAudio = true,
            requestedVideoBitrate = 6_000_000,
            averageVideoBitrate = 6_000_000,
        )

        assertTrue(result.isValid)
    }

    @Test
    fun wrongShortSideIsRejectedInsteadOfSilentlyFallingBack() {
        val result = RenderedOutputValidationPolicy.validate(
            metadata = RenderedOutputMetadata(
                width = 1080,
                height = 1920,
                durationMs = 60_000L,
                videoMimeType = "video/avc",
                audioMimeType = "audio/mp4a-latm",
            ),
            preset = RenderPreset.QHD_2K,
            expectedDurationMs = 60_000L,
            expectedAudio = true,
            expectedWidth = 1440,
            expectedHeight = 2560,
            requestedVideoBitrate = 20_000_000,
            averageVideoBitrate = 20_000_000,
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("exact 1440x2560") })
    }

    @Test
    fun mutedExportRejectsUnexpectedAudioTrack() {
        val result = RenderedOutputValidationPolicy.validate(
            metadata = RenderedOutputMetadata(
                width = 1280,
                height = 720,
                durationMs = 30_000L,
                videoMimeType = "video/avc",
                audioMimeType = "audio/mp4a-latm",
            ),
            preset = RenderPreset.HD_720P,
            expectedDurationMs = 30_000L,
            expectedAudio = false,
            requestedVideoBitrate = 6_000_000,
            averageVideoBitrate = 6_000_000,
        )

        assertFalse(result.isValid)
    }

    @Test
    fun longRenderAcceptsBoundedFrameAndCodecDrift() {
        val result = RenderedOutputValidationPolicy.validate(
            metadata = validMetadata(durationMs = 293_430L),
            preset = RenderPreset.FULL_HD_1080P,
            expectedDurationMs = 293_154L,
            expectedAudio = true,
            expectedWidth = 1080,
            expectedHeight = 1920,
            requestedVideoBitrate = 12_000_000,
            averageVideoBitrate = 12_000_000,
        )

        assertTrue(result.isValid)
        assertEquals(276L, result.durationDriftMs)
        assertEquals(294L, result.allowedDurationDriftMs)
        assertTrue(result.warnings.any { it.contains("276ms") })
    }

    @Test
    fun durationOutsideBoundedToleranceStillFails() {
        val result = RenderedOutputValidationPolicy.validate(
            metadata = validMetadata(durationMs = 293_449L),
            preset = RenderPreset.FULL_HD_1080P,
            expectedDurationMs = 293_154L,
            expectedAudio = true,
            expectedWidth = 1080,
            expectedHeight = 1920,
            requestedVideoBitrate = 12_000_000,
            averageVideoBitrate = 12_000_000,
        )

        assertFalse(result.isValid)
        assertEquals(295L, result.durationDriftMs)
        assertTrue(result.errors.any { it.contains("allowed drift is 294ms") })
    }

    @Test
    fun veryLowVbrAverageRemainsVisibleWithoutPretendingCbrFailure() {
        val result = RenderedOutputValidationPolicy.validate(
            metadata = validMetadata(durationMs = 60_000L).copy(frameRate = 30.0),
            preset = RenderPreset.FULL_HD_1080P,
            expectedDurationMs = 60_000L,
            expectedAudio = true,
            expectedWidth = 1080,
            expectedHeight = 1920,
            expectedFrameRate = 30,
            requestedVideoBitrate = 8_000_000,
            averageVideoBitrate = 2_000_000,
        )

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("VBR average bitrate") })
        assertFalse(result.warnings.any { it.contains("CBR") })
    }

    @Test
    fun proportionalToleranceNeverExceedsHardCap() {
        assertEquals(
            RenderedOutputValidationPolicy.MAX_DURATION_DRIFT_MS,
            RenderedOutputValidationPolicy.allowedDurationDriftMs(3_600_000L),
        )
    }

    private fun validMetadata(durationMs: Long) = RenderedOutputMetadata(
        width = 1080,
        height = 1920,
        durationMs = durationMs,
        videoMimeType = "video/avc",
        audioMimeType = "audio/mp4a-latm",
    )
    @Test
    fun matchingSourceAwareFrameRatePasses() {
        val result = RenderedOutputValidationPolicy.validate(
            metadata = validMetadata(durationMs = 60_000L).copy(frameRate = 59.94),
            preset = RenderPreset.FULL_HD_1080P, expectedDurationMs = 60_000L, expectedAudio = true,
            expectedWidth = 1080, expectedHeight = 1920, expectedFrameRate = 60,
            requestedVideoBitrate = 12_000_000, averageVideoBitrate = 8_000_000,
        )
        assertTrue(result.isValid)
    }

    @Test
    fun materialFrameRateFallbackIsRejectedWhenMetadataIsAvailable() {
        val result = RenderedOutputValidationPolicy.validate(
            metadata = validMetadata(durationMs = 60_000L).copy(frameRate = 30.0),
            preset = RenderPreset.FULL_HD_1080P, expectedDurationMs = 60_000L, expectedAudio = true,
            expectedWidth = 1080, expectedHeight = 1920, expectedFrameRate = 60,
            requestedVideoBitrate = 12_000_000, averageVideoBitrate = 8_000_000,
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("60fps") })
    }

    @Test
    fun vbrAverageBelowTargetIsNotTreatedAsCbrFailure() {
        val result = RenderedOutputValidationPolicy.validate(
            metadata = validMetadata(durationMs = 60_000L).copy(frameRate = 30.0),
            preset = RenderPreset.FULL_HD_1080P, expectedDurationMs = 60_000L, expectedAudio = true,
            expectedWidth = 1080, expectedHeight = 1920, expectedFrameRate = 30,
            requestedVideoBitrate = 8_000_000, averageVideoBitrate = 4_000_000,
        )
        assertTrue(result.isValid)
        assertFalse(result.warnings.any { it.contains("CBR") })
    }

}
