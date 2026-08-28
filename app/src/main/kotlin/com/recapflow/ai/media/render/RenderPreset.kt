package com.recapflow.ai.media.render

enum class RenderPreset(
    val shortSidePixels: Int,
    val displayName: String,
    val accessibilityName: String,
    val standardFrameRateVideoBitrate: Int,
    val highFrameRateVideoBitrate: Int,
) {
    HD_720P(
        shortSidePixels = 720,
        displayName = "720p",
        accessibilityName = "HD 720p",
        standardFrameRateVideoBitrate = 5_000_000,
        highFrameRateVideoBitrate = 7_500_000,
    ),
    FULL_HD_1080P(
        shortSidePixels = 1080,
        displayName = "1080p",
        accessibilityName = "Full HD 1080p",
        standardFrameRateVideoBitrate = 8_000_000,
        highFrameRateVideoBitrate = 12_000_000,
    ),
    QHD_2K(
        shortSidePixels = 1440,
        displayName = "2K",
        accessibilityName = "2K QHD 1440p",
        standardFrameRateVideoBitrate = 16_000_000,
        highFrameRateVideoBitrate = 24_000_000,
    );

    fun videoBitrateFor(frameRate: Int): Int =
        if (ExportFrameRatePolicy.isHighFrameRate(frameRate)) {
            highFrameRateVideoBitrate
        } else {
            standardFrameRateVideoBitrate
        }

    val minimumVideoBitrate: Int
        get() = standardFrameRateVideoBitrate

    val maximumVideoBitrate: Int
        get() = highFrameRateVideoBitrate

    companion object {
        val DEFAULT = FULL_HD_1080P
    }
}
