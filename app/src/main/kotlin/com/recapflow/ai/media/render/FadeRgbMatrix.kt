package com.recapflow.ai.media.render

import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbMatrix
import com.recapflow.ai.media.edit.CompiledTransition

/** Scales linear RGB channels toward black while preserving the opaque alpha channel. */
@UnstableApi
class FadeRgbMatrix(
    private val transition: CompiledTransition,
    private val timelineOffsetUs: Long = 0L,
) : RgbMatrix {

    private val matrix = FloatArray(MATRIX_SIZE)

    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
        val gain = transition.gainAt(
            (presentationTimeUs - timelineOffsetUs).coerceAtLeast(0L),
        )
        matrix.fill(0f)
        matrix[RED_SCALE] = gain
        matrix[GREEN_SCALE] = gain
        matrix[BLUE_SCALE] = gain
        matrix[ALPHA_SCALE] = 1f
        return matrix
    }

    private companion object {
        const val MATRIX_SIZE = 16
        const val RED_SCALE = 0
        const val GREEN_SCALE = 5
        const val BLUE_SCALE = 10
        const val ALPHA_SCALE = 15
    }
}
