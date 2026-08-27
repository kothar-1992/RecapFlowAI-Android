package com.recapflow.ai.media.edit

import com.recapflow.ai.media.render.RenderPreset

data class CompiledTransform(
    val targetWidth: Int,
    val targetHeight: Int,
    val scaleMode: ScaleMode,
)

object TransformCompiler {

    /**
     * Returns null when the optional transform is disabled or keeps the source
     * aspect. The render layer then uses its already-verified short-side scale.
     */
    fun compile(
        settings: TransformSettings,
        preset: RenderPreset,
    ): CompiledTransform? = compile(settings, preset.shortSidePixels)

    /**
     * Compiles geometry for a caller-owned short side. Preview uses this overload so it can
     * remain bounded by the source/device budget instead of inheriting the export resolution.
     */
    fun compile(
        settings: TransformSettings,
        shortSidePixels: Int,
    ): CompiledTransform? {
        if (!settings.enabled || settings.aspectRatio == AspectRatioPreset.ORIGINAL) {
            return null
        }

        val widthUnits = checkNotNull(settings.aspectRatio.widthUnits)
        val heightUnits = checkNotNull(settings.aspectRatio.heightUnits)
        val shortSide = shortSidePixels.coerceAtLeast(2).let { it - (it and 1) }
        val (width, height) = if (widthUnits <= heightUnits) {
            shortSide to scaledLongSide(shortSide, heightUnits, widthUnits)
        } else {
            scaledLongSide(shortSide, widthUnits, heightUnits) to shortSide
        }
        return CompiledTransform(
            targetWidth = width,
            targetHeight = height,
            scaleMode = settings.scaleMode,
        )
    }

    private fun scaledLongSide(shortSide: Int, longUnits: Int, shortUnits: Int): Int {
        val raw = (shortSide.toLong() * longUnits.toLong() / shortUnits.toLong()).toInt()
        return raw + (raw and 1)
    }
}
