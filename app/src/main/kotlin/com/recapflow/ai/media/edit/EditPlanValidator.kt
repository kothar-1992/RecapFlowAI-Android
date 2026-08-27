package com.recapflow.ai.media.edit

object EditPlanValidator {
    const val MIN_TRIM_DURATION_MS = 1_000L

    fun validate(plan: EditPlan): List<EditPlanIssue> = buildList {
        if (plan.sourcePath.isBlank()) {
            add(EditPlanIssue.SOURCE_PATH_MISSING)
        }
        if (plan.sourceDurationMs <= 0L) {
            add(EditPlanIssue.SOURCE_DURATION_INVALID)
        }
        if (plan.trimRange.startMs < 0L) {
            add(EditPlanIssue.TRIM_START_BEFORE_SOURCE)
        }
        if (plan.trimRange.endMs > plan.sourceDurationMs) {
            add(EditPlanIssue.TRIM_END_AFTER_SOURCE)
        }
        if (plan.trimRange.durationMs < MIN_TRIM_DURATION_MS) {
            add(EditPlanIssue.TRIM_TOO_SHORT)
        }
        if (plan.adaptiveCuts.enabled) {
            when {
                plan.adaptiveCuts.reviewedRanges.isEmpty() -> {
                    add(EditPlanIssue.ADAPTIVE_RANGES_MISSING)
                }
                plan.adaptiveCuts.reviewedRanges.size > AdaptiveCutCompiler.MAX_REVIEWED_RANGES ||
                    !AdaptiveCutCompiler.areRangesValid(
                        plan.adaptiveCuts.reviewedRanges,
                        plan.trimRange,
                    ) -> {
                    add(EditPlanIssue.ADAPTIVE_RANGES_INVALID)
                }
            }
        }
        if (
            plan.transform.enabled &&
            plan.transform.speedEnabled &&
            plan.transform.speed !in SpeedCompiler.MIN_SPEED..SpeedCompiler.MAX_SPEED
        ) {
            add(EditPlanIssue.SPEED_INVALID)
        }
        if (
            plan.transform.enabled &&
            plan.transform.freeze.enabled &&
            plan.transform.freeze.durationMs !in FreezeCompiler.supportedDurationsMs
        ) {
            add(EditPlanIssue.FREEZE_DURATION_INVALID)
        }
        if (
            plan.transform.enabled &&
            plan.transform.transition.enabled &&
            plan.transform.transition.mode != TransitionMode.OFF
        ) {
            if (plan.transform.transition.durationMs !in TransitionCompiler.supportedDurationsMs) {
                add(EditPlanIssue.TRANSITION_DURATION_INVALID)
            } else {
                val requiredDurationMs = plan.transform.transition.durationMs *
                    if (plan.transform.transition.mode == TransitionMode.FADE_IN_OUT) 2L else 1L
                val selectedRanges = AdaptiveCutCompiler.compile(
                    plan.adaptiveCuts,
                    plan.trimRange,
                ) ?: listOf(plan.trimRange)
                val speed = SpeedCompiler.compile(plan.transform)
                val hasTooShortRange = selectedRanges.any { range ->
                    val contentDurationMs = speed?.outputDurationMs(range.durationMs)
                        ?: range.durationMs
                    contentDurationMs < requiredDurationMs
                }
                if (hasTooShortRange) {
                    add(EditPlanIssue.TRANSITION_TOO_LONG)
                }
            }
        }
        if (
            plan.transform.enabled &&
            plan.transform.crop.enabled &&
            !plan.transform.crop.rectangle.isValid()
        ) {
            add(EditPlanIssue.CROP_RECTANGLE_INVALID)
        }
        if (
            plan.transform.enabled &&
            plan.transform.color.enabled &&
            !plan.transform.color.isValid()
        ) {
            add(EditPlanIssue.COLOR_SETTINGS_INVALID)
        }
        if (
            plan.audio.enabled &&
            plan.audio.volume !in AudioCompiler.MIN_LINEAR_GAIN..AudioCompiler.MAX_LINEAR_GAIN
        ) {
            add(EditPlanIssue.AUDIO_VOLUME_INVALID)
        }
        if (
            plan.audio.enabled &&
            plan.audio.policy == AudioPolicy.MIX &&
            plan.audio.mixVolume !in AudioCompiler.MIN_LINEAR_GAIN..AudioCompiler.MAX_LINEAR_GAIN
        ) {
            add(EditPlanIssue.MIX_AUDIO_VOLUME_INVALID)
        }
        if (
            plan.audio.enabled &&
            plan.audio.policy in setOf(AudioPolicy.REPLACE, AudioPolicy.MIX) &&
            plan.audio.replacement == null
        ) {
            add(EditPlanIssue.EXTERNAL_AUDIO_MISSING)
        }
        if (
            plan.audio.enabled &&
            plan.audio.policy in setOf(AudioPolicy.REPLACE, AudioPolicy.MIX) &&
            plan.audio.replacement?.let { asset ->
                asset.workingFilePath.isBlank() ||
                    asset.displayName.isBlank() ||
                    asset.durationMs <= 0L ||
                    asset.fileSizeBytes <= 0L
            } == true
        ) {
            add(EditPlanIssue.REPLACEMENT_AUDIO_INVALID)
        }
        if (
            plan.audio.enabled &&
            plan.audio.policy !in setOf(
                AudioPolicy.KEEP_ORIGINAL,
                AudioPolicy.MUTE,
                AudioPolicy.REPLACE,
                AudioPolicy.MIX,
            )
        ) {
            add(EditPlanIssue.AUDIO_POLICY_UNSUPPORTED)
        }
        OverlayCompiler.compile(plan.overlays)?.let { blur ->
            if (!blur.rectangle.isValid()) {
                add(EditPlanIssue.SOURCE_BLUR_RECTANGLE_INVALID)
            }
            if (blur.strength !in OverlayCompiler.MIN_BLUR_STRENGTH..OverlayCompiler.MAX_BLUR_STRENGTH) {
                add(EditPlanIssue.SOURCE_BLUR_STRENGTH_INVALID)
            }
            if (
                blur.startMs < 0L ||
                blur.endMs > plan.sourceDurationMs ||
                blur.endMs - blur.startMs < OverlayCompiler.MIN_BLUR_DURATION_MS
            ) {
                add(EditPlanIssue.SOURCE_BLUR_TIME_RANGE_INVALID)
            }
        }
        if (
            plan.overlays.enabled &&
            plan.overlays.image.enabled &&
            plan.overlays.image.asset == null
        ) {
            add(EditPlanIssue.IMAGE_OVERLAY_ASSET_INVALID)
        }
        OverlayCompiler.compileImage(plan.overlays)?.let { image ->
            if (
                image.asset.workingFilePath.isBlank() ||
                image.asset.displayName.isBlank() ||
                image.asset.mimeType !in SUPPORTED_IMAGE_MIME_TYPES ||
                image.asset.pixelWidth <= 0 ||
                image.asset.pixelHeight <= 0 ||
                image.asset.fileSizeBytes <= 0L
            ) {
                add(EditPlanIssue.IMAGE_OVERLAY_ASSET_INVALID)
            }
            if (
                image.centerX !in 0f..1f ||
                image.centerY !in 0f..1f ||
                image.widthFraction !in
                    OverlayCompiler.MIN_IMAGE_WIDTH_FRACTION..OverlayCompiler.MAX_IMAGE_WIDTH_FRACTION ||
                image.opacity !in
                    OverlayCompiler.MIN_IMAGE_OPACITY..OverlayCompiler.MAX_IMAGE_OPACITY
            ) {
                add(EditPlanIssue.IMAGE_OVERLAY_GEOMETRY_INVALID)
            }
            if (
                image.startMs < 0L ||
                image.endMs > plan.sourceDurationMs ||
                image.endMs - image.startMs < OverlayCompiler.MIN_IMAGE_DURATION_MS
            ) {
                add(EditPlanIssue.IMAGE_OVERLAY_TIME_RANGE_INVALID)
            }
        }
    }

    private val SUPPORTED_IMAGE_MIME_TYPES = setOf(
        "image/png",
        "image/jpeg",
        "image/webp",
    )
}

enum class EditPlanIssue(val description: String) {
    SOURCE_PATH_MISSING("Source path is missing"),
    SOURCE_DURATION_INVALID("Source duration is invalid"),
    TRIM_START_BEFORE_SOURCE("Trim start is before the source"),
    TRIM_END_AFTER_SOURCE("Trim end is after the source"),
    TRIM_TOO_SHORT("Select at least one second"),
    ADAPTIVE_RANGES_MISSING("Generate and review cut ranges before applying Adaptive Cuts"),
    ADAPTIVE_RANGES_INVALID("Adaptive cut ranges must be ordered and remain inside Trim"),
    SPEED_INVALID("Speed must be between 0.5× and 2×"),
    FREEZE_DURATION_INVALID("Freeze duration must be 1, 2, or 3 seconds"),
    TRANSITION_DURATION_INVALID("Transition duration must be 0.5, 1, or 1.5 seconds"),
    TRANSITION_TOO_LONG("The selected clip is too short for this transition"),
    CROP_RECTANGLE_INVALID("Crop edges must leave at least ten percent of the frame"),
    COLOR_SETTINGS_INVALID("Color adjustments are outside the supported range"),
    AUDIO_VOLUME_INVALID("Audio volume must be between 0% and 100%"),
    MIX_AUDIO_VOLUME_INVALID("Added audio volume must be between 0% and 100%"),
    EXTERNAL_AUDIO_MISSING("Choose an external audio file before rendering"),
    REPLACEMENT_AUDIO_INVALID("The selected external audio file is invalid"),
    AUDIO_POLICY_UNSUPPORTED("The selected audio policy is unavailable"),
    SOURCE_BLUR_RECTANGLE_INVALID("Blur area must stay inside the video and retain at least five percent width and height"),
    SOURCE_BLUR_STRENGTH_INVALID("Blur strength must be between 4 and 32"),
    SOURCE_BLUR_TIME_RANGE_INVALID("Blur time range must stay inside the source and last at least 0.25 seconds"),
    IMAGE_OVERLAY_ASSET_INVALID("Choose a valid PNG, JPEG, or WebP image before rendering"),
    IMAGE_OVERLAY_GEOMETRY_INVALID("Image overlay position, size, or opacity is outside the supported range"),
    IMAGE_OVERLAY_TIME_RANGE_INVALID("Image overlay time range must stay inside the source and last at least 0.25 seconds"),
}
