package com.recapflow.ai.media.edit

import com.recapflow.ai.media.render.RenderPreset

/**
 * Selects the single component that owns source-to-output aspect conversion.
 *
 * Media3 Presentation/Crop already produces the intended output-frame geometry. In that
 * case PlayerView must display the complete effect output inside the matching preview card
 * without applying a second source-aspect correction. Ordinary source playback still lets
 * PlayerView preserve the decoded video's aspect ratio.
 */
enum class PreviewAspectOwner {
    PLAYER_VIEW,
    VIDEO_EFFECTS,
}

object PreviewAspectPolicy {

    fun resolve(
        settings: TransformSettings,
        preset: RenderPreset,
        liveEffectsAvailable: Boolean,
    ): PreviewAspectOwner {
        if (!liveEffectsAvailable) return PreviewAspectOwner.PLAYER_VIEW

        val hasOutputGeometry =
            TransformCompiler.compile(settings, preset) != null ||
                CropCompiler.compile(settings) != null
        return if (hasOutputGeometry) {
            PreviewAspectOwner.VIDEO_EFFECTS
        } else {
            PreviewAspectOwner.PLAYER_VIEW
        }
    }
}
