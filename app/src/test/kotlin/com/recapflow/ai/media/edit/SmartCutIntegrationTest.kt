package com.recapflow.ai.media.edit

import com.recapflow.ai.media.render.RenderPreset
import kotlin.test.*

class SmartCutIntegrationTest {
    private fun plan() = EditPlan("source.mp4", 20_000, exportPreset = RenderPreset.HD_720P)
    private fun draft(plan: EditPlan) = SmartCutPlanner.draft("v1", plan.trimRange,
        AdaptiveCutCompiler.compile(plan.adaptiveCuts, plan.trimRange) ?: listOf(plan.trimRange),
        listOf(SmartCutCandidate("pause", TrimRange(3_000, 6_000), SmartCutReason.PAUSE, 0.99, "pause")))

    @Test fun appliesToCanonicalPlanWithSpeedFreezeAndUndo() {
        val before = plan().copy(transform = TransformSettings(enabled = true, speedEnabled = true, speed = 2f,
            freeze = FreezeSettings(enabled = true, durationMs = 2_000)))
        val result = assertNotNull(SmartCutIntegration.apply(before, before, "v1", draft(before)))
        assertEquals(ClipPlanningMode.AI_SMART_CUTS, result.after.adaptiveCuts.mode)
        assertEquals(EditProfile.ADAPTIVE, result.after.profile)
        assertNull(result.after.adaptiveCuts.targetDurationMs)
        assertEquals(10_650L, result.after.plannedDurationMs)
        assertEquals(before.audio, result.after.audio)
        assertEquals(before.overlays, result.after.overlays)
        assertEquals(before, result.undo(result.after))
        assertNull(result.undo(result.after.copy(exportPreset = RenderPreset.FULL_HD_1080P)))
    }

    @Test fun changedTimingOrMediaRevisionRejectsApply() {
        val before = plan()
        assertNull(SmartCutIntegration.apply(before.copy(transform = TransformSettings(enabled = true)), before, "v1", draft(before)))
        assertNull(SmartCutIntegration.apply(before, before, "v2", draft(before)))
    }

    @Test fun transitionFollowsRetainedEndpointsInsteadOfListIndex() {
        val selected = listOf(TrimRange(0, 8_000), TrimRange(10_000, 20_000))
        val boundary = ClipTransitionBoundary(8_000, 10_000, durationMs = 300)
        val before = plan().copy(adaptiveCuts = AdaptiveCutSettings(enabled = true, reviewedRanges = selected),
            clipTransitions = ClipTransitionSettings(enabled = true, boundaries = listOf(boundary)))
        val result = assertNotNull(SmartCutIntegration.apply(before, before, "v1", draft(before)))
        assertEquals(listOf(boundary), result.after.clipTransitions.boundaries)
        val compiled = ClipTransitionPolicy.compile(result.after.clipTransitions, result.after.adaptiveCuts.reviewedRanges, result.after.transform)
        assertEquals(1, compiled.single().boundaryIndex)
        assertEquals(15_000L, result.after.plannedDurationMs)
    }

    @Test fun forgedKeepRangesCannotRestorePreviouslyExcludedSource() {
        val before = plan().copy(adaptiveCuts = AdaptiveCutSettings(enabled = true,
            reviewedRanges = listOf(TrimRange(0, 8_000), TrimRange(10_000, 20_000))))
        val forged = draft(before).copy(keptRanges = listOf(TrimRange(0, 20_000)))
        assertNull(SmartCutIntegration.apply(before, before, "v1", forged))
    }
}
