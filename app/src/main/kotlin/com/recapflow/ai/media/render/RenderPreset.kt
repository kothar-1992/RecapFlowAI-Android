package com.recapflow.ai.media.render

enum class RenderPreset(
    val shortSidePixels: Int,
    val displayName: String,
    val accessibilityName: String,
    val minimumVideoBitrate: Int,
    val maximumVideoBitrate: Int,
) {
    HD_720P(
        shortSidePixels = 720,
        displayName = "720p",
        accessibilityName = "HD 720p",
        minimumVideoBitrate = 25_000_000,
        maximumVideoBitrate = 30_000_000,
    ),
    FULL_HD_1080P(
        shortSidePixels = 1080,
        displayName = "1080p",
        accessibilityName = "Full HD 1080p",
        minimumVideoBitrate = 30_000_000,
        maximumVideoBitrate = 45_000_000,
    ),
    QHD_2K(
        shortSidePixels = 1440,
        displayName = "2K",
        accessibilityName = "2K QHD 1440p",
        minimumVideoBitrate = 45_000_000,
        maximumVideoBitrate = 60_000_000,
    );

    companion object {
        val DEFAULT = FULL_HD_1080P
    }
}
