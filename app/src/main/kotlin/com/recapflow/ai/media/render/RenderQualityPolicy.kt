package com.recapflow.ai.media.render

import com.recapflow.ai.media.MediaInfo

/**
 * Chooses deterministic social-upload H.264 settings for the reviewed export.
 *
 * The probed source frame rate controls both the output frame-rate class and the bitrate target.
 * Source container bitrate is diagnostic only; it no longer forces oversized 30-60 Mbps outputs.
 */
object RenderQualityPolicy {

    fun forSource(
        mediaInfo: MediaInfo,
        preset: RenderPreset,
    ): RenderQualityRequest {
        val sourceShortSide = minOf(mediaInfo.width, mediaInfo.height).coerceAtLeast(1)
        val targetFrameRate = ExportFrameRatePolicy.forSource(mediaInfo.frameRate)
        return RenderQualityRequest(
            requestedVideoBitrate = preset.videoBitrateFor(targetFrameRate),
            targetFrameRate = targetFrameRate,
            sourceShortSidePixels = sourceShortSide,
            isUpscaling = preset.shortSidePixels > sourceShortSide,
            isPreviousRecapFlowExport = mediaInfo.displayName.startsWith(
                prefix = "RecapFlow_",
                ignoreCase = true,
            ),
        )
    }
}

data class RenderQualityRequest(
    val requestedVideoBitrate: Int,
    val targetFrameRate: Int,
    val sourceShortSidePixels: Int,
    val isUpscaling: Boolean,
    val isPreviousRecapFlowExport: Boolean,
)
