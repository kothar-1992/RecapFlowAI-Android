package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MirrorCompilerTest {

    @Test
    fun transformOffOmitsRememberedMirror() {
        val settings = TransformSettings(
            enabled = false,
            mirrorEnabled = true,
        )

        assertNull(MirrorCompiler.compile(settings))
    }

    @Test
    fun mirrorOffIsAnExplicitNoOp() {
        val settings = TransformSettings(
            enabled = true,
            mirrorEnabled = false,
        )

        assertNull(MirrorCompiler.compile(settings))
    }

    @Test
    fun mirrorOnCompilesHorizontalAxisFlip() {
        val settings = TransformSettings(
            enabled = true,
            mirrorEnabled = true,
        )

        assertEquals(
            CompiledMirror(scaleX = -1f, scaleY = 1f),
            MirrorCompiler.compile(settings),
        )
    }
}
