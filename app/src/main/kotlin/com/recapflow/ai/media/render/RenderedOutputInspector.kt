package com.recapflow.ai.media.render

import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File

/** Reads only finalized local MP4 track metadata; no frame decode or network access is used. */
object RenderedOutputInspector {
    fun inspect(file: File): RenderedOutputMetadata {
        require(file.isFile && file.length() > 0L) { "Rendered output is missing or empty" }
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            var width = 0
            var height = 0
            var durationMs = 0L
            var rotationDegrees = 0
            var frameRate = 0.0
            var videoMimeType: String? = null
            var audioMimeType: String? = null
            for (trackIndex in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(trackIndex)
                val mimeType = format.getString(MediaFormat.KEY_MIME)
                when {
                    mimeType?.startsWith("video/") == true && videoMimeType == null -> {
                        videoMimeType = mimeType
                        width = format.integerOrZero(MediaFormat.KEY_WIDTH)
                        height = format.integerOrZero(MediaFormat.KEY_HEIGHT)
                        rotationDegrees = format.rotationDegreesOrZero()
                        frameRate = format.frameRateOrZero()
                        durationMs = maxOf(durationMs, format.durationMsOrZero())
                    }
                    mimeType?.startsWith("audio/") == true && audioMimeType == null -> {
                        audioMimeType = mimeType
                        durationMs = maxOf(durationMs, format.durationMsOrZero())
                    }
                }
            }
            RenderedOutputMetadata(
                width = width,
                height = height,
                rotationDegrees = rotationDegrees,
                frameRate = frameRate,
                durationMs = durationMs,
                videoMimeType = videoMimeType,
                audioMimeType = audioMimeType,
            )
        } finally {
            extractor.release()
        }
    }

    private fun MediaFormat.integerOrZero(key: String): Int =
        if (containsKey(key)) getInteger(key) else 0

    private fun MediaFormat.rotationDegreesOrZero(): Int {
        val raw = if (containsKey(MediaFormat.KEY_ROTATION)) {
            getInteger(MediaFormat.KEY_ROTATION)
        } else {
            0
        }
        return ((raw % 360) + 360) % 360
    }

    private fun MediaFormat.frameRateOrZero(): Double {
        if (!containsKey(MediaFormat.KEY_FRAME_RATE)) return 0.0
        return runCatching { getFloat(MediaFormat.KEY_FRAME_RATE).toDouble() }
            .recoverCatching { getInteger(MediaFormat.KEY_FRAME_RATE).toDouble() }
            .getOrDefault(0.0)
    }

    private fun MediaFormat.durationMsOrZero(): Long =
        if (containsKey(MediaFormat.KEY_DURATION)) {
            (getLong(MediaFormat.KEY_DURATION) / 1_000L).coerceAtLeast(0L)
        } else {
            0L
        }
}
