package com.recapflow.ai.media.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import com.recapflow.ai.media.edit.CompiledImageOverlay
import com.recapflow.ai.media.edit.ImageOverlayLayoutPolicy
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.math.max

/**
 * Blends one static PNG/JPEG/WebP over the final visual frame.
 *
 * Position and width are normalized against the frame produced by the preceding Transform and
 * source-blur effects. Final clipped export projects overlay windows into each item's local time,
 * so [sourceTimeOffsetUs] is normally zero there; [fixedSourceTimeUs] keeps intro-freeze behavior
 * deterministic.
 */
@UnstableApi
class StaticImageOverlayEffect(
    private val image: CompiledImageOverlay,
    private val sourceTimeOffsetUs: Long = 0L,
    private val fixedSourceTimeUs: Long? = null,
    private val realtimeState: RealtimeImageOverlayState? = null,
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        StaticImageOverlayShaderProgram(
            context = context,
            useHdr = useHdr,
            image = image,
            sourceTimeOffsetUs = sourceTimeOffsetUs,
            fixedSourceTimeUs = fixedSourceTimeUs,
            realtimeState = realtimeState,
        )

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = false
}

@UnstableApi
private class StaticImageOverlayShaderProgram(
    context: Context,
    useHdr: Boolean,
    private val image: CompiledImageOverlay,
    private val sourceTimeOffsetUs: Long,
    private val fixedSourceTimeUs: Long?,
    private val realtimeState: RealtimeImageOverlayState?,
) : BaseGlShaderProgram(
    /* useHighPrecisionColorComponents = */ useHdr,
    /* texturePoolCapacity = */ 1,
) {

    private val glProgram: GlProgram = try {
        GlProgram(context, VERTEX_SHADER_ASSET_PATH, FRAGMENT_SHADER_ASSET_PATH)
    } catch (error: Exception) {
        throw VideoFrameProcessingException(error)
    }
    private val bitmap = try {
        ImageOverlayBitmapCache.load(image.asset.workingFilePath)
    } catch (error: Exception) {
        throw VideoFrameProcessingException(error)
    }
    private val overlayTextureId: Int = createOverlayTexture(bitmap)
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
        this.inputWidth = inputWidth
        this.inputHeight = inputHeight
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        // PREVIEW_LOGO_LIVE_STATE: an existing shader may survive setVideoEffects(). Resolve the
        // current immutable snapshot every frame so bar/preset changes cannot remain stale.
        val activeImage = realtimeState
            ?.snapshotFor(image.asset.workingFilePath)
            ?: image.takeIf { realtimeState == null }
        val layoutImage = activeImage ?: image
        val bounds = ImageOverlayLayoutPolicy.resolve(
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            frameWidth = inputWidth,
            frameHeight = inputHeight,
            centerX = layoutImage.centerX,
            centerY = layoutImage.centerY,
            requestedWidthFraction = layoutImage.widthFraction,
        )
        val sourceTimeUs = fixedSourceTimeUs
            ?: (presentationTimeUs + sourceTimeOffsetUs).coerceAtLeast(0L)
        val enabled = activeImage?.isActiveAt(sourceTimeUs / 1_000L) == true
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            glProgram.setSamplerTexIdUniform("uOverlaySampler", overlayTextureId, 1)
            glProgram.setFloatUniform("uOverlayLeft", bounds.left)
            glProgram.setFloatUniform("uOverlayTop", bounds.top)
            glProgram.setFloatUniform("uOverlayRight", bounds.right)
            glProgram.setFloatUniform("uOverlayBottom", bounds.bottom)
            glProgram.setFloatUniform("uOverlayOpacity", activeImage?.opacity ?: 0f)
            glProgram.setFloatUniform("uOverlayEnabled", if (enabled) 1f else 0f)
            glProgram.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        } catch (error: GlUtil.GlException) {
            throw VideoFrameProcessingException(error)
        }
    }

    override fun release() {
        super.release()
        GLES20.glDeleteTextures(1, intArrayOf(overlayTextureId), 0)
        try {
            glProgram.delete()
        } catch (error: GlUtil.GlException) {
            throw VideoFrameProcessingException(error)
        }
    }

    private fun createOverlayTexture(bitmap: Bitmap): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]
        if (textureId == 0) {
            throw VideoFrameProcessingException(IOException("Could not allocate logo texture"))
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            GLES20.glDeleteTextures(1, textureIds, 0)
            throw VideoFrameProcessingException(
                IOException("Could not upload logo texture; GL error 0x${error.toString(16)}"),
            )
        }
        return textureId
    }

    private companion object {
        const val VERTEX_SHADER_ASSET_PATH =
            "shaders/vertex_shader_static_image_overlay_es2.glsl"
        const val FRAGMENT_SHADER_ASSET_PATH =
            "shaders/fragment_shader_static_image_overlay_es2.glsl"
    }
}

private object ImageOverlayBitmapCache {
    private var cachedPath: String? = null
    private var cachedModifiedMs = -1L
    private var cachedBitmap: WeakReference<Bitmap>? = null

    @Synchronized
    fun load(path: String): Bitmap {
        val file = File(path)
        if (!file.isFile || file.length() <= 0L) {
            throw IOException("The selected image overlay is missing")
        }
        val modifiedMs = file.lastModified()
        cachedBitmap?.get()?.takeIf {
            cachedPath == file.absolutePath && cachedModifiedMs == modifiedMs && !it.isRecycled
        }?.let { return it }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("The selected image overlay could not be decoded")
        }
        var sampleSize = 1
        while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > MAX_BITMAP_SIDE) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                inPremultiplied = false
            }
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw IOException("The selected image overlay could not be decoded")
        cachedPath = file.absolutePath
        cachedModifiedMs = modifiedMs
        cachedBitmap = WeakReference(bitmap)
        return bitmap
    }

    private const val MAX_BITMAP_SIDE = 2_048
}
