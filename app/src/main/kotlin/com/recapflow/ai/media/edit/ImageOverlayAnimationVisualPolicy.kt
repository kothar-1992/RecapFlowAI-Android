package com.recapflow.ai.media.edit

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Converts one deterministic animation phase into visual transform multipliers.
 *
 * Translation is expressed in units of the resolved overlay width/height rather than frame pixels,
 * so the same semantic animation survives aspect conversion and 720p/1080p export without changing
 * its apparent motion. [ImageOverlayAnimationPreset.NONE] always returns [IDENTITY]; the GL effect
 * keeps a dedicated legacy-static shader branch for that preset.
 */
object ImageOverlayAnimationVisualPolicy {
    val IDENTITY = ImageOverlayAnimationVisualState()

    fun resolve(
        settings: ImageOverlayAnimationSettings,
        phase: ImageOverlayAnimationPhase,
    ): ImageOverlayAnimationVisualState {
        if (settings.preset == ImageOverlayAnimationPreset.NONE || !phase.animating) {
            return IDENTITY
        }

        val progress = phase.progress.coerceIn(0f, 1f)
        val eased = easeOutCubic(progress)
        return when (settings.preset) {
            ImageOverlayAnimationPreset.NONE -> IDENTITY
            ImageOverlayAnimationPreset.FADE -> ImageOverlayAnimationVisualState(
                opacityMultiplier = smoothStep(progress),
            )
            ImageOverlayAnimationPreset.FADE_SCALE -> ImageOverlayAnimationVisualState(
                opacityMultiplier = smoothStep(progress),
                scaleMultiplier = 0.82f + 0.18f * eased,
            )
            ImageOverlayAnimationPreset.POP -> ImageOverlayAnimationVisualState(
                opacityMultiplier = smoothStep(progress),
                scaleMultiplier = 0.65f + 0.35f * easeOutBack(progress),
            )
            ImageOverlayAnimationPreset.SLIDE -> ImageOverlayAnimationVisualState(
                opacityMultiplier = smoothStep(progress),
                translateXInOverlayWidths = 1.20f * (1f - eased),
            )
            ImageOverlayAnimationPreset.PULSE -> ImageOverlayAnimationVisualState(
                scaleMultiplier = 1f + 0.08f * sinTurn(progress),
            )
            ImageOverlayAnimationPreset.FLOAT -> ImageOverlayAnimationVisualState(
                translateYInOverlayHeights = -0.20f * sinTurn(progress),
            )
            ImageOverlayAnimationPreset.ROTATE -> ImageOverlayAnimationVisualState(
                rotationDegrees = 360f * progress,
            )
            ImageOverlayAnimationPreset.BOUNCE -> ImageOverlayAnimationVisualState(
                translateYInOverlayHeights = -0.35f *
                    abs(sin(PI * progress.toDouble())).toFloat(),
            )
        }
    }

    private fun smoothStep(value: Float): Float = value * value * (3f - 2f * value)

    private fun easeOutCubic(value: Float): Float {
        val inverse = 1f - value
        return 1f - inverse * inverse * inverse
    }

    private fun easeOutBack(value: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val shifted = value - 1f
        return 1f + c3 * shifted * shifted * shifted + c1 * shifted * shifted
    }

    private fun sinTurn(progress: Float): Float =
        sin(2.0 * PI * progress.toDouble()).toFloat()
}

data class ImageOverlayAnimationVisualState(
    val opacityMultiplier: Float = 1f,
    val scaleMultiplier: Float = 1f,
    val translateXInOverlayWidths: Float = 0f,
    val translateYInOverlayHeights: Float = 0f,
    val rotationDegrees: Float = 0f,
)
