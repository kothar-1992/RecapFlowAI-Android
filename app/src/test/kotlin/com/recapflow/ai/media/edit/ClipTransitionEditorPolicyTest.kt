package com.recapflow.ai.media.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipTransitionEditorPolicyTest {

    private val ranges = listOf(
        TrimRange(0L, 4_000L),
        TrimRange(5_000L, 9_000L),
        TrimRange(10_000L, 14_000L),
    )

    @Test
    fun enablingSelectedBoundaryCreatesSemanticCrossfade() {
        val state = ClipTransitionEditorPolicy.setSelectedEnabled(
            state = ClipTransitionEditorState(),
            selectedRanges = ranges,
            enabled = true,
        )

        assertTrue(state.settings.enabled)
        assertEquals(1, state.settings.boundaries.size)
        assertEquals(4_000L, state.settings.boundaries.single().leftSourceEndMs)
        assertEquals(5_000L, state.settings.boundaries.single().rightSourceStartMs)
        assertEquals(ClipTransitionPolicy.DEFAULT_DURATION_MS, state.settings.boundaries.single().durationMs)
        assertTrue(state.settings.boundaries.single().enabled)
    }

    @Test
    fun nextBoundaryKeepsIndependentSettings() {
        var state = ClipTransitionEditorPolicy.setSelectedEnabled(
            ClipTransitionEditorState(),
            ranges,
            true,
        )
        state = ClipTransitionEditorPolicy.setSelectedDuration(state, ranges, 500L)
        state = ClipTransitionEditorPolicy.selectNext(state, ranges)
        state = ClipTransitionEditorPolicy.setSelectedEnabled(state, ranges, true)
        state = ClipTransitionEditorPolicy.setSelectedEasing(
            state,
            ranges,
            ClipTransitionEasing.LINEAR,
        )

        assertEquals(2, state.settings.boundaries.size)
        assertEquals(500L, state.settings.boundaries.first { it.leftSourceEndMs == 4_000L }.durationMs)
        assertEquals(
            ClipTransitionEasing.LINEAR,
            state.settings.boundaries.first { it.leftSourceEndMs == 9_000L }.easing,
        )
    }

    @Test
    fun disabledBoundaryIsRememberedButGlobalSettingsBecomeNoOp() {
        var state = ClipTransitionEditorPolicy.setSelectedDuration(
            ClipTransitionEditorState(),
            ranges,
            650L,
        )
        state = ClipTransitionEditorPolicy.setSelectedEnabled(state, ranges, false)

        assertFalse(state.settings.enabled)
        assertEquals(650L, state.settings.boundaries.single().durationMs)
        assertFalse(state.settings.boundaries.single().enabled)
    }

    @Test
    fun resetRemovesSelectedBoundaryAndRestoresHardCut() {
        var state = ClipTransitionEditorPolicy.setSelectedEnabled(
            ClipTransitionEditorState(),
            ranges,
            true,
        )
        state = ClipTransitionEditorPolicy.resetSelectedToHardCut(state, ranges)

        assertFalse(state.settings.enabled)
        assertTrue(state.settings.boundaries.isEmpty())
        assertNull(ClipTransitionEditorPolicy.selectedBoundary(state, ranges))
    }

    @Test
    fun reconcilePrunesBoundaryWhenReviewedClipsChange() {
        val state = ClipTransitionEditorPolicy.setSelectedEnabled(
            ClipTransitionEditorState(),
            ranges,
            true,
        )
        val changedRanges = listOf(
            TrimRange(0L, 3_500L),
            TrimRange(5_000L, 9_000L),
        )

        val reconciled = ClipTransitionEditorPolicy.reconcile(state, changedRanges)

        assertFalse(reconciled.settings.enabled)
        assertTrue(reconciled.settings.boundaries.isEmpty())
        assertEquals(0, reconciled.selectedBoundaryIndex)
    }

    @Test
    fun noBoundaryExistsForSingleSelectedClip() {
        val reconciled = ClipTransitionEditorPolicy.reconcile(
            ClipTransitionEditorState(selectedBoundaryIndex = 4),
            listOf(TrimRange(0L, 5_000L)),
        )

        assertEquals(ClipTransitionEditorState(), reconciled)
        assertNull(
            ClipTransitionEditorPolicy.selectedRangePair(
                reconciled,
                listOf(TrimRange(0L, 5_000L)),
            ),
        )
    }
}
