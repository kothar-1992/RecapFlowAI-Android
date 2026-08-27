package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.AspectRatioPreset
import com.recapflow.ai.media.edit.ScaleMode
import com.recapflow.ai.media.edit.TransformSettings

/**
 * Identifies Transform changes that alter Media3 Presentation geometry/layout.
 *
 * ExoPlayer can update most retained Effect parameters in-place, but changing the Presentation
 * target aspect or FIT/FILL layout while a TextureView is already bound can race the movable
 * preview card's layout. On affected HEVC devices that race leaves the decoded frame clipped to
 * one side of the card and can subsequently reject the live-effect graph. Geometry changes are
 * therefore rebound only after the preview card/surface has consumed its new bounds.
 */
object PreviewGeometryChangePolicy {

    data class GeometryIdentity(
        val presentationEnabled: Boolean,
        val aspectRatio: AspectRatioPreset,
        val scaleMode: ScaleMode?,
    )

    fun identity(settings: TransformSettings): GeometryIdentity {
        val presentationEnabled = settings.enabled &&
            settings.aspectRatio != AspectRatioPreset.ORIGINAL
        return GeometryIdentity(
            presentationEnabled = presentationEnabled,
            aspectRatio = if (presentationEnabled) {
                settings.aspectRatio
            } else {
                AspectRatioPreset.ORIGINAL
            },
            scaleMode = settings.scaleMode.takeIf { presentationEnabled },
        )
    }

    fun requiresSurfaceRebind(
        previous: TransformSettings?,
        requested: TransformSettings,
    ): Boolean {
        if (previous == null) return false
        return identity(previous) != identity(requested)
    }
}
