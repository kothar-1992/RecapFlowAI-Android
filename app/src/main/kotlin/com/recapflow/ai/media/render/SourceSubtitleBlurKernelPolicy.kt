package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.BlurRectangle
import kotlin.math.max
import kotlin.math.min

/**
 * Converts the editor's 4–32 strength scale into a dense, normalized GPU kernel.
 *
 * The server implementation maps strength to a bounded box-blur radius with
 * `radius = strength / 2`. The Android shader keeps that radius contract, then samples a dense
 * 9x9 square instead of placing a few full-strength copies far apart. This prevents readable
 * subtitle echoes while retaining one localized realtime/export effect.
 */
internal object SourceSubtitleBlurKernelPolicy {

    const val REFERENCE_SHORT_SIDE = 720f
    const val MAX_RADIUS_PIXELS_AT_REFERENCE = 24f
    const val KERNEL_HALF_WIDTH = 4f
    private const val MULTIPASS_EQUIVALENT_EXTENT = 1.5f
    private const val MIN_FEATHER_FRACTION = 0.0025f

    fun radiusPixelsAtReference(strength: Float): Float =
        (strength / 2f).coerceIn(1f, MAX_RADIUS_PIXELS_AT_REFERENCE)

    fun sampling(
        strength: Float,
        inputWidth: Int,
        inputHeight: Int,
        rectangle: BlurRectangle,
    ): SourceSubtitleBlurKernelSampling {
        val width = inputWidth.coerceAtLeast(1).toFloat()
        val height = inputHeight.coerceAtLeast(1).toFloat()
        val shortSide = min(width, height)
        val requestedRadiusPixels =
            radiusPixelsAtReference(strength) * shortSide / REFERENCE_SHORT_SIDE
        val regionWidthPixels = rectangle.width * width
        val regionHeightPixels = rectangle.height * height
        val regionRadiusLimit =
            max(1f, min(regionWidthPixels, regionHeightPixels) / 2f - 1f)
        val radiusPixels = min(requestedRadiusPixels, regionRadiusLimit)
        val extentPixels = min(radiusPixels * MULTIPASS_EQUIVALENT_EXTENT, regionRadiusLimit)
        val horizontalExtent = extentPixels / width
        val verticalExtent = extentPixels / height

        return SourceSubtitleBlurKernelSampling(
            horizontalStep = horizontalExtent / KERNEL_HALF_WIDTH,
            verticalStep = verticalExtent / KERNEL_HALF_WIDTH,
            horizontalFeather = max(horizontalExtent * 0.5f, MIN_FEATHER_FRACTION),
            verticalFeather = max(verticalExtent * 0.5f, MIN_FEATHER_FRACTION),
            radiusPixels = radiusPixels,
        )
    }
}

internal data class SourceSubtitleBlurKernelSampling(
    val horizontalStep: Float,
    val verticalStep: Float,
    val horizontalFeather: Float,
    val verticalFeather: Float,
    val radiusPixels: Float,
)
