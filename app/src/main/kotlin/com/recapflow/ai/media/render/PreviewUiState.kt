package com.recapflow.ai.media.render

/** User-visible preview capability is independent from the saved edit/render plan. */
sealed interface PreviewUiState {
    data object LiveEffects : PreviewUiState

    data class SourceOnly(
        val technicalReason: String,
    ) : PreviewUiState

    data class Unavailable(
        val technicalReason: String,
    ) : PreviewUiState
}
