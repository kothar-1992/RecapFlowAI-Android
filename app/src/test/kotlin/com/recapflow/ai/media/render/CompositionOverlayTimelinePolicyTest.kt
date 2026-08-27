package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.SpeedCompiler
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TrimRange
import kotlin.test.Test
import kotlin.test.assertEquals

class CompositionOverlayTimelinePolicyTest {
    @Test
    fun laterSequenceItemSubtractsItsCompositionOffset() {
        assertEquals(-120_000_000L, CompositionOverlayTimelinePolicy.localEffectTimeOffsetUs(120_000_000L))
    }

    @Test
    fun normalSpeedKeepsRangeDuration() {
        assertEquals(
            60_000_000L,
            CompositionOverlayTimelinePolicy.presentationDurationUs(
                TransformSettings(),
                TrimRange(180_000L, 240_000L),
            ),
        )
    }

    @Test
    fun speedAdjustedDurationMatchesEditPlanCompilerRule() {
        val settings = TransformSettings(enabled = true, speedEnabled = true, speed = 2f)
        val range = TrimRange(180_000L, 240_000L)
        assertEquals(
            SpeedCompiler.compile(settings)!!.outputDurationMs(range.durationMs) * 1_000L,
            CompositionOverlayTimelinePolicy.presentationDurationUs(settings, range),
        )
        assertEquals(30_000_000L, CompositionOverlayTimelinePolicy.presentationDurationUs(settings, range))
    }
}
