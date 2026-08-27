package com.recapflow.ai.media.render

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import com.recapflow.ai.media.edit.CompiledSourceSubtitleBlur

/**
 * Blurs only one user-positioned rectangle while leaving every pixel outside it unchanged.
 *
 * [sourceTimeOffsetUs] is retained for preview/legacy callers. Final clipped Media3 export now
 * projects absolute source windows into each item's local timeline and normally passes zero here,
 * avoiding source/composition timestamp double counting. [fixedSourceTimeUs] is used for an
 * exported intro-freeze image.
 */
@UnstableApi
class SourceSubtitleBlurEffect(
    private val blur: CompiledSourceSubtitleBlur,
    private val sourceTimeOffsetUs: Long = 0L,
    private val fixedSourceTimeUs: Long? = null,
    private val realtimeState: RealtimeSourceBlurState? = null,
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        SourceSubtitleBlurShaderProgram(
            context = context,
            useHdr = useHdr,
            blur = blur,
            sourceTimeOffsetUs = sourceTimeOffsetUs,
            fixedSourceTimeUs = fixedSourceTimeUs,
            realtimeState = realtimeState,
        )

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = false
}

@UnstableApi
private class SourceSubtitleBlurShaderProgram(
    context: Context,
    useHdr: Boolean,
    private val blur: CompiledSourceSubtitleBlur,
    private val sourceTimeOffsetUs: Long,
    private val fixedSourceTimeUs: Long?,
    private val realtimeState: RealtimeSourceBlurState?,
) : BaseGlShaderProgram(
    /* useHighPrecisionColorComponents = */ useHdr,
    /* texturePoolCapacity = */ 1,
) {

    private val glProgram: GlProgram = try {
        GlProgram(
            context,
            VERTEX_SHADER_ASSET_PATH,
            FRAGMENT_SHADER_ASSET_PATH,
        )
    } catch (error: Exception) {
        throw VideoFrameProcessingException(error)
    }

    private var inputWidth = 1
    private var inputHeight = 1

    init {
        try {
            glProgram.setBufferAttribute(
                "aFramePosition",
                GlUtil.getNormalizedCoordinateBounds(),
                GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
            )
        } catch (error: GlUtil.GlException) {
            throw VideoFrameProcessingException(error)
        }
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        this.inputWidth = inputWidth.coerceAtLeast(1)
        this.inputHeight = inputHeight.coerceAtLeast(1)
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        // PREVIEW_BLUR_LIVE_STATE: resolve the current rectangle, strength, and active time every
        // frame. A retained Media3 GL program therefore never requires a graph rebuild for bars.
        val activeBlur = realtimeState?.snapshot() ?: blur.takeIf { realtimeState == null }
        val layoutBlur = activeBlur ?: blur
        val sampling = SourceSubtitleBlurKernelPolicy.sampling(
            strength = layoutBlur.strength,
            inputWidth = inputWidth,
            inputHeight = inputHeight,
            rectangle = layoutBlur.rectangle,
        )
        val sourceTimeUs = fixedSourceTimeUs
            ?: (presentationTimeUs + sourceTimeOffsetUs).coerceAtLeast(0L)
        val blurEnabled = activeBlur?.isActiveAt(sourceTimeUs / 1_000L) == true
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            glProgram.setFloatUniform("uBlurLeft", layoutBlur.rectangle.left)
            glProgram.setFloatUniform("uBlurTop", layoutBlur.rectangle.top)
            glProgram.setFloatUniform("uBlurRight", layoutBlur.rectangle.right)
            glProgram.setFloatUniform("uBlurBottom", layoutBlur.rectangle.bottom)
            glProgram.setFloatUniform("uHorizontalStep", sampling.horizontalStep)
            glProgram.setFloatUniform("uVerticalStep", sampling.verticalStep)
            glProgram.setFloatUniform("uHorizontalFeather", sampling.horizontalFeather)
            glProgram.setFloatUniform("uVerticalFeather", sampling.verticalFeather)
            glProgram.setFloatUniform("uBlurEnabled", if (blurEnabled) 1f else 0f)
            glProgram.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        } catch (error: GlUtil.GlException) {
            throw VideoFrameProcessingException(error)
        }
    }

    override fun release() {
        super.release()
        try {
            glProgram.delete()
        } catch (error: GlUtil.GlException) {
            throw VideoFrameProcessingException(error)
        }
    }

    private companion object {
        const val VERTEX_SHADER_ASSET_PATH =
            "shaders/vertex_shader_source_subtitle_blur_es2.glsl"
        const val FRAGMENT_SHADER_ASSET_PATH =
            "shaders/fragment_shader_source_subtitle_blur_es2.glsl"
    }
}
