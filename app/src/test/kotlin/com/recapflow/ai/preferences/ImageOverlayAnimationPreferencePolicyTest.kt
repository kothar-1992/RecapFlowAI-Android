package com.recapflow.ai.preferences

import com.recapflow.ai.media.edit.ImageOverlayAnimationPolicy
import com.recapflow.ai.media.edit.ImageOverlayAnimationPreset
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageOverlayAnimationPreferencePolicyTest {

    @Test
    fun oldPreferenceDefaultsRemainStatic() {
        val overlay = EditorPreferencesSnapshot().overlay

        assertEquals(ImageOverlayAnimationPreset.NONE, overlay.imageAnimationPreset)
        assertEquals(false, overlay.imageAnimationLoopEnabled)
        assertEquals(ImageOverlayAnimationPolicy.DEFAULT_DURATION_MS, overlay.imageAnimationDurationMs)
        assertEquals(ImageOverlayAnimationPolicy.DEFAULT_PERIOD_MS, overlay.imageAnimationPeriodMs)
    }

    @Test
    fun sanitizeClampsDurationAndKeepsPeriodAtLeastDuration() {
        val result = EditorPreferencesPolicy.sanitize(
            EditorPreferencesSnapshot(
                overlay = OverlayPreference(
                    imageAnimationPreset = ImageOverlayAnimationPreset.PULSE,
                    imageAnimationLoopEnabled = true,
                    imageAnimationDurationMs = ImageOverlayAnimationPolicy.MAX_DURATION_MS + 5_000L,
                    imageAnimationPeriodMs = 100L,
                ),
            ),
        ).overlay

        assertEquals(ImageOverlayAnimationPreset.PULSE, result.imageAnimationPreset)
        assertEquals(true, result.imageAnimationLoopEnabled)
        assertEquals(ImageOverlayAnimationPolicy.MAX_DURATION_MS, result.imageAnimationDurationMs)
        assertEquals(ImageOverlayAnimationPolicy.MAX_DURATION_MS, result.imageAnimationPeriodMs)
    }
}
