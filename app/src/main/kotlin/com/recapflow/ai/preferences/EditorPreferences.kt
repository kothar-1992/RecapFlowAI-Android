package com.recapflow.ai.preferences

import com.recapflow.ai.media.edit.AdaptiveCutPreset
import com.recapflow.ai.media.edit.AudioCompiler
import com.recapflow.ai.media.edit.AudioPolicy
import com.recapflow.ai.media.edit.BlurRectangle
import com.recapflow.ai.media.edit.ColorSettings
import com.recapflow.ai.media.edit.CropRectangle
import com.recapflow.ai.media.edit.CropSettings
import com.recapflow.ai.media.edit.FreezeCompiler
import com.recapflow.ai.media.edit.FreezeSettings
import com.recapflow.ai.media.edit.ImageOverlayAnimationPolicy
import com.recapflow.ai.media.edit.ImageOverlayAnimationPreset
import com.recapflow.ai.media.edit.OverlayCompiler
import com.recapflow.ai.media.edit.SpeedCompiler
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TransitionCompiler
import com.recapflow.ai.media.edit.TransitionMode
import com.recapflow.ai.media.edit.TransitionSettings
import com.recapflow.ai.media.edit.ZoomMode
import com.recapflow.ai.media.edit.ZoomSettings
import com.recapflow.ai.media.render.RenderPreset

/**
 * Version-independent editor choices that are safe to restore without reopening user media.
 * Source paths, imported asset paths, trim ranges, player position and render outputs are
 * deliberately excluded.
 */
data class EditorPreferencesSnapshot(
    val transform: TransformSettings = TransformSettings(speed = DEFAULT_SPEED_MULTIPLIER),
    val audio: AudioPreference = AudioPreference(),
    val overlay: OverlayPreference = OverlayPreference(),
    val adaptivePreset: AdaptiveCutPreset = AdaptiveCutPreset.BALANCED,
    val renderPreset: RenderPreset = RenderPreset.DEFAULT,
    val selectedSection: EditorSection = EditorSection.CLIPS,
    val transformDetailsVisible: Boolean = true,
    val overlayDetailsVisible: Boolean = true,
    val previewScale: Float = DEFAULT_PREVIEW_SCALE,
    val previewCenterX: Float = DEFAULT_PREVIEW_CENTER_X,
    val previewCenterY: Float? = null,
) {
    companion object {
        const val DEFAULT_SPEED_MULTIPLIER = 1.25f
        const val DEFAULT_PREVIEW_SCALE = 1f
        const val DEFAULT_PREVIEW_CENTER_X = 0.5f
        const val MIN_PREVIEW_SCALE = 0.55f
        const val MAX_PERSISTED_PREVIEW_SCALE = 2.5f
    }
}

data class AudioPreference(
    val enabled: Boolean = false,
    val policy: AudioPolicy = AudioPolicy.KEEP_ORIGINAL,
    val volume: Float = AudioCompiler.UNITY_LINEAR_GAIN,
    val mixSourceVolume: Float = AudioCompiler.DEFAULT_MIX_SOURCE_LINEAR_GAIN,
    val mixAddedVolume: Float = AudioCompiler.DEFAULT_MIX_LINEAR_GAIN,
)

data class OverlayPreference(
    val enabled: Boolean = false,
    val blurEnabled: Boolean = false,
    val blurRectangle: BlurRectangle = BlurRectangle(),
    val blurStrength: Float = OverlayCompiler.DEFAULT_BLUR_STRENGTH,
    val imageEnabled: Boolean = false,
    val imageCenterX: Float = OverlayCompiler.DEFAULT_IMAGE_CENTER_X,
    val imageCenterY: Float = OverlayCompiler.DEFAULT_IMAGE_CENTER_Y,
    val imageWidthFraction: Float = OverlayCompiler.DEFAULT_IMAGE_WIDTH_FRACTION,
    val imageOpacity: Float = OverlayCompiler.DEFAULT_IMAGE_OPACITY,
    val imageAnimationPreset: ImageOverlayAnimationPreset = ImageOverlayAnimationPreset.NONE,
    val imageAnimationLoopEnabled: Boolean = false,
    val imageAnimationDurationMs: Long = ImageOverlayAnimationPolicy.DEFAULT_DURATION_MS,
    val imageAnimationPeriodMs: Long = ImageOverlayAnimationPolicy.DEFAULT_PERIOD_MS,
)

enum class EditorSection {
    CLIPS,
    TRANSFORM,
    AUDIO,
    OVERLAY,
    EXPORT,
}

/** Rejects stale/corrupt preference values before they reach Media3 or the view layer. */
object EditorPreferencesPolicy {
    fun sanitize(input: EditorPreferencesSnapshot): EditorPreferencesSnapshot {
        val transform = input.transform
        val safeCrop = transform.crop.rectangle.takeIf(CropRectangle::isValid)
            ?: CropRectangle()
        val safeColor = transform.color.takeIf(ColorSettings::isValid)
            ?: ColorSettings(enabled = transform.color.enabled)
        val safeZoomMode = transform.zoom.mode.takeUnless { it == ZoomMode.OFF } ?: ZoomMode.IN
        val safeTransitionMode = transform.transition.mode
            .takeUnless { it == TransitionMode.OFF }
            ?: TransitionMode.FADE_IN_OUT
        val safeTransform = transform.copy(
            crop = CropSettings(transform.crop.enabled, safeCrop),
            color = safeColor,
            zoom = ZoomSettings(transform.zoom.enabled, safeZoomMode),
            speed = transform.speed.takeIf { it in SpeedCompiler.supportedPresets }
                ?: EditorPreferencesSnapshot.DEFAULT_SPEED_MULTIPLIER,
            freeze = FreezeSettings(
                enabled = transform.freeze.enabled,
                durationMs = transform.freeze.durationMs
                    .takeIf { it in FreezeCompiler.supportedDurationsMs }
                    ?: FreezeCompiler.DEFAULT_DURATION_MS,
            ),
            transition = TransitionSettings(
                enabled = transform.transition.enabled,
                mode = safeTransitionMode,
                durationMs = transform.transition.durationMs
                    .takeIf { it in TransitionCompiler.supportedDurationsMs }
                    ?: TransitionCompiler.DEFAULT_DURATION_MS,
            ),
        )
        val safeAudio = input.audio.copy(
            volume = input.audio.volume.coerceIn(
                AudioCompiler.MIN_LINEAR_GAIN,
                AudioCompiler.MAX_LINEAR_GAIN,
            ),
            mixSourceVolume = input.audio.mixSourceVolume.coerceIn(
                AudioCompiler.MIN_LINEAR_GAIN,
                AudioCompiler.MAX_LINEAR_GAIN,
            ),
            mixAddedVolume = input.audio.mixAddedVolume.coerceIn(
                AudioCompiler.MIN_LINEAR_GAIN,
                AudioCompiler.MAX_LINEAR_GAIN,
            ),
        )
        val safeBlur = input.overlay.blurRectangle.takeIf(BlurRectangle::isValid)
            ?: BlurRectangle()
        val safeAnimationDurationMs = input.overlay.imageAnimationDurationMs.coerceIn(
            ImageOverlayAnimationPolicy.MIN_DURATION_MS,
            ImageOverlayAnimationPolicy.MAX_DURATION_MS,
        )
        val safeAnimationPeriodMs = input.overlay.imageAnimationPeriodMs.coerceIn(
            maxOf(ImageOverlayAnimationPolicy.MIN_PERIOD_MS, safeAnimationDurationMs),
            ImageOverlayAnimationPolicy.MAX_PERIOD_MS,
        )
        val safeOverlay = input.overlay.copy(
            blurRectangle = safeBlur,
            blurStrength = input.overlay.blurStrength.coerceIn(
                OverlayCompiler.MIN_BLUR_STRENGTH,
                OverlayCompiler.MAX_BLUR_STRENGTH,
            ),
            imageCenterX = input.overlay.imageCenterX.coerceIn(0f, 1f),
            imageCenterY = input.overlay.imageCenterY.coerceIn(0f, 1f),
            imageWidthFraction = input.overlay.imageWidthFraction.coerceIn(
                OverlayCompiler.MIN_IMAGE_WIDTH_FRACTION,
                OverlayCompiler.MAX_IMAGE_WIDTH_FRACTION,
            ),
            imageOpacity = input.overlay.imageOpacity.coerceIn(
                OverlayCompiler.MIN_IMAGE_OPACITY,
                OverlayCompiler.MAX_IMAGE_OPACITY,
            ),
            imageAnimationDurationMs = safeAnimationDurationMs,
            imageAnimationPeriodMs = safeAnimationPeriodMs,
        )
        return input.copy(
            transform = safeTransform,
            audio = safeAudio,
            overlay = safeOverlay,
            previewScale = input.previewScale.coerceIn(
                EditorPreferencesSnapshot.MIN_PREVIEW_SCALE,
                EditorPreferencesSnapshot.MAX_PERSISTED_PREVIEW_SCALE,
            ),
            previewCenterX = input.previewCenterX.coerceIn(0f, 1f),
            previewCenterY = input.previewCenterY?.coerceIn(0f, 1f),
        )
    }
}
