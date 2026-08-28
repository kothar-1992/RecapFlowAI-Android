package com.recapflow.ai.media.edit

/**
 * App-private video prepared from Android's system document picker for use as one timed overlay.
 * Phase 6G.1 deliberately excludes overlay audio from the edit contract; the overlay is muted.
 */
data class VideoOverlayAsset(
    val workingFilePath: String,
    val displayName: String,
    val mimeType: String,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val durationMs: Long,
    val fileSizeBytes: Long,
) {
    fun isValid(): Boolean =
        workingFilePath.isNotBlank() &&
            pixelWidth > 0 &&
            pixelHeight > 0 &&
            durationMs > 0L &&
            fileSizeBytes >= 0L
}

/**
 * One picture-in-picture video overlay measured against the final video frame.
 *
 * [startMs] and [endMs] are absolute source-timeline positions. This is intentional: Trim and
 * reviewed Adaptive Cuts can remove source intervals without changing the user's remembered
 * overlay placement. The render compiler projects this absolute window into presentation time.
 *
 * Audio is always muted in Phase 6G.1. A future explicit audio policy must be introduced as a
 * separate gate rather than silently mixing overlay audio into the verified main audio path.
 */
data class VideoOverlaySettings(
    val enabled: Boolean = false,
    val asset: VideoOverlayAsset? = null,
    val centerX: Float = DEFAULT_CENTER_X,
    val centerY: Float = DEFAULT_CENTER_Y,
    val widthFraction: Float = DEFAULT_WIDTH_FRACTION,
    val opacity: Float = DEFAULT_OPACITY,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
) {
    val configuredDurationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)

    fun isValid(sourceDurationMs: Long): Boolean =
        !enabled || (
            asset?.isValid() == true &&
                centerX in 0f..1f &&
                centerY in 0f..1f &&
                widthFraction in MIN_WIDTH_FRACTION..MAX_WIDTH_FRACTION &&
                opacity in 0f..1f &&
                startMs >= 0L &&
                endMs > startMs &&
                endMs <= sourceDurationMs.coerceAtLeast(0L)
            )

    companion object {
        const val DEFAULT_CENTER_X = 0.82f
        const val DEFAULT_CENTER_Y = 0.18f
        const val DEFAULT_WIDTH_FRACTION = 0.28f
        const val DEFAULT_OPACITY = 1f
        const val MIN_WIDTH_FRACTION = 0.05f
        const val MAX_WIDTH_FRACTION = 1f
    }
}
