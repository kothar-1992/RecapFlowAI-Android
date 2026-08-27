package com.recapflow.ai.preferences

import com.recapflow.ai.media.edit.AudioCompiler
import com.recapflow.ai.media.edit.BlurRectangle
import com.recapflow.ai.media.edit.ColorSettings
import com.recapflow.ai.media.edit.CropRectangle
import com.recapflow.ai.media.edit.CropSettings
import com.recapflow.ai.media.edit.FreezeSettings
import com.recapflow.ai.media.edit.OverlayCompiler
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TransitionMode
import com.recapflow.ai.media.edit.TransitionSettings
import com.recapflow.ai.media.edit.ZoomMode
import com.recapflow.ai.media.edit.ZoomSettings
import com.recapflow.ai.media.render.RenderPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorPreferencesPolicyTest {
    @Test
    fun sanitize_replacesInvalidDiscreteAndGeometryValues() {
        val input = EditorPreferencesSnapshot(
            transform = TransformSettings(
                crop = CropSettings(
                    enabled = true,
                    rectangle = CropRectangle(0.9f, 0.9f, 0.2f, 0.2f),
                ),
                color = ColorSettings(enabled = true, brightness = 500f),
                zoom = ZoomSettings(enabled = true, mode = ZoomMode.OFF),
                speedEnabled = true,
                speed = 9f,
                freeze = FreezeSettings(enabled = true, durationMs = 99_999L),
                transition = TransitionSettings(
                    enabled = true,
                    mode = TransitionMode.OFF,
                    durationMs = 99_999L,
                ),
            ),
            overlay = OverlayPreference(
                blurEnabled = true,
                blurRectangle = BlurRectangle(0.8f, 0.8f, 0.1f, 0.1f),
                blurStrength = 999f,
                imageCenterX = -10f,
                imageCenterY = 10f,
                imageWidthFraction = 10f,
                imageOpacity = -1f,
            ),
            previewScale = 99f,
            previewCenterX = -5f,
            previewCenterY = 8f,
        )

        val result = EditorPreferencesPolicy.sanitize(input)

        assertTrue(result.transform.crop.rectangle.isValid())
        assertTrue(result.transform.color.isValid())
        assertEquals(ZoomMode.IN, result.transform.zoom.mode)
        assertEquals(EditorPreferencesSnapshot.DEFAULT_SPEED_MULTIPLIER, result.transform.speed)
        assertEquals(TransitionMode.FADE_IN_OUT, result.transform.transition.mode)
        assertTrue(result.overlay.blurRectangle.isValid())
        assertEquals(OverlayCompiler.MAX_BLUR_STRENGTH, result.overlay.blurStrength)
        assertEquals(0f, result.overlay.imageCenterX)
        assertEquals(1f, result.overlay.imageCenterY)
        assertEquals(OverlayCompiler.MAX_IMAGE_WIDTH_FRACTION, result.overlay.imageWidthFraction)
        assertEquals(OverlayCompiler.MIN_IMAGE_OPACITY, result.overlay.imageOpacity)
        assertEquals(EditorPreferencesSnapshot.MAX_PERSISTED_PREVIEW_SCALE, result.previewScale)
        assertEquals(0f, result.previewCenterX)
        assertEquals(1f, checkNotNull(result.previewCenterY))
    }

    @Test
    fun sanitize_clampsAudioGains() {
        val result = EditorPreferencesPolicy.sanitize(
            EditorPreferencesSnapshot(
                audio = AudioPreference(
                    enabled = true,
                    volume = -4f,
                    mixSourceVolume = 99f,
                    mixAddedVolume = -2f,
                ),
            ),
        )

        assertEquals(AudioCompiler.MIN_LINEAR_GAIN, result.audio.volume)
        assertEquals(AudioCompiler.MAX_LINEAR_GAIN, result.audio.mixSourceVolume)
        assertEquals(AudioCompiler.MIN_LINEAR_GAIN, result.audio.mixAddedVolume)
    }

    @Test
    fun defaultProductionRenderQualityIs1080p() {
        assertEquals(RenderPreset.FULL_HD_1080P, EditorPreferencesSnapshot().renderPreset)
    }
}
