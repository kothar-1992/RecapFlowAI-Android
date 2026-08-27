package com.recapflow.ai.media.render

import com.recapflow.ai.media.MediaInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RenderQualityPolicyTest {

    @Test
    fun lowBitrateSourceGetsTwentyFiveMegabit720pFloor() {
        val request = RenderQualityPolicy.forSource(
            mediaInfo = mediaInfo(width = 576, height = 1024, bitrate = 1_600_000L),
            preset = RenderPreset.HD_720P,
        )

        assertEquals(25_000_000, request.requestedVideoBitrate)
        assertEquals(576, request.sourceShortSidePixels)
        assertTrue(request.isUpscaling)
    }

    @Test
    fun lowBitrateSourceGetsThirtyMegabit1080pFloor() {
        val request = RenderQualityPolicy.forSource(
            mediaInfo = mediaInfo(width = 1080, height = 1920, bitrate = 5_000_000L),
            preset = RenderPreset.FULL_HD_1080P,
        )

        assertEquals(30_000_000, request.requestedVideoBitrate)
        assertFalse(request.isUpscaling)
    }

    @Test
    fun strongSourceBitrateIsPreservedWithinPresetCap() {
        assertEquals(
            25_000_000,
            RenderQualityPolicy.forSource(
                mediaInfo = mediaInfo(width = 720, height = 1280, bitrate = 10_000_000L),
                preset = RenderPreset.HD_720P,
            ).requestedVideoBitrate,
        )
        assertEquals(
            45_000_000,
            RenderQualityPolicy.forSource(
                mediaInfo = mediaInfo(width = 2160, height = 3840, bitrate = 80_000_000L),
                preset = RenderPreset.FULL_HD_1080P,
            ).requestedVideoBitrate,
        )
    }

    @Test
    fun twoKUsesExact1440ShortSideAndFortyFiveMegabitFloor() {
        val request = RenderQualityPolicy.forSource(
            mediaInfo = mediaInfo(width = 1080, height = 1920, bitrate = 8_000_000L),
            preset = RenderPreset.QHD_2K,
        )

        assertEquals(45_000_000, request.requestedVideoBitrate)
        assertTrue(request.isUpscaling)
        assertEquals(1440, RenderPreset.QHD_2K.shortSidePixels)
    }

    @Test
    fun previousRecapFlowOutputIsIdentifiedForGenerationLossWarning() {
        val request = RenderQualityPolicy.forSource(
            mediaInfo = mediaInfo(
                width = 1280,
                height = 720,
                bitrate = 7_000_000L,
                displayName = "RecapFlow_720p_20260827_120000.mp4",
            ),
            preset = RenderPreset.HD_720P,
        )

        assertTrue(request.isPreviousRecapFlowExport)
    }

    private fun mediaInfo(
        width: Int,
        height: Int,
        bitrate: Long,
        displayName: String = "source.mp4",
    ) = MediaInfo(
        sourceUri = "content://test/source",
        workingFilePath = "/tmp/source.mp4",
        displayName = displayName,
        fileSizeBytes = 1L,
        durationMs = 60_000L,
        width = width,
        height = height,
        rotationDegrees = 0,
        frameRate = 30.0,
        videoCodec = "h264",
        audioCodec = "aac",
        audioSampleRate = 48_000,
        audioChannels = 2,
        bitrate = bitrate,
        containerFormat = "mp4",
    )
}
