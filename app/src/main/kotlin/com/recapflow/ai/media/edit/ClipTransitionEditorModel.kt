package com.recapflow.ai.media.edit

/**
 * Pure editor state for choosing and configuring one reviewed adjacent clip boundary at a time.
 *
 * Boundary identity stays source-based so Trim / reviewed Adaptive Cut changes can safely prune
 * stale settings instead of reassigning a Crossfade to a different pair of clips.
 */
data class ClipTransitionEditorState(
    val selectedBoundaryIndex: Int = 0,
    val settings: ClipTransitionSettings = ClipTransitionSettings(),
)

object ClipTransitionEditorPolicy {

    fun reconcile(
        state: ClipTransitionEditorState,
        selectedRanges: List<TrimRange>,
    ): ClipTransitionEditorState {
        if (selectedRanges.size < 2) {
            return ClipTransitionEditorState()
        }
        val validKeys = (0 until selectedRanges.lastIndex)
            .mapTo(linkedSetOf()) { index ->
                selectedRanges[index].endMs to selectedRanges[index + 1].startMs
            }
        val boundaries = state.settings.boundaries
            .filter { boundary ->
                (boundary.leftSourceEndMs to boundary.rightSourceStartMs) in validKeys
            }
            .distinctBy { it.leftSourceEndMs to it.rightSourceStartMs }
        return state.copy(
            selectedBoundaryIndex = state.selectedBoundaryIndex.coerceIn(
                0,
                selectedRanges.lastIndex - 1,
            ),
            settings = state.settings.copy(
                enabled = boundaries.any { it.enabled },
                boundaries = boundaries,
            ),
        )
    }

    fun selectPrevious(
        state: ClipTransitionEditorState,
        selectedRanges: List<TrimRange>,
    ): ClipTransitionEditorState = reconcile(state, selectedRanges).let { reconciled ->
        reconciled.copy(
            selectedBoundaryIndex = (reconciled.selectedBoundaryIndex - 1).coerceAtLeast(0),
        )
    }

    fun selectNext(
        state: ClipTransitionEditorState,
        selectedRanges: List<TrimRange>,
    ): ClipTransitionEditorState = reconcile(state, selectedRanges).let { reconciled ->
        reconciled.copy(
            selectedBoundaryIndex = (reconciled.selectedBoundaryIndex + 1)
                .coerceAtMost((selectedRanges.size - 2).coerceAtLeast(0)),
        )
    }

    fun selectedBoundary(
        state: ClipTransitionEditorState,
        selectedRanges: List<TrimRange>,
    ): ClipTransitionBoundary? {
        val reconciled = reconcile(state, selectedRanges)
        val index = reconciled.selectedBoundaryIndex
        if (index !in 0 until selectedRanges.lastIndex) return null
        val left = selectedRanges[index]
        val right = selectedRanges[index + 1]
        return reconciled.settings.boundaries.firstOrNull { boundary ->
            boundary.leftSourceEndMs == left.endMs &&
                boundary.rightSourceStartMs == right.startMs
        }
    }

    fun selectedRangePair(
        state: ClipTransitionEditorState,
        selectedRanges: List<TrimRange>,
    ): Pair<TrimRange, TrimRange>? {
        val reconciled = reconcile(state, selectedRanges)
        val index = reconciled.selectedBoundaryIndex
        if (index !in 0 until selectedRanges.lastIndex) return null
        return selectedRanges[index] to selectedRanges[index + 1]
    }

    fun setSelectedEnabled(
        state: ClipTransitionEditorState,
        selectedRanges: List<TrimRange>,
        enabled: Boolean,
    ): ClipTransitionEditorState = updateSelected(state, selectedRanges) { boundary ->
        boundary.copy(enabled = enabled)
    }

    fun setSelectedDuration(
        state: ClipTransitionEditorState,
        selectedRanges: List<TrimRange>,
        durationMs: Long,
    ): ClipTransitionEditorState = updateSelected(state, selectedRanges) { boundary ->
        boundary.copy(
            durationMs = durationMs.coerceIn(
                ClipTransitionPolicy.MIN_DURATION_MS,
                ClipTransitionPolicy.MAX_DURATION_MS,
            ),
        )
    }

    fun setSelectedEasing(
        state: ClipTransitionEditorState,
        selectedRanges: List<TrimRange>,
        easing: ClipTransitionEasing,
    ): ClipTransitionEditorState = updateSelected(state, selectedRanges) { boundary ->
        boundary.copy(easing = easing)
    }

    /** Removes the selected semantic boundary entirely, restoring a true hard cut. */
    fun resetSelectedToHardCut(
        state: ClipTransitionEditorState,
        selectedRanges: List<TrimRange>,
    ): ClipTransitionEditorState {
        val reconciled = reconcile(state, selectedRanges)
        val pair = selectedRangePair(reconciled, selectedRanges) ?: return reconciled
        val key = pair.first.endMs to pair.second.startMs
        val boundaries = reconciled.settings.boundaries.filterNot { boundary ->
            (boundary.leftSourceEndMs to boundary.rightSourceStartMs) == key
        }
        return reconciled.copy(
            settings = reconciled.settings.copy(
                enabled = boundaries.any { it.enabled },
                boundaries = boundaries,
            ),
        )
    }

    private fun updateSelected(
        state: ClipTransitionEditorState,
        selectedRanges: List<TrimRange>,
        update: (ClipTransitionBoundary) -> ClipTransitionBoundary,
    ): ClipTransitionEditorState {
        val reconciled = reconcile(state, selectedRanges)
        val pair = selectedRangePair(reconciled, selectedRanges) ?: return reconciled
        val left = pair.first
        val right = pair.second
        val key = left.endMs to right.startMs
        val remembered = reconciled.settings.boundaries.firstOrNull { boundary ->
            (boundary.leftSourceEndMs to boundary.rightSourceStartMs) == key
        } ?: ClipTransitionBoundary(
            leftSourceEndMs = left.endMs,
            rightSourceStartMs = right.startMs,
            enabled = false,
        )
        val replacement = update(remembered)
        val boundaries = reconciled.settings.boundaries
            .filterNot { boundary ->
                (boundary.leftSourceEndMs to boundary.rightSourceStartMs) == key
            } + replacement
        return reconciled.copy(
            settings = reconciled.settings.copy(
                enabled = boundaries.any { it.enabled },
                boundaries = boundaries,
            ),
        )
    }
}
