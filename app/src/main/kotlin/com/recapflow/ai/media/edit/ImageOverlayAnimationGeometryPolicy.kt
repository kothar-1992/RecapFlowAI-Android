package com.recapflow.ai.media.edit

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Resolves animated logo geometry while keeping the full transformed rectangle inside the output
 * frame. The calculation uses the post-Transform normalized bounds, so it applies identically to
 * portrait, landscape, square, 720p and 1080p output.
 */
object ImageOverlayAnimationGeometryPolicy {
    fun resolve(
        bounds: ImageOverlayFrameBounds,
        visual: ImageOverlayAnimationVisualState,
    ): ImageOverlayAnimationGeometry {
        val halfWidth = (bounds.width / 2f).coerceAtLeast(MIN_HALF_EXTENT)
        val halfHeight = (bounds.height / 2f).coerceAtLeast(MIN_HALF_EXTENT)
        val baseCenterX = (bounds.left + bounds.right) / 2f
        val baseCenterY = (bounds.top + bounds.bottom) / 2f
        val rotationRadians = (visual.rotationDegrees.toDouble() * PI / 180.0).toFloat()
        val absCos = abs(cos(rotationRadians.toDouble())).toFloat()
        val absSin = abs(sin(rotationRadians.toDouble())).toFloat()

        val unitRotatedHalfWidth = absCos * halfWidth + absSin * halfHeight
        val unitRotatedHalfHeight = absSin * halfWidth + absCos * halfHeight
        val requestedScale = visual.scaleMultiplier.coerceAtLeast(MIN_SCALE)
        val maxScaleX = 0.5f / unitRotatedHalfWidth.coerceAtLeast(MIN_HALF_EXTENT)
        val maxScaleY = 0.5f / unitRotatedHalfHeight.coerceAtLeast(MIN_HALF_EXTENT)
        val safeScale = min(requestedScale, min(maxScaleX, maxScaleY)).coerceAtLeast(MIN_SCALE)

        val rotatedHalfWidth = unitRotatedHalfWidth * safeScale
        val rotatedHalfHeight = unitRotatedHalfHeight * safeScale
        val requestedCenterX = baseCenterX + visual.translateXInOverlayWidths * bounds.width
        val requestedCenterY = baseCenterY + visual.translateYInOverlayHeights * bounds.height
        val centerX = requestedCenterX.coerceIn(rotatedHalfWidth, 1f - rotatedHalfWidth)
        val centerY = requestedCenterY.coerceIn(rotatedHalfHeight, 1f - rotatedHalfHeight)

        return ImageOverlayAnimationGeometry(
            centerX = centerX,
            centerY = centerY,
            halfWidth = halfWidth,
            halfHeight = halfHeight,
            scaleMultiplier = safeScale,
            rotationRadians = rotationRadians,
        )
    }

    private const val MIN_HALF_EXTENT = 0.0001f
    private const val MIN_SCALE = 0.01f
}

data class ImageOverlayAnimationGeometry(
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    val scaleMultiplier: Float,
    val rotationRadians: Float,
) {
    val rotatedHalfWidth: Float
        get() {
            val c = abs(cos(rotationRadians.toDouble())).toFloat()
            val s = abs(sin(rotationRadians.toDouble())).toFloat()
            return (c * halfWidth + s * halfHeight) * scaleMultiplier
        }

    val rotatedHalfHeight: Float
        get() {
            val c = abs(cos(rotationRadians.toDouble())).toFloat()
            val s = abs(sin(rotationRadians.toDouble())).toFloat()
            return (s * halfWidth + c * halfHeight) * scaleMultiplier
        }
}
