package com.recapflow.ai.media.edit

import com.recapflow.ai.media.render.RenderPreset
import kotlin.test.Test
import kotlin.test.assertEquals

class PreviewAspectPolicyTest {

    @Test
    fun ordinarySourcePlaybackKeepsPlayerViewAsAspectOwner() {
        assertEquals(
            PreviewAspectOwner.PLAYER_VIEW,
            PreviewAspectPolicy.resolve(
                settings = TransformSettings(),
                preset = RenderPreset.HD_720P,
                liveEffectsAvailable = true,
            ),
        )
    }

    @Test
    fun portraitToLandscapeFitLetsPresentationOwnAspect() {
        assertEquals(
            PreviewAspectOwner.VIDEO_EFFECTS,
            PreviewAspectPolicy.resolve(
                settings = TransformSettings(
                    enabled = true,
                    aspectRatio = AspectRatioPreset.LANDSCAPE_16_9,
                    scaleMode = ScaleMode.FIT,
                ),
                preset = RenderPreset.HD_720P,
                liveEffectsAvailable = true,
            ),
        )
    }

    @Test
    fun customCropAlsoOwnsOutputFrameGeometry() {
        assertEquals(
            PreviewAspectOwner.VIDEO_EFFECTS,
            PreviewAspectPolicy.resolve(
                settings = TransformSettings(
                    enabled = true,
                    crop = CropSettings(enabled = true),
                ),
                preset = RenderPreset.HD_720P,
                liveEffectsAvailable = true,
            ),
        )
    }

    @Test
    fun previewFallbackReturnsAspectOwnershipToPlayerView() {
        assertEquals(
            PreviewAspectOwner.PLAYER_VIEW,
            PreviewAspectPolicy.resolve(
                settings = TransformSettings(
                    enabled = true,
                    aspectRatio = AspectRatioPreset.LANDSCAPE_16_9,
                    scaleMode = ScaleMode.FIT,
                ),
                preset = RenderPreset.HD_720P,
                liveEffectsAvailable = false,
            ),
        )
    }
}
