package com.recapflow.ai.ui

import android.content.Context
import android.text.format.Formatter
import com.recapflow.ai.R
import com.recapflow.ai.media.MediaInfo
import java.util.Locale

object MediaFormatters {

    fun duration(durationMs: Long): String {
        if (durationMs <= 0L) {
            return "--:--"
        }
        val totalSeconds = durationMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun resolution(info: MediaInfo): String =
        if (info.width > 0 && info.height > 0) {
            "${info.width} × ${info.height}"
        } else {
            "—"
        }

    fun orientation(context: Context, info: MediaInfo): String {
        val orientation = when {
            info.width == info.height && info.width > 0 -> context.getString(R.string.square)
            info.isPortrait -> context.getString(R.string.portrait)
            else -> context.getString(R.string.landscape)
        }
        return if (info.rotationDegrees == 0) {
            orientation
        } else {
            "$orientation • ${context.getString(R.string.rotation_degrees, info.rotationDegrees)}"
        }
    }

    fun frameRate(context: Context, frameRate: Double): String =
        if (frameRate > 0.0) {
            context.getString(R.string.fps_value, frameRate)
        } else {
            context.getString(R.string.unknown_value)
        }

    fun bitrate(context: Context, bitsPerSecond: Long): String {
        if (bitsPerSecond <= 0L) {
            return context.getString(R.string.unknown_value)
        }
        return if (bitsPerSecond >= 1_000_000L) {
            context.getString(R.string.bitrate_mbps, bitsPerSecond / 1_000_000.0)
        } else {
            context.getString(R.string.bitrate_kbps, bitsPerSecond / 1000.0)
        }
    }

    fun fileSize(context: Context, bytes: Long): String =
        if (bytes > 0L) {
            Formatter.formatFileSize(context, bytes)
        } else {
            context.getString(R.string.unknown_value)
        }

    fun container(context: Context, value: String): String {
        if (value.isBlank()) {
            return context.getString(R.string.unknown_value)
        }
        val lowercase = value.lowercase(Locale.US)
        return if ("mp4" in lowercase) {
            "MP4"
        } else {
            value.substringBefore(',').uppercase(Locale.US)
        }
    }

    fun codec(context: Context, value: String?): String {
        if (value.isNullOrBlank()) {
            return context.getString(R.string.unknown_value)
        }
        return when (value.lowercase(Locale.US)) {
            "h264" -> "H.264"
            "hevc" -> "HEVC"
            "aac" -> "AAC"
            "av1" -> "AV1"
            "vp9" -> "VP9"
            else -> value.uppercase(Locale.US)
        }
    }

    fun audio(context: Context, info: MediaInfo): String {
        if (!info.hasAudio) {
            return context.getString(R.string.no_audio)
        }
        val sampleRate = if (info.audioSampleRate > 0) {
            context.getString(
                R.string.sample_rate_khz,
                info.audioSampleRate / 1000.0,
            )
        } else {
            context.getString(R.string.unknown_value)
        }
        return context.getString(
            R.string.audio_summary,
            codec(context, info.audioCodec),
            sampleRate,
            info.audioChannels,
        )
    }

    fun copyProgress(context: Context, copied: Long, total: Long?): String {
        val copiedText = fileSize(context, copied)
        return if (total != null && total > 0L) {
            context.getString(
                R.string.progress_of_total,
                copiedText,
                fileSize(context, total),
            )
        } else {
            context.getString(R.string.progress_copied, copiedText)
        }
    }
}
