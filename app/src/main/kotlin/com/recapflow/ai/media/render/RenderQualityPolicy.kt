package com.recapflow.ai.media.render

import com.recapflow.ai.media.MediaInfo

/**
 * Chooses a deterministic H.264 bitrate request for the reviewed export.
 *
 * MediaInfo.bitrate is the probed container bitrate, so it is used only as a
 * source-quality floor and is always constrained by the selected preset. The
 * encoder can still apply a device fallback; ExportResult reports the actual
 * average bitrate so that difference remains visible to the user.
 */
object RenderQualityPolicy {

    fun forSource(
        mediaInfo: MediaInfo,
        preset: RenderPreset,
    ): RenderQualityRequest {
        val sourceShortSide = minOf(mediaInfo.width, mediaInfo.height).coerceAtLeast(1)
        val boundedSourceBitrate = mediaInfo.bitrate
            .coerceAtLeast(0L)
            .coerceAtMost(preset.maximumVideoBitrate.toLong())
            .toInt()
        return RenderQualityRequest(
            requestedVideoBitrate = maxOf(
                preset.minimumVideoBitrate,
                boundedSourceBitrate,
            ),
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
    val sourceShortSidePixels: Int,
    val isUpscaling: Boolean,
    val isPreviousRecapFlowExport: Boolean,
)
