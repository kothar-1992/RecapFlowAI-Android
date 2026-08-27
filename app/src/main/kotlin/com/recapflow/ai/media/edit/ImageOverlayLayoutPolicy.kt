package com.recapflow.ai.media.edit

data class ImageOverlayFrameBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** Aspect-preserving static-image bounds resolved against the post-Transform frame. */
object ImageOverlayLayoutPolicy {
    fun resolve(
        imageWidth: Int,
        imageHeight: Int,
        frameWidth: Int,
        frameHeight: Int,
        centerX: Float,
        centerY: Float,
        requestedWidthFraction: Float,
    ): ImageOverlayFrameBounds {
        val safeImageWidth = imageWidth.coerceAtLeast(1)
        val safeImageHeight = imageHeight.coerceAtLeast(1)
        val safeFrameWidth = frameWidth.coerceAtLeast(1)
        val safeFrameHeight = frameHeight.coerceAtLeast(1)
        val requestedWidth = requestedWidthFraction.coerceIn(
            OverlayCompiler.MIN_IMAGE_WIDTH_FRACTION,
            OverlayCompiler.MAX_IMAGE_WIDTH_FRACTION,
        )
        val requestedHeight = requestedWidth *
            safeImageHeight.toFloat() / safeImageWidth.toFloat() *
            safeFrameWidth.toFloat() / safeFrameHeight.toFloat()
        val fitScale = minOf(
            1f,
            MAX_FRAME_FRACTION / requestedWidth.coerceAtLeast(MIN_DIMENSION),
            MAX_FRAME_FRACTION / requestedHeight.coerceAtLeast(MIN_DIMENSION),
        )
        // Apply one shared scale so even unusually tall or wide logos keep their pixel aspect.
        // The validated source dimensions and width floor keep both results strictly positive.
        val width = (requestedWidth * fitScale).coerceAtMost(MAX_FRAME_FRACTION)
        val height = (requestedHeight * fitScale).coerceAtMost(MAX_FRAME_FRACTION)
        val left = (centerX - width / 2f).coerceIn(0f, 1f - width)
        val top = (centerY - height / 2f).coerceIn(0f, 1f - height)
        return ImageOverlayFrameBounds(left, top, left + width, top + height)
    }

    private const val MIN_DIMENSION = 0.005f
    private const val MAX_FRAME_FRACTION = 0.96f
}
