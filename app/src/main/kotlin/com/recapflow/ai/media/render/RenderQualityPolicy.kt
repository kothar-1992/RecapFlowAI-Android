package com.recapflow.ai.media.render

import com.recapflow.ai.media.MediaInfo

/** Social-upload-oriented H.264 request; source bitrate is diagnostic rather than an output floor. */
object RenderQualityPolicy {
    fun forSource(mediaInfo: MediaInfo, preset: RenderPreset): RenderQualityRequest {
        val sourceShortSide = minOf(mediaInfo.width, mediaInfo.height).coerceAtLeast(1)
        val targetFrameRate = ExportFrameRatePolicy.forSource(mediaInfo.frameRate)
        return RenderQualityRequest(
            requestedVideoBitrate = preset.videoBitrateFor(targetFrameRate),
            targetFrameRate = targetFrameRate,
            sourceShortSidePixels = sourceShortSide,
            isUpscaling = preset.shortSidePixels > sourceShortSide,
            isPreviousRecapFlowExport = mediaInfo.displayName.startsWith("RecapFlow_", ignoreCase = true),
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
