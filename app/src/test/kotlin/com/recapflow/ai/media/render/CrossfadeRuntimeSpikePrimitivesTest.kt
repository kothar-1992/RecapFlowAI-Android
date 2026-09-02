package com.recapflow.ai.media.render

import androidx.media3.common.util.Size
import com.recapflow.ai.media.edit.ClipTransitionEasing
import com.recapflow.ai.media.edit.TrimRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CrossfadeRuntimeSpikePrimitivesTest {
    @Test
    fun pcmEnvelopeUsesSameComplementaryEaseInOutCurveAsVideo() {
        val slot = Media3CrossfadeClipSlot(
            rangeIndex = 1,
            lane = 1,
            sourceRange = TrimRange(5_000L, 9_000L),
            presentationStartUs = 3_700_000L,
            presentationDurationUs = 4_000_000L,
            fadeIn = Media3CrossfadeEnvelope(
                startUs = 3_700_000L,
                durationUs = 300_000L,
                easing = ClipTransitionEasing.EASE_IN_OUT,
            ),
            fadeOut = null,
        )
        val processor = CrossfadePcmAudioProcessor(slot)

        assertEquals(0f, processor.gainAtLocalPresentationTimeUs(0L))
        assertEquals(0.5f, processor.gainAtLocalPresentationTimeUs(150_000L), 0.0001f)
        assertEquals(1f, processor.gainAtLocalPresentationTimeUs(300_000L))
    }

    @Test
    fun pcmFadeOutComplementsIncomingEnvelope() {
        val slot = Media3CrossfadeClipSlot(
            rangeIndex = 0,
            lane = 0,
            sourceRange = TrimRange(0L, 4_000L),
            presentationStartUs = 0L,
            presentationDurationUs = 4_000_000L,
            fadeIn = null,
            fadeOut = Media3CrossfadeEnvelope(
                startUs = 3_700_000L,
                durationUs = 300_000L,
                easing = ClipTransitionEasing.EASE_IN_OUT,
            ),
        )
        val processor = CrossfadePcmAudioProcessor(slot)

        assertEquals(1f, processor.gainAtLocalPresentationTimeUs(3_700_000L))
        assertEquals(0.5f, processor.gainAtLocalPresentationTimeUs(3_850_000L), 0.0001f)
        assertEquals(0f, processor.gainAtLocalPresentationTimeUs(4_000_000L))
    }

    @Test
    fun compositorKeepsFullFrameGeometryAndRejectsUnexpectedThirdLane() {
        val topology = Media3CrossfadeTopology(
            slots = listOf(
                Media3CrossfadeClipSlot(
                    rangeIndex = 0,
                    lane = 0,
                    sourceRange = TrimRange(0L, 4_000L),
                    presentationStartUs = 0L,
                    presentationDurationUs = 4_000_000L,
                    fadeIn = null,
                    fadeOut = null,
                ),
                Media3CrossfadeClipSlot(
                    rangeIndex = 1,
                    lane = 1,
                    sourceRange = TrimRange(5_000L, 9_000L),
                    presentationStartUs = 3_700_000L,
                    presentationDurationUs = 4_000_000L,
                    fadeIn = Media3CrossfadeEnvelope(
                        startUs = 3_700_000L,
                        durationUs = 300_000L,
                        easing = ClipTransitionEasing.LINEAR,
                    ),
                    fadeOut = null,
                ),
            ),
            freezeDurationUs = 0L,
            totalDurationUs = 7_700_000L,
        )
        val compositor = Media3CrossfadeVideoCompositorSettings(topology)
        val size = Size(1080, 1920)

        assertEquals(size, compositor.getOutputSize(listOf(size, size)))
        assertEquals(
            0.5f,
            compositor.alphaForInput(1, 3_850_000L),
            0.0001f,
        )
        assertFailsWith<IllegalStateException> {
            compositor.alphaForInput(2, 3_850_000L)
        }
    }
}
