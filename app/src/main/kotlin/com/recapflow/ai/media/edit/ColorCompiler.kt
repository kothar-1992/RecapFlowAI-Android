package com.recapflow.ai.media.edit

/**
 * Compiles UI percentage units into Media3 color-effect units. A disabled,
 * invalid, or neutral adjustment returns null so the optional operation is
 * omitted from both preview and export.
 */
object ColorCompiler {

    fun compile(settings: TransformSettings): CompiledColor? {
        val color = settings.color
        if (!settings.enabled || !color.enabled || !color.isValid() || color.isNeutral()) {
            return null
        }
        val temperatureScale = color.temperature / TEMPERATURE_SCALE_DIVISOR
        return CompiledColor(
            brightness = color.brightness / PERCENT_DIVISOR,
            contrast = color.contrast / PERCENT_DIVISOR,
            saturationAdjustment = color.saturation,
            redScale = 1f + temperatureScale,
            blueScale = 1f - temperatureScale,
        )
    }

    private const val PERCENT_DIVISOR = 100f
    private const val TEMPERATURE_SCALE_DIVISOR = 200f
}

data class CompiledColor(
    val brightness: Float,
    val contrast: Float,
    val saturationAdjustment: Float,
    val redScale: Float,
    val blueScale: Float,
)
