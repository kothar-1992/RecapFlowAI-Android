package com.recapflow.ai.media.render

data class RenderedOutputMetadata(
    /** Encoded/coded video width reported by MediaExtractor. */
    val width: Int,
    /** Encoded/coded video height reported by MediaExtractor. */
    val height: Int,
    /** Track rotation metadata normalized to 0/90/180/270 degrees. */
    val rotationDegrees: Int = 0,
    val frameRate: Double = 0.0,
    val durationMs: Long,
    val videoMimeType: String?,
    val audioMimeType: String?,
) {
    /** Display geometry after applying the MP4 track rotation metadata. */
    val displayWidth: Int
        get() = if (rotationDegrees == 90 || rotationDegrees == 270) height else width

    /** Display geometry after applying the MP4 track rotation metadata. */
    val displayHeight: Int
        get() = if (rotationDegrees == 90 || rotationDegrees == 270) width else height
}

data class RenderedOutputValidation(
    val errors: List<String>,
    val warnings: List<String>,
    val durationDriftMs: Long,
    val allowedDurationDriftMs: Long,
) {
    val isValid: Boolean
        get() = errors.isEmpty()
}

/** Pure policy used after Media3 finishes, before a private output may be published. */
object RenderedOutputValidationPolicy {
    fun validate(
        metadata: RenderedOutputMetadata,
        preset: RenderPreset,
        expectedDurationMs: Long,
        expectedAudio: Boolean,
        expectedWidth: Int? = null,
        expectedHeight: Int? = null,
        expectedFrameRate: Int? = null,
        requestedVideoBitrate: Int,
        averageVideoBitrate: Int?,
    ): RenderedOutputValidation {
        val durationDriftMs = if (expectedDurationMs > 0L) {
            kotlin.math.abs(metadata.durationMs - expectedDurationMs)
        } else {
            0L
        }
        val allowedDurationDriftMs = allowedDurationDriftMs(expectedDurationMs)
        val errors = buildList {
            if (metadata.width <= 0 || metadata.height <= 0) {
                add("Output dimensions are missing")
            } else {
                // Android video tracks may keep portrait content as coded 1920x1080 plus a
                // 90/270-degree rotation matrix. Validate the user-visible/display geometry,
                // while still checking H.264 evenness against the coded dimensions.
                val displayWidth = metadata.displayWidth
                val displayHeight = metadata.displayHeight
                if (
                    expectedWidth != null &&
                    expectedHeight != null &&
                    (displayWidth != expectedWidth || displayHeight != expectedHeight)
                ) {
                    add(
                        "Expected exact ${expectedWidth}x${expectedHeight}, but received " +
                            "display ${displayWidth}x${displayHeight} " +
                            "(coded ${metadata.width}x${metadata.height}, " +
                            "rotation ${metadata.rotationDegrees}°)",
                    )
                } else if (minOf(displayWidth, displayHeight) != preset.shortSidePixels) {
                    add(
                        "Expected an exact ${preset.shortSidePixels}px short side, " +
                            "but received display ${displayWidth}x${displayHeight} " +
                            "(coded ${metadata.width}x${metadata.height}, " +
                            "rotation ${metadata.rotationDegrees}°)",
                    )
                }
                if (metadata.width % 2 != 0 || metadata.height % 2 != 0) {
                    add("H.264 output dimensions must be even")
                }
            }
            if (!metadata.videoMimeType.equals(VIDEO_AVC_MIME, ignoreCase = true)) {
                add("Expected H.264 video, but received ${metadata.videoMimeType ?: "no video"}")
            }
            if (expectedFrameRate != null && metadata.frameRate > 0.0 &&
                kotlin.math.abs(metadata.frameRate - expectedFrameRate.toDouble()) > FRAME_RATE_TOLERANCE
            ) {
                add(
                    "Expected approximately ${expectedFrameRate}fps, but finalized track reports " +
                        "${"%.3f".format(java.util.Locale.US, metadata.frameRate)}fps",
                )
            }
            if (expectedAudio && !metadata.audioMimeType.equals(AUDIO_AAC_MIME, true)) {
                add("Expected AAC audio, but received ${metadata.audioMimeType ?: "no audio"}")
            }
            if (!expectedAudio && metadata.audioMimeType != null) {
                add("Mute requested video-only output, but an audio track is present")
            }
            if (
                expectedDurationMs > 0L &&
                durationDriftMs > allowedDurationDriftMs
            ) {
                add(
                    "Output duration ${metadata.durationMs}ms differs from the reviewed plan " +
                        "${expectedDurationMs}ms by ${durationDriftMs}ms; allowed drift is " +
                        "${allowedDurationDriftMs}ms",
                )
            }
        }
        val warnings = buildList {
            if (expectedFrameRate != null && metadata.frameRate <= 0.0) {
                add("Finalized track did not expose frame-rate metadata; verify FPS on device evidence")
            }
            if (
                averageVideoBitrate != null && averageVideoBitrate > 0 &&
                averageVideoBitrate < requestedVideoBitrate * 35L / 100L
            ) {
                add(
                    "VBR average bitrate is below 35% of the requested target; " +
                        "review visual quality before publishing",
                )
            }
            if (
                expectedDurationMs > 0L && durationDriftMs > BASE_DURATION_DRIFT_MS &&
                durationDriftMs <= allowedDurationDriftMs
            ) {
                add(
                    "Output duration drift ${durationDriftMs}ms is inside the bounded " +
                        "${allowedDurationDriftMs}ms frame/codec tolerance",
                )
            }
        }
        return RenderedOutputValidation(
            errors = errors,
            warnings = warnings,
            durationDriftMs = durationDriftMs,
            allowedDurationDriftMs = allowedDurationDriftMs,
        )
    }

    /**
     * Output FPS may be 24-60 depending on source and AAC is packetized separately. Keep the historical
     * 250 ms floor, allow at most 0.1% duration drift for longer files, and cap the exception at
     * 750 ms so a real clip/timeline mismatch still fails.
     */
    fun allowedDurationDriftMs(expectedDurationMs: Long): Long {
        if (expectedDurationMs <= 0L) return BASE_DURATION_DRIFT_MS
        val proportionalDriftMs = (expectedDurationMs + 999L) / 1_000L
        return maxOf(BASE_DURATION_DRIFT_MS, proportionalDriftMs)
            .coerceAtMost(MAX_DURATION_DRIFT_MS)
    }

    private const val VIDEO_AVC_MIME = "video/avc"
    private const val AUDIO_AAC_MIME = "audio/mp4a-latm"
    const val BASE_DURATION_DRIFT_MS = 250L
    const val MAX_DURATION_DRIFT_MS = 750L
    const val FRAME_RATE_TOLERANCE = 1.0
}
