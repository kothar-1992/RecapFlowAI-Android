package com.recapflow.ai.media.edit

data class CompiledSourceSubtitleBlur(
    val rectangle: BlurRectangle,
    /** Blur radius expressed in pixels at a 720-pixel short side. */
    val strength: Float,
    val startMs: Long,
    val endMs: Long,
) {
    fun isActiveAt(sourceTimeMs: Long): Boolean = sourceTimeMs in startMs until endMs

    fun intersects(range: TrimRange): Boolean = startMs < range.endMs && endMs > range.startMs
}

data class CompiledImageOverlay(
    val asset: ImageOverlayAsset,
    val centerX: Float,
    val centerY: Float,
    val widthFraction: Float,
    val opacity: Float,
    val startMs: Long,
    val endMs: Long,
    val animation: ImageOverlayAnimationSettings = ImageOverlayAnimationSettings(),
) {
    fun isActiveAt(sourceTimeMs: Long): Boolean = sourceTimeMs in startMs until endMs

    fun intersects(range: TrimRange): Boolean = startMs < range.endMs && endMs > range.startMs

    fun animationPhaseAt(sourceTimeMs: Long): ImageOverlayAnimationPhase =
        ImageOverlayAnimationPolicy.resolve(
            settings = animation,
            windowLocalTimeMs = (sourceTimeMs - startMs).coerceAtLeast(0L),
        )
}

/** Compiles only the manual Overlay operations whose master and item switches are enabled. */
object OverlayCompiler {
    const val MIN_BLUR_STRENGTH = 4f
    const val MAX_BLUR_STRENGTH = 32f
    const val DEFAULT_BLUR_STRENGTH = 14f
    const val MIN_BLUR_DURATION_MS = 250L
    const val MIN_IMAGE_WIDTH_FRACTION = 0.08f
    const val MAX_IMAGE_WIDTH_FRACTION = 0.80f
    const val DEFAULT_IMAGE_WIDTH_FRACTION = 0.22f
    const val MIN_IMAGE_OPACITY = 0.10f
    const val MAX_IMAGE_OPACITY = 1f
    const val DEFAULT_IMAGE_OPACITY = 1f
    const val DEFAULT_IMAGE_CENTER_X = 0.86f
    const val DEFAULT_IMAGE_CENTER_Y = 0.12f
    const val MIN_IMAGE_DURATION_MS = 250L

    fun compile(settings: OverlaySettings): CompiledSourceSubtitleBlur? {
        val blur = settings.sourceSubtitleBlur
        if (!settings.enabled || !blur.enabled) return null
        return CompiledSourceSubtitleBlur(
            rectangle = blur.rectangle,
            strength = blur.strength,
            startMs = blur.startMs,
            endMs = blur.endMs,
        )
    }

    fun compileImage(settings: OverlaySettings): CompiledImageOverlay? {
        val image = settings.image
        val asset = image.asset
        if (!settings.enabled || !image.enabled || asset == null) return null
        return CompiledImageOverlay(
            asset = asset,
            centerX = image.centerX,
            centerY = image.centerY,
            widthFraction = image.widthFraction,
            opacity = image.opacity,
            startMs = image.startMs,
            endMs = image.endMs,
            animation = image.animation,
        )
    }

    /**
     * Projects absolute source-timeline overlay windows into one clipped Media3 item's local
     * timeline. Media3 applies item effects before adding the sequence offset, so a clipped item
     * must compare its local presentation timestamp against local overlay bounds rather than add
     * source and composition offsets inside the shader.
     *
     * Geometry, strength, opacity and assets remain unchanged. Image animation also retains its
     * original source-time phase by advancing [ImageOverlayAnimationSettings.phaseOffsetMs] when a
     * reviewed clip begins after the overlay's absolute start. This prevents animation restart at
     * every Target-duration Clips boundary.
     *
     * An item with no time intersection receives that overlay disabled, which keeps the final graph
     * deterministic across long and multi-range edits.
     */
    fun projectToRange(settings: OverlaySettings, sourceRange: TrimRange): OverlaySettings {
        if (!settings.enabled) return settings

        val blur = settings.sourceSubtitleBlur
        val projectedBlur = if (!blur.enabled) {
            blur
        } else {
            val local = projectWindow(blur.startMs, blur.endMs, sourceRange)
            if (local == null) {
                blur.copy(enabled = false)
            } else {
                blur.copy(startMs = local.first, endMs = local.second)
            }
        }

        val image = settings.image
        val projectedImage = if (!image.enabled) {
            image
        } else {
            val absoluteImageStartMs = image.startMs
            val intersectionStartMs = maxOf(image.startMs, sourceRange.startMs)
            val local = projectWindow(image.startMs, image.endMs, sourceRange)
            if (local == null) {
                image.copy(enabled = false)
            } else {
                val phaseAdvanceMs = (intersectionStartMs - absoluteImageStartMs).coerceAtLeast(0L)
                image.copy(
                    startMs = local.first,
                    endMs = local.second,
                    animation = image.animation.copy(
                        phaseOffsetMs = image.animation.phaseOffsetMs + phaseAdvanceMs,
                    ),
                )
            }
        }

        return settings.copy(
            sourceSubtitleBlur = projectedBlur,
            image = projectedImage,
        )
    }

    private fun projectWindow(
        absoluteStartMs: Long,
        absoluteEndMs: Long,
        sourceRange: TrimRange,
    ): Pair<Long, Long>? {
        val start = maxOf(absoluteStartMs, sourceRange.startMs)
        val end = minOf(absoluteEndMs, sourceRange.endMs)
        if (end <= start) return null
        return (start - sourceRange.startMs) to (end - sourceRange.startMs)
    }

    fun hasOperationIntersecting(settings: OverlaySettings, range: TrimRange): Boolean =
        compile(settings)?.intersects(range) == true ||
            compileImage(settings)?.intersects(range) == true

    fun hasOperationActiveAt(settings: OverlaySettings, sourceTimeMs: Long): Boolean =
        compile(settings)?.isActiveAt(sourceTimeMs) == true ||
            compileImage(settings)?.isActiveAt(sourceTimeMs) == true
}
