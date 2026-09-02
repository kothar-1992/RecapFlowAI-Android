package com.recapflow.ai.media.render

import androidx.media3.common.OverlaySettings
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.StaticOverlaySettings

/**
 * Experimental compositor settings for the Phase 6H.1 Crossfade runtime spike.
 *
 * Lane 0 is kept opaque as the background. Lane 1 is presented as a full-frame overlay whose
 * alpha follows the same semantic easing used by [Media3CrossfadeTopology]. Alternating clip lanes
 * make this sufficient for both A->B and B->A transitions: when lane 1 is incoming it fades in over
 * lane 0; when lane 1 is outgoing it fades out and reveals lane 0 underneath.
 */
@UnstableApi
class Media3CrossfadeVideoCompositorSettings(
    private val topology: Media3CrossfadeTopology,
) : VideoCompositorSettings {

    override fun getOutputSize(inputSizes: List<Size>): Size {
        require(inputSizes.isNotEmpty()) { "Crossfade compositor requires at least one video input" }
        val output = inputSizes.first()
        require(inputSizes.all { it == output }) {
            "Crossfade compositor requires matching full-frame lane geometry: $inputSizes"
        }
        return output
    }

    /**
     * Pure semantic alpha lookup kept separate from Media3's Android-backed OverlaySettings object.
     * Local JVM tests exercise this method; assemble/device tests exercise getOverlaySettings().
     */
    internal fun alphaForInput(inputId: Int, presentationTimeUs: Long): Float = when (inputId) {
        0 -> 1f
        1 -> topology.overlayLaneAlpha(presentationTimeUs)
        else -> error("Crossfade compositor supports exactly two video lanes; inputId=$inputId")
    }

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings =
        StaticOverlaySettings.Builder()
            .setAlphaScale(alphaForInput(inputId, presentationTimeUs))
            .build()
}
