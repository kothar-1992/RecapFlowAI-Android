package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.CompiledTransform
import com.recapflow.ai.media.edit.TransformCompiler
import com.recapflow.ai.media.edit.TransformSettings
import kotlin.math.min

/** Keeps interactive GPU work independent from the selected export resolution. */
object PreviewGeometryPolicy {
    const val MAX_SHORT_SIDE_PIXELS = 720

    fun shortSidePixels(sourceWidth: Int, sourceHeight: Int): Int {
        val sourceShortSide = min(sourceWidth, sourceHeight).coerceAtLeast(2)
        val bounded = sourceShortSide.coerceAtMost(MAX_SHORT_SIDE_PIXELS)
        return bounded - (bounded and 1)
    }

    fun compile(
        settings: TransformSettings,
        sourceWidth: Int,
        sourceHeight: Int,
    ): CompiledTransform? = TransformCompiler.compile(
        settings = settings,
        shortSidePixels = shortSidePixels(sourceWidth, sourceHeight),
    )
}
