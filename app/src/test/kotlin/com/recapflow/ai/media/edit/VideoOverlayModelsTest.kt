package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoOverlayModelsTest {

    @Test
    fun disabledOverlayIsValidWithoutAsset() {
        assertTrue(VideoOverlaySettings().isValid(sourceDurationMs = 60_000L))
    }

    @Test
    fun enabledOverlayRequiresValidAssetAndWindow() {
        val settings = VideoOverlaySettings(
            enabled = true,
            asset = asset(),
            startMs = 1_000L,
            endMs = 5_000L,
        )
        assertTrue(settings.isStructurallyValid())
        assertTrue(settings.isValid(sourceDurationMs = 10_000L))
    }

    @Test
    fun projectValidationRejectsWindowPastSourceEnd() {
        val settings = VideoOverlaySettings(
            enabled = true,
            asset = asset(),
            startMs = 8_000L,
            endMs = 12_000L,
        )
        assertTrue(settings.isStructurallyValid())
        assertFalse(settings.isValid(sourceDurationMs = 10_000L))
    }

    @Test
    fun invalidGeometryIsRejected() {
        val settings = VideoOverlaySettings(
            enabled = true,
            asset = asset(),
            widthFraction = 1.1f,
            startMs = 0L,
            endMs = 3_000L,
        )
        assertFalse(settings.isStructurallyValid())
    }

    private fun asset() = VideoOverlayAsset(
        workingFilePath = "/data/user/0/com.recapflow.ai/files/overlay.mp4",
        displayName = "overlay.mp4",
        mimeType = "video/mp4",
        pixelWidth = 720,
        pixelHeight = 1280,
        durationMs = 5_000L,
        fileSizeBytes = 500_000L,
    )
}
