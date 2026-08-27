package com.recapflow.ai.media.render

import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.Crop
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.ScaleAndRotateTransformation
import com.recapflow.ai.media.edit.ColorCompiler
import com.recapflow.ai.media.edit.CropCompiler
import com.recapflow.ai.media.edit.MirrorCompiler
import com.recapflow.ai.media.edit.OverlayCompiler
import com.recapflow.ai.media.edit.OverlaySettings
import com.recapflow.ai.media.edit.ScaleMode
import com.recapflow.ai.media.edit.TransformCompiler
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TransitionCompiler
import com.recapflow.ai.media.edit.ZoomCompiler

/**
 * Builds the shared Transform video-effect chain used by both live preview and export.
 *
 * The visual operations always run in the same order: custom Crop, horizontal Mirror,
 * Color (Brightness -> Contrast -> Saturation -> Temperature), centered Zoom, then the optional
 * aspect-ratio Presentation. Export adds its baseline short-side scale and frame-rate
 * normalization after those operations; preview intentionally omits those export-only
 * steps so changing a control stays responsive on the device.
 */
@UnstableApi
object TransformVideoEffects {

    fun forPreview(
        settings: TransformSettings,
        sourceWidth: Int,
        sourceHeight: Int,
        overlays: OverlaySettings = OverlaySettings(),
        timelineOffsetUs: Long = 0L,
        sourceDurationMs: Long,
        sourceTimeOffsetUs: Long = 0L,
        fixedSourceTimeUs: Long? = null,
        realtimeSourceBlurState: RealtimeSourceBlurState? = null,
        realtimeImageOverlayState: RealtimeImageOverlayState? = null,
    ): List<Effect> = buildList {
        addAll(
            buildVisualEffects(
                settings = settings,
                shortSidePixels = PreviewGeometryPolicy.shortSidePixels(sourceWidth, sourceHeight),
                timelineOffsetUs = timelineOffsetUs,
                sourceDurationMs = sourceDurationMs,
            ),
        )
        val compiledBlur = OverlayCompiler.compile(overlays)
        realtimeSourceBlurState?.update(compiledBlur)
        compiledBlur?.let { blur ->
            add(
                SourceSubtitleBlurEffect(
                    blur = blur,
                    sourceTimeOffsetUs = sourceTimeOffsetUs,
                    fixedSourceTimeUs = fixedSourceTimeUs,
                    realtimeState = realtimeSourceBlurState,
                ),
            )
        }
        val compiledImage = OverlayCompiler.compileImage(overlays)
        realtimeImageOverlayState?.update(compiledImage)
        compiledImage?.let { image ->
            add(
                StaticImageOverlayEffect(
                    image = image,
                    sourceTimeOffsetUs = sourceTimeOffsetUs,
                    fixedSourceTimeUs = fixedSourceTimeUs,
                    realtimeState = realtimeImageOverlayState,
                ),
            )
        }
    }

    fun forRender(
        settings: TransformSettings,
        preset: RenderPreset,
        targetFrameRate: Float,
        sourceDurationMs: Long,
        speedEffect: Effect? = null,
        overlays: OverlaySettings = OverlaySettings(),
        sourceTimeOffsetUs: Long = 0L,
        fixedSourceTimeUs: Long? = null,
    ): List<Effect> = buildList {
        addAll(
            buildVisualEffects(
                settings = settings,
                shortSidePixels = preset.shortSidePixels,
                timelineOffsetUs = 0L,
                sourceDurationMs = sourceDurationMs,
            ),
        )
        if (TransformCompiler.compile(settings, preset) == null) {
            add(Presentation.createForShortSide(preset.shortSidePixels))
        }
        OverlayCompiler.compile(overlays)?.let { blur ->
            add(SourceSubtitleBlurEffect(blur, sourceTimeOffsetUs, fixedSourceTimeUs))
        }
        OverlayCompiler.compileImage(overlays)?.let { image ->
            add(StaticImageOverlayEffect(image, sourceTimeOffsetUs, fixedSourceTimeUs))
        }
        speedEffect?.let(::add)
        add(FrameDropEffect.createDefaultFrameDropEffect(targetFrameRate))
    }

    private fun buildVisualEffects(
        settings: TransformSettings,
        shortSidePixels: Int,
        timelineOffsetUs: Long,
        sourceDurationMs: Long,
    ): List<Effect> = buildList {
        if (!settings.enabled) return@buildList
        CropCompiler.compile(settings)?.let { crop ->
            add(
                Crop(
                    crop.leftNdc,
                    crop.rightNdc,
                    crop.bottomNdc,
                    crop.topNdc,
                ),
            )
        }
        MirrorCompiler.compile(settings)?.let { mirror ->
            add(
                ScaleAndRotateTransformation.Builder()
                    .setScale(mirror.scaleX, mirror.scaleY)
                    .build(),
            )
        }
        ColorCompiler.compile(settings)?.let { color ->
            if (color.brightness != 0f) add(Brightness(color.brightness))
            if (color.contrast != 0f) add(Contrast(color.contrast))
            if (color.saturationAdjustment != 0f) {
                add(
                    HslAdjustment.Builder()
                        .adjustSaturation(color.saturationAdjustment)
                        .build(),
                )
            }
            if (color.redScale != 1f || color.blueScale != 1f) {
                add(
                    RgbAdjustment.Builder()
                        .setRedScale(color.redScale)
                        .setBlueScale(color.blueScale)
                        .build(),
                )
            }
        }
        ZoomCompiler.compile(settings)?.let { zoom ->
            add(ZoomMatrixTransformation(zoom, timelineOffsetUs))
        }
        TransformCompiler.compile(settings, shortSidePixels)?.let { transform ->
            val layout = when (transform.scaleMode) {
                ScaleMode.FIT -> Presentation.LAYOUT_SCALE_TO_FIT
                ScaleMode.FILL -> Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            }
            add(
                Presentation.createForWidthAndHeight(
                    transform.targetWidth,
                    transform.targetHeight,
                    layout,
                ),
            )
        }
        TransitionCompiler.compile(settings, sourceDurationMs)?.let { transition ->
            add(FadeRgbMatrix(transition, timelineOffsetUs))
        }
    }
}
