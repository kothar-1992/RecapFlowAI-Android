package com.recapflow.ai.media.render

import android.opengl.Matrix
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.GlMatrixTransformation
import com.recapflow.ai.media.edit.CompiledZoom

/**
 * Applies a centered zoom while keeping the current frame canvas unchanged.
 * Values above 1 crop the outer edges; values below 1 reveal the canvas background.
 */
@UnstableApi
class ZoomMatrixTransformation(
    private val zoom: CompiledZoom,
    private val timelineOffsetUs: Long = 0L,
) : GlMatrixTransformation {

    private val matrix = FloatArray(MATRIX_SIZE)

    override fun configure(inputWidth: Int, inputHeight: Int): Size =
        Size(inputWidth, inputHeight)

    override fun getGlMatrixArray(presentationTimeUs: Long): FloatArray {
        val relativeTimeUs = (presentationTimeUs - timelineOffsetUs).coerceAtLeast(0L)
        val scale = zoom.scaleAt(relativeTimeUs)
        Matrix.setIdentityM(matrix, 0)
        Matrix.scaleM(matrix, 0, scale, scale, 1f)
        return matrix
    }

    private companion object {
        const val MATRIX_SIZE = 16
    }
}
