package com.recapflow.ai.media.render

sealed interface RenderUiState {
    data class Idle(
        val capability: RenderCapability,
    ) : RenderUiState

    data class Preparing(
        val preset: RenderPreset,
        val outputPath: String,
        val elapsedMs: Long = 0L,
    ) : RenderUiState

    data class Rendering(
        val preset: RenderPreset,
        val outputPath: String,
        val progressPercent: Int?,
        val elapsedMs: Long,
    ) : RenderUiState

    data class Finalizing(
        val preset: RenderPreset,
        val outputPath: String,
        val elapsedMs: Long,
    ) : RenderUiState

    data class Completed(
        val preset: RenderPreset,
        val outputPath: String,
        val elapsedMs: Long,
        val sourceDurationMs: Long,
        val plannedDurationMs: Long,
        val outputSizeBytes: Long,
        val videoEncoderName: String?,
        val requestedVideoBitrate: Int,
        val averageVideoBitrate: Int?,
        val sourceShortSidePixels: Int,
        val sourceWasUpscaled: Boolean,
        val sourceWasPreviousRecapFlowExport: Boolean,
        val outputWidth: Int,
        val outputHeight: Int,
        val outputDurationMs: Long,
        val outputHasAudio: Boolean,
        val validationWarnings: List<String> = emptyList(),
    ) : RenderUiState {
        val realtimeFactor: Double?
            get() = if (plannedDurationMs > 0L) {
                elapsedMs.toDouble() / plannedDurationMs.toDouble()
            } else {
                null
            }
    }

    data class Failed(
        val preset: RenderPreset,
        val message: String,
        val diagnostics: String,
        val elapsedMs: Long,
    ) : RenderUiState

    data class Cancelled(
        val preset: RenderPreset,
        val elapsedMs: Long,
    ) : RenderUiState
}

data class RenderCapability(
    val available: Boolean,
    val videoEncoderName: String? = null,
    val audioEncoderName: String? = null,
    val reason: String? = null,
)
