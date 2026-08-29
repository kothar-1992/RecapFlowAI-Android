package com.recapflow.ai.media.render

import com.recapflow.ai.media.MediaInfo
import com.recapflow.ai.media.edit.AdaptiveCutCompiler
import com.recapflow.ai.media.edit.AudioCompiler
import com.recapflow.ai.media.edit.ClipTransitionPolicy
import com.recapflow.ai.media.edit.CompiledClipTransition
import com.recapflow.ai.media.edit.EditPlan
import com.recapflow.ai.media.edit.FreezeCompiler
import com.recapflow.ai.media.edit.ReplacementAudioAsset
import com.recapflow.ai.media.edit.TrimRange

/**
 * Media3-independent topology for one immutable [EditPlan].
 *
 * Preview and export can consume this same description without duplicating the decisions about
 * selected ranges, clip-boundary transitions, freeze placement, audio removal/replacement, or
 * expected duration. The actual Media3 objects are deliberately built by
 * [Media3CompositionCompiler].
 */
data class Media3CompositionPlan(
    val selectedRanges: List<TrimRange>,
    val clipTransitions: List<CompiledClipTransition>,
    val freeze: Media3FreezePlan?,
    val removeSourceAudio: Boolean,
    val sourceLinearGain: Float,
    val replacementAudio: ReplacementAudioAsset?,
    val replacementLinearGain: Float,
    val mixesSourceAudio: Boolean,
    val forceSourceAudioTrack: Boolean,
    val outputHasAudio: Boolean,
    val plannedDurationMs: Long,
) {
    val videoItemCount: Int
        get() = selectedRanges.size + if (freeze == null) 0 else 1

    val sequenceCount: Int
        get() = 1 + if (replacementAudio == null) 0 else 1

    val summary: String
        get() = "ranges=${selectedRanges.size}; clipTransitions=${clipTransitions.size}; " +
            "freeze=${freeze != null}; sequences=$sequenceCount; videoItems=$videoItemCount; " +
            "sourceAudio=${!removeSourceAudio}; replacementAudio=${replacementAudio != null}; " +
            "plannedDurationMs=$plannedDurationMs"
}

data class Media3FreezePlan(
    val durationMs: Long,
    val sourceFrameTimeMs: Long,
)

object Media3CompositionPlanCompiler {
    fun compile(mediaInfo: MediaInfo, editPlan: EditPlan): Media3CompositionPlan {
        val selectedRanges = AdaptiveCutCompiler.compile(
            editPlan.adaptiveCuts,
            editPlan.trimRange,
        ) ?: listOf(editPlan.trimRange)
        require(selectedRanges.isNotEmpty()) { "A Media3 composition requires a selected range" }

        val clipTransitions = ClipTransitionPolicy.compile(
            settings = editPlan.clipTransitions,
            selectedRanges = selectedRanges,
            transform = editPlan.transform,
        )
        val audio = AudioCompiler.compile(editPlan.audio)
        val removeSourceAudio = audio?.removeAudio == true
        val freeze = FreezeCompiler.compile(editPlan.transform)?.let {
            Media3FreezePlan(
                durationMs = it.durationMs,
                sourceFrameTimeMs = selectedRanges.first().startMs,
            )
        }

        return Media3CompositionPlan(
            selectedRanges = selectedRanges,
            clipTransitions = clipTransitions,
            freeze = freeze,
            removeSourceAudio = removeSourceAudio,
            sourceLinearGain = audio?.linearGain ?: AudioCompiler.UNITY_LINEAR_GAIN,
            replacementAudio = audio?.replacement,
            replacementLinearGain = audio?.replacementLinearGain
                ?: AudioCompiler.UNITY_LINEAR_GAIN,
            mixesSourceAudio = audio?.mixesSourceAudio == true,
            forceSourceAudioTrack = freeze != null && mediaInfo.hasAudio && !removeSourceAudio,
            outputHasAudio = (mediaInfo.hasAudio && !removeSourceAudio) || audio?.replacement != null,
            plannedDurationMs = editPlan.plannedDurationMs,
        )
    }
}
