package com.recapflow.ai.media.edit

/**
 * Pure deterministic timing policy for Phase 6H.2 image/logo animation.
 *
 * The reviewed overlay window remains source-time based. [phaseOffsetMs] is compiler-owned
 * projection metadata used when a source-time overlay spans multiple reviewed clips: the next
 * clipped item resumes the same source-anchored animation phase instead of restarting at zero.
 * CompositionPlayer preview scales these timing values into presentation time after Speed so the
 * same phase is observed by preview and export.
 */
object ImageOverlayAnimationPolicy {
    const val MIN_DURATION_MS = 100L
    const val MAX_DURATION_MS = 10_000L
    const val DEFAULT_DURATION_MS = 700L

    const val MIN_PERIOD_MS = 100L
    const val MAX_PERIOD_MS = 30_000L
    const val DEFAULT_PERIOD_MS = 2_000L

    fun isValid(settings: ImageOverlayAnimationSettings): Boolean =
        settings.durationMs in MIN_DURATION_MS..MAX_DURATION_MS &&
            settings.periodMs in MIN_PERIOD_MS..MAX_PERIOD_MS &&
            settings.periodMs >= settings.durationMs &&
            settings.phaseOffsetMs >= 0L

    /**
     * Resolves the animation's normalized 0..1 progress at one time inside the visible overlay
     * window. Non-looping presets play once and remain at their settled end state. Looping presets
     * repeat every [ImageOverlayAnimationSettings.periodMs]; any time between animation duration and
     * the next period remains at the settled end state.
     */
    fun resolve(
        settings: ImageOverlayAnimationSettings,
        windowLocalTimeMs: Long,
    ): ImageOverlayAnimationPhase {
        require(isValid(settings)) { "Invalid image overlay animation settings: $settings" }

        if (settings.preset == ImageOverlayAnimationPreset.NONE) {
            return ImageOverlayAnimationPhase(
                progress = 1f,
                cycleIndex = 0L,
                animating = false,
            )
        }

        val elapsedMs = windowLocalTimeMs.coerceAtLeast(0L) + settings.phaseOffsetMs
        if (!settings.loopEnabled) {
            val animating = elapsedMs < settings.durationMs
            return ImageOverlayAnimationPhase(
                progress = normalizedProgress(elapsedMs, settings.durationMs),
                cycleIndex = 0L,
                animating = animating,
            )
        }

        val cycleIndex = elapsedMs / settings.periodMs
        val cycleTimeMs = elapsedMs % settings.periodMs
        val animating = cycleTimeMs < settings.durationMs
        return ImageOverlayAnimationPhase(
            progress = if (animating) {
                normalizedProgress(cycleTimeMs, settings.durationMs)
            } else {
                1f
            },
            cycleIndex = cycleIndex,
            animating = animating,
        )
    }

    private fun normalizedProgress(elapsedMs: Long, durationMs: Long): Float =
        (elapsedMs.toDouble() / durationMs.toDouble()).coerceIn(0.0, 1.0).toFloat()
}

data class ImageOverlayAnimationPhase(
    val progress: Float,
    val cycleIndex: Long,
    val animating: Boolean,
)
