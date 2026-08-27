package com.recapflow.ai.media.render

import kotlin.test.Test
import kotlin.test.assertEquals

class PcmVolumeAudioProcessorTest {
    @Test
    fun unityPreservesSignedPcmExtremes() {
        assertEquals(
            Short.MAX_VALUE,
            PcmVolumeAudioProcessor.scalePcm16Sample(Short.MAX_VALUE, 1f),
        )
        assertEquals(
            Short.MIN_VALUE,
            PcmVolumeAudioProcessor.scalePcm16Sample(Short.MIN_VALUE, 1f),
        )
    }

    @Test
    fun attenuationScalesPositiveAndNegativeSamples() {
        assertEquals(10_000.toShort(), PcmVolumeAudioProcessor.scalePcm16Sample(20_000, 0.5f))
        assertEquals((-10_000).toShort(), PcmVolumeAudioProcessor.scalePcm16Sample(-20_000, 0.5f))
    }

    @Test
    fun zeroGainProducesSilence() {
        assertEquals(0.toShort(), PcmVolumeAudioProcessor.scalePcm16Sample(Short.MAX_VALUE, 0f))
        assertEquals(0.toShort(), PcmVolumeAudioProcessor.scalePcm16Sample(Short.MIN_VALUE, 0f))
    }
}
