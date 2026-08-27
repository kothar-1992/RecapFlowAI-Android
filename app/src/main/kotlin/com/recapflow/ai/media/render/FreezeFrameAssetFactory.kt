package com.recapflow.ai.media.render

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import com.recapflow.ai.media.MediaInfo
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

/** Extracts one bounded-size source frame for the intro-freeze composition item. */
object FreezeFrameAssetFactory {
    fun create(
        context: Context,
        mediaInfo: MediaInfo,
        positionMs: Long,
        preset: RenderPreset,
    ): File {
        val directory = File(context.cacheDir, "freeze_frames")
        check(directory.exists() || directory.mkdirs()) {
            "Could not create the freeze-frame cache directory"
        }
        val output = File(directory, "freeze_${System.currentTimeMillis()}.jpg")
        val retriever = MediaMetadataRetriever()
        var bitmap: Bitmap? = null
        try {
            retriever.setDataSource(mediaInfo.workingFilePath)
            val target = targetSize(mediaInfo, preset)
            bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    positionMs * 1_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                    target.first,
                    target.second,
                )
            } else {
                retriever.getFrameAtTime(
                    positionMs * 1_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                )?.let { fullSize ->
                    if (fullSize.width == target.first && fullSize.height == target.second) {
                        fullSize
                    } else {
                        Bitmap.createScaledBitmap(fullSize, target.first, target.second, true)
                            .also { fullSize.recycle() }
                    }
                }
            }
            val frame = checkNotNull(bitmap) { "Could not decode the selected freeze frame" }
            FileOutputStream(output).use { stream ->
                check(frame.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                    "Could not encode the selected freeze frame"
                }
            }
            check(output.isFile && output.length() > 0L) {
                "Freeze-frame image is empty"
            }
            return output
        } catch (error: Throwable) {
            output.delete()
            throw error
        } finally {
            bitmap?.recycle()
            retriever.release()
        }
    }

    private fun targetSize(mediaInfo: MediaInfo, preset: RenderPreset): Pair<Int, Int> {
        val sourceWidth = mediaInfo.width.coerceAtLeast(1)
        val sourceHeight = mediaInfo.height.coerceAtLeast(1)
        val sourceShortSide = min(sourceWidth, sourceHeight).coerceAtLeast(1)
        val scale = min(1f, preset.shortSidePixels.toFloat() / sourceShortSide.toFloat())
        return Pair(
            (sourceWidth * scale).roundToInt().coerceAtLeast(1),
            (sourceHeight * scale).roundToInt().coerceAtLeast(1),
        )
    }

    private const val JPEG_QUALITY = 95
}
