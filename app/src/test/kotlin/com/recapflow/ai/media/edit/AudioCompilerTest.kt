package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AudioCompilerTest {
    @Test
    fun disabledAudioIsNoOp() {
        assertNull(
            AudioCompiler.compile(
                AudioSettings(enabled = false, policy = AudioPolicy.MUTE),
            ),
        )
    }

    @Test
    fun keepOriginalIsNoOp() {
        assertNull(
            AudioCompiler.compile(
                AudioSettings(enabled = true, policy = AudioPolicy.KEEP_ORIGINAL),
            ),
        )
    }

    @Test
    fun attenuatedKeepOriginalCompilesConstantGain() {
        assertEquals(
            CompiledAudio(removeAudio = false, linearGain = 0.55f),
            AudioCompiler.compile(
                AudioSettings(
                    enabled = true,
                    policy = AudioPolicy.KEEP_ORIGINAL,
                    volume = 0.55f,
                ),
            ),
        )
    }

    @Test
    fun zeroVolumeKeepsAnAudioTrackInsteadOfCompilingMute() {
        assertEquals(
            CompiledAudio(removeAudio = false, linearGain = 0f),
            AudioCompiler.compile(
                AudioSettings(
                    enabled = true,
                    policy = AudioPolicy.KEEP_ORIGINAL,
                    volume = 0f,
                ),
            ),
        )
    }

    @Test
    fun muteRemovesAudio() {
        assertEquals(
            CompiledAudio(removeAudio = true, linearGain = 0f),
            AudioCompiler.compile(
                AudioSettings(enabled = true, policy = AudioPolicy.MUTE),
            ),
        )
    }

    @Test
    fun replaceRemovesSourceAndCarriesSelectedAssetAndGain() {
        val replacement = ReplacementAudioAsset(
            workingFilePath = "/private/music.m4a",
            displayName = "music.m4a",
            durationMs = 8_000L,
            fileSizeBytes = 256_000L,
        )

        assertEquals(
            CompiledAudio(
                removeAudio = true,
                linearGain = 0.65f,
                replacement = replacement,
                replacementLinearGain = 0.65f,
            ),
            AudioCompiler.compile(
                AudioSettings(
                    enabled = true,
                    policy = AudioPolicy.REPLACE,
                    volume = 0.65f,
                    replacement = replacement,
                ),
            ),
        )
    }

    @Test
    fun replaceWithoutAssetStillCompilesSourceRemovalForPreviewButValidatorOwnsTheGate() {
        assertEquals(
            CompiledAudio(removeAudio = true, linearGain = 1f),
            AudioCompiler.compile(
                AudioSettings(enabled = true, policy = AudioPolicy.REPLACE),
            ),
        )
    }

    @Test
    fun mixKeepsSourceAndCarriesIndependentTrackGains() {
        val addedAudio = ReplacementAudioAsset(
            workingFilePath = "/private/background.m4a",
            displayName = "background.m4a",
            durationMs = 5_000L,
            fileSizeBytes = 128_000L,
        )

        assertEquals(
            CompiledAudio(
                removeAudio = false,
                linearGain = 0.7f,
                replacement = addedAudio,
                replacementLinearGain = 0.3f,
                mixesSourceAudio = true,
            ),
            AudioCompiler.compile(
                AudioSettings(
                    enabled = true,
                    policy = AudioPolicy.MIX,
                    volume = 0.7f,
                    mixVolume = 0.3f,
                    replacement = addedAudio,
                ),
            ),
        )
    }
}
