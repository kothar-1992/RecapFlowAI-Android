package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.BlurRectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceSubtitleBlurKernelPolicyTest {

    @Test
    fun strengthThirtyMatchesBoundedServerRadiusContract() {
        assertEquals(15f, SourceSubtitleBlurKernelPolicy.radiusPixelsAtReference(30f))
        assertEquals(16f, SourceSubtitleBlurKernelPolicy.radiusPixelsAtReference(32f))
        assertEquals(2f, SourceSubtitleBlurKernelPolicy.radiusPixelsAtReference(4f))
    }

    @Test
    fun sameAspectKeepsNormalizedKernelAcross720And1080() {
        val rectangle = BlurRectangle(left = 0.10f, top = 0.63f, right = 0.91f, bottom = 0.76f)
        val at720 = SourceSubtitleBlurKernelPolicy.sampling(30f, 720, 1280, rectangle)
        val at1080 = SourceSubtitleBlurKernelPolicy.sampling(30f, 1080, 1920, rectangle)

        assertEquals(at720.horizontalStep, at1080.horizontalStep, 0.000001f)
        assertEquals(at720.verticalStep, at1080.verticalStep, 0.000001f)
        assertEquals(15f, at720.radiusPixels, 0.0001f)
        assertEquals(22.5f, at1080.radiusPixels, 0.0001f)
    }

    @Test
    fun thinRegionCapsKernelBeforeSamplingOutsideItsBounds() {
        val thin = BlurRectangle(left = 0.10f, top = 0.70f, right = 0.90f, bottom = 0.71f)
        val sampling = SourceSubtitleBlurKernelPolicy.sampling(32f, 720, 1280, thin)

        assertTrue(sampling.radiusPixels < 16f)
        assertTrue(sampling.horizontalStep > 0f)
        assertTrue(sampling.verticalStep > 0f)
    }
}
