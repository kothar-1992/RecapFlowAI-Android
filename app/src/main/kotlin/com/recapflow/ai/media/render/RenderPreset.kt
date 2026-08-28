package com.recapflow.ai.media.render

enum class RenderPreset(
    val shortSidePixels: Int,
    val displayName: String,
    val accessibilityName: String,
    val standardFrameRateVideoBitrate: Int,
    val highFrameRateVideoBitrate: Int,
) {
    HD_720P(720, "720p", "HD 720p", 5_000_000, 7_500_000),
    FULL_HD_1080P(1080, "1080p", "Full HD 1080p", 8_000_000, 12_000_000),
    QHD_2K(1440, "2K", "2K QHD 1440p", 16_000_000, 24_000_000);

    fun videoBitrateFor(frameRate: Int): Int =
        if (ExportFrameRatePolicy.isHighFrameRate(frameRate)) highFrameRateVideoBitrate
        else standardFrameRateVideoBitrate

    val minimumVideoBitrate: Int get() = standardFrameRateVideoBitrate
    val maximumVideoBitrate: Int get() = highFrameRateVideoBitrate

    companion object { val DEFAULT = FULL_HD_1080P }
}
