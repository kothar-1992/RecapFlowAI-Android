package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.CompiledImageOverlay
import com.recapflow.ai.media.edit.ImageOverlayAsset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RealtimeImageOverlayStateTest {

    @Test
    fun latestMatchingAssetSnapshotReplacesStaleGeometry() {
        val state = RealtimeImageOverlayState()
        state.update(compiled(centerX = 0.86f, widthFraction = 0.22f))
        state.update(compiled(centerX = 0.14f, widthFraction = 0.35f))

        val snapshot = state.snapshotFor(WORKING_PATH)

        assertEquals(0.14f, snapshot?.centerX)
        assertEquals(0.35f, snapshot?.widthFraction)
    }

    @Test
    fun disabledOrReplacedAssetCannotLeakThroughOldShader() {
        val state = RealtimeImageOverlayState()
        state.update(compiled(centerX = 0.5f, widthFraction = 0.4f))

        assertNull(state.snapshotFor("/private/replacement.png"))

        state.update(null)
        assertNull(state.snapshotFor(WORKING_PATH))
    }

    private fun compiled(centerX: Float, widthFraction: Float) = CompiledImageOverlay(
        asset = ImageOverlayAsset(
            workingFilePath = WORKING_PATH,
            displayName = "logo.png",
            mimeType = "image/png",
            pixelWidth = 512,
            pixelHeight = 256,
            fileSizeBytes = 16_000L,
        ),
        centerX = centerX,
        centerY = 0.18f,
        widthFraction = widthFraction,
        opacity = 0.7f,
        startMs = 1_000L,
        endMs = 8_000L,
    )

    private companion object {
        const val WORKING_PATH = "/private/logo.png"
    }
}
