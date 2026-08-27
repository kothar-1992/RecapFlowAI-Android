package com.recapflow.ai.media

internal class NativeProbePayload(
    val code: Int,
    val stage: Int,
    val message: String,
    val ffmpegError: String?,
    val recoverable: Boolean,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val frameRate: Double,
    val bitrate: Long,
    val videoCodec: String,
    val audioCodec: String?,
    val audioSampleRate: Int,
    val audioChannels: Int,
    val containerFormat: String,
)
