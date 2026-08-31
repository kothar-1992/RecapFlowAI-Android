package com.recapflow.ai.media.edit

import com.recapflow.ai.media.render.RenderPreset

data class EditPlan(
    val sourcePath: String,
    val sourceDurationMs: Long,
    val profile: EditProfile = EditProfile.NORMAL,
    val trimRange: TrimRange = TrimRange(0L, sourceDurationMs),
    val adaptiveCuts: AdaptiveCutSettings = AdaptiveCutSettings(),
    val transform: TransformSettings = TransformSettings(),
    val audio: AudioSettings = AudioSettings(),
    val overlays: OverlaySettings = OverlaySettings(),
    val subtitles: SubtitleSettings = SubtitleSettings(),
    val exportPreset: RenderPreset,
    val clipTransitions: ClipTransitionSettings = ClipTransitionSettings(),
) {
    val plannedDurationMs: Long
        get() {
            val selectedRanges = AdaptiveCutCompiler.compile(adaptiveCuts, trimRange)
                ?: listOf(trimRange)
            val selectedDurationMs = selectedRanges.sumOf { it.durationMs }
            val presentationDurationMs = SpeedCompiler.compile(transform)
                ?.outputDurationMs(selectedDurationMs)
                ?: selectedDurationMs
            val transitionOverlapMs = ClipTransitionPolicy.plannedOverlapDurationMs(
                settings = clipTransitions,
                selectedRanges = selectedRanges,
                transform = transform,
            )
            return presentationDurationMs - transitionOverlapMs +
                (FreezeCompiler.compile(transform)?.durationMs ?: 0L)
        }
}

enum class EditProfile {
    NORMAL,
    ADAPTIVE,
    CUSTOM,
}

data class TrimRange(
    val startMs: Long,
    val endMs: Long,
) {
    val durationMs: Long
        get() = endMs - startMs
}

data class AdaptiveCutSettings(
    val enabled: Boolean = false,
    val preset: AdaptiveCutPreset = AdaptiveCutPreset.BALANCED,
    val reviewedRanges: List<TrimRange> = emptyList(),
)

enum class AdaptiveCutPreset(
    val keepWindowMs: Long,
    val skipWindowMs: Long,
) {
    GENTLE(5_000L, 1_000L),
    BALANCED(4_000L, 1_000L),
    COMPACT(3_000L, 1_000L),
}

data class TransformSettings(
    val enabled: Boolean = false,
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.ORIGINAL,
    val scaleMode: ScaleMode = ScaleMode.FIT,
    val crop: CropSettings = CropSettings(),
    val zoom: ZoomSettings = ZoomSettings(),
    val mirrorEnabled: Boolean = false,
    val randomMirrorPerClipEnabled: Boolean = false,
    val color: ColorSettings = ColorSettings(),
    val freeze: FreezeSettings = FreezeSettings(),
    val speedEnabled: Boolean = false,
    val speed: Float = 1f,
    val transition: TransitionSettings = TransitionSettings(),
)

data class FreezeSettings(
    val enabled: Boolean = false,
    val durationMs: Long = FreezeCompiler.DEFAULT_DURATION_MS,
)

data class TransitionSettings(
    val enabled: Boolean = false,
    val mode: TransitionMode = TransitionMode.FADE_IN_OUT,
    val durationMs: Long = TransitionCompiler.DEFAULT_DURATION_MS,
)

data class ZoomSettings(
    val enabled: Boolean = false,
    val mode: ZoomMode = ZoomMode.IN,
)

data class ColorSettings(
    val enabled: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val temperature: Float = 0f,
) {
    fun isNeutral(): Boolean =
        brightness == 0f && contrast == 0f && saturation == 0f && temperature == 0f

    fun isValid(): Boolean =
        brightness in MIN_BRIGHTNESS..MAX_BRIGHTNESS &&
            contrast in MIN_CONTRAST..MAX_CONTRAST &&
            saturation in MIN_SATURATION..MAX_SATURATION &&
            temperature in MIN_TEMPERATURE..MAX_TEMPERATURE

    companion object {
        const val MIN_BRIGHTNESS = -50f
        const val MAX_BRIGHTNESS = 50f
        const val MIN_CONTRAST = -50f
        const val MAX_CONTRAST = 50f
        const val MIN_SATURATION = -100f
        const val MAX_SATURATION = 100f
        const val MIN_TEMPERATURE = -50f
        const val MAX_TEMPERATURE = 50f
    }
}

enum class AspectRatioPreset(
    val widthUnits: Int?,
    val heightUnits: Int?,
) {
    ORIGINAL(null, null),
    PORTRAIT_9_16(9, 16),
    LANDSCAPE_16_9(16, 9),
    SQUARE_1_1(1, 1),
}

enum class ScaleMode {
    FIT,
    FILL,
}

data class CropSettings(
    val enabled: Boolean = false,
    val rectangle: CropRectangle = CropRectangle(),
)

/**
 * Normalized source-frame coordinates measured from the top-left corner.
 * The remembered default trims ten percent from each edge when Crop is enabled.
 */
data class CropRectangle(
    val left: Float = 0.10f,
    val top: Float = 0.10f,
    val right: Float = 0.90f,
    val bottom: Float = 0.90f,
) {
    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top

    fun isValid(): Boolean =
        left in 0f..1f &&
            top in 0f..1f &&
            right in 0f..1f &&
            bottom in 0f..1f &&
            width >= MIN_CROP_SPAN &&
            height >= MIN_CROP_SPAN

    companion object {
        const val MIN_CROP_SPAN = 0.10f
    }
}

enum class ZoomMode {
    OFF,
    IN,
    OUT,
    ALTERNATE,
}

enum class TransitionMode {
    OFF,
    FADE_IN,
    FADE_OUT,
    FADE_IN_OUT,
}

data class AudioSettings(
    val enabled: Boolean = false,
    val policy: AudioPolicy = AudioPolicy.KEEP_ORIGINAL,
    val volume: Float = 1f,
    val mixVolume: Float = AudioCompiler.DEFAULT_MIX_LINEAR_GAIN,
    val replacement: ReplacementAudioAsset? = null,
)

/** App-private external audio prepared from Android's system document picker. */
data class ReplacementAudioAsset(
    val workingFilePath: String,
    val displayName: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
)

enum class AudioPolicy {
    KEEP_ORIGINAL,
    MUTE,
    REPLACE,
    MIX,
}

data class OverlaySettings(
    val enabled: Boolean = false,
    val sourceSubtitleBlur: SourceSubtitleBlurSettings = SourceSubtitleBlurSettings(),
    val image: ImageOverlaySettings = ImageOverlaySettings(),
)

/**
 * One manual blur region measured in normalized final-preview coordinates from the top-left.
 * The default targets the lower caption-safe area without assuming subtitle detection.
 */
data class SourceSubtitleBlurSettings(
    val enabled: Boolean = false,
    val rectangle: BlurRectangle = BlurRectangle(),
    val strength: Float = OverlayCompiler.DEFAULT_BLUR_STRENGTH,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
)

data class BlurRectangle(
    val left: Float = 0.10f,
    val top: Float = 0.76f,
    val right: Float = 0.90f,
    val bottom: Float = 0.94f,
) {
    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top

    fun isValid(): Boolean =
        left in 0f..1f &&
            top in 0f..1f &&
            right in 0f..1f &&
            bottom in 0f..1f &&
            width >= MIN_BLUR_SPAN &&
            height >= MIN_BLUR_SPAN

    companion object {
        const val MIN_BLUR_SPAN = 0.05f
    }
}

/** App-private still image prepared from Android's system document picker. */
data class ImageOverlayAsset(
    val workingFilePath: String,
    val displayName: String,
    val mimeType: String,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val fileSizeBytes: Long,
)

/**
 * One static image/logo overlay measured against the final video frame.
 * Position uses normalized top-left coordinates for the image center; [widthFraction]
 * is the requested fraction of the final frame width. Height preserves the source image ratio.
 */
data class ImageOverlaySettings(
    val enabled: Boolean = false,
    val asset: ImageOverlayAsset? = null,
    val centerX: Float = OverlayCompiler.DEFAULT_IMAGE_CENTER_X,
    val centerY: Float = OverlayCompiler.DEFAULT_IMAGE_CENTER_Y,
    val widthFraction: Float = OverlayCompiler.DEFAULT_IMAGE_WIDTH_FRACTION,
    val opacity: Float = OverlayCompiler.DEFAULT_IMAGE_OPACITY,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
)

enum class ImageOverlayPositionPreset(
    val centerX: Float,
    val centerY: Float,
) {
    TOP_LEFT(0.14f, 0.12f),
    TOP_RIGHT(0.86f, 0.12f),
    CENTER(0.50f, 0.50f),
    BOTTOM_LEFT(0.14f, 0.88f),
    BOTTOM_RIGHT(0.86f, 0.88f),
}

data class SubtitleSettings(
    val enabled: Boolean = false,
)
