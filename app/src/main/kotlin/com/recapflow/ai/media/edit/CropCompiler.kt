package com.recapflow.ai.media.edit

data class CompiledCrop(
    val leftNdc: Float,
    val rightNdc: Float,
    val bottomNdc: Float,
    val topNdc: Float,
)

object CropCompiler {

    /**
     * Compiles normalized top-left UI coordinates into Media3's -1..1 NDC
     * coordinates. A disabled Transform or disabled Crop is omitted completely.
     */
    fun compile(settings: TransformSettings): CompiledCrop? {
        if (!settings.enabled || !settings.crop.enabled) {
            return null
        }

        val rectangle = settings.crop.rectangle
        check(rectangle.isValid()) { "Crop rectangle is invalid" }
        return CompiledCrop(
            leftNdc = toHorizontalNdc(rectangle.left),
            rightNdc = toHorizontalNdc(rectangle.right),
            bottomNdc = toVerticalNdc(rectangle.bottom),
            topNdc = toVerticalNdc(rectangle.top),
        )
    }

    private fun toHorizontalNdc(value: Float): Float = -1f + (2f * value)

    private fun toVerticalNdc(valueFromTop: Float): Float = 1f - (2f * valueFromTop)
}
