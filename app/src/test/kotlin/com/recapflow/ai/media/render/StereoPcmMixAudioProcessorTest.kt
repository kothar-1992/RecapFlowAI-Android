package com.recapflow.ai.media.render

import kotlin.test.Test
import kotlin.test.assertEquals

class StereoPcmMixAudioProcessorTest {
    @Test
    fun monoIsDuplicatedAndAttenuated() {
        assertEquals(
            5_000.toShort() to 5_000.toShort(),
            StereoPcmMixAudioProcessor.mixFrameToStereo(shortArrayOf(10_000), 0.5f),
        )
    }

    @Test
    fun stereoChannelsRemainIndependent() {
        assertEquals(
            4_000.toShort() to (-2_000).toShort(),
            StereoPcmMixAudioProcessor.mixFrameToStereo(
                shortArrayOf(8_000, -4_000),
                0.5f,
            ),
        )
    }

    @Test
    fun multichannelInputFoldsToCenteredStereo() {
        assertEquals(
            2_500.toShort() to 2_500.toShort(),
            StereoPcmMixAudioProcessor.mixFrameToStereo(
                shortArrayOf(10_000, 0, 0, 10_000),
                0.5f,
            ),
        )
    }
}
