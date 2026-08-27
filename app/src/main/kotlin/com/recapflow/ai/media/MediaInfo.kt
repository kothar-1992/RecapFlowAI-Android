package com.recapflow.ai.media

data class PreparedMedia(
    val sourceUri: String,
    val workingFilePath: String,
    val displayName: String,
    val fileSizeBytes: Long,
)

data class MediaInfo(
    val sourceUri: String,
    val workingFilePath: String,
    val displayName: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val frameRate: Double,
    val videoCodec: String,
    val audioCodec: String?,
    val audioSampleRate: Int,
    val audioChannels: Int,
    val bitrate: Long,
    val containerFormat: String,
) {
    val hasAudio: Boolean
        get() = !audioCodec.isNullOrBlank() && audioChannels > 0

    val isPortrait: Boolean
        get() {
            val swapsDimensions = rotationDegrees == 90 || rotationDegrees == 270
            val displayWidth = if (swapsDimensions) height else width
            val displayHeight = if (swapsDimensions) width else height
            return displayHeight > displayWidth
        }
}
