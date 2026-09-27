package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TargetDurationReachabilityTest {
    private val source = TrimRange(0L, 124_000L) // 02:04, the source from the device report

    @Test
    fun neutralSpeedReachesATargetBelowTheSourceLength() {
        assertNull(
            TargetDurationClipPlanner.unreachableReason(
                sourceRange = source,
                targetDurationMs = 99_000L,
            ),
        )
    }

    @Test
    fun speedingUpMakesTheSameTargetUnreachable() {
        val reason = TargetDurationClipPlanner.unreachableReason(
            sourceRange = source,
            targetDurationMs = 99_000L,
            transform = transformWithSpeed(2f),
        )

        assertEquals(TargetDurationUnreachableReason.SPEED_NEEDS_MORE_SOURCE, reason)
    }

    @Test
    fun theReasonIsSpecificToSpeedingUpNotSlowingDown() {
        assertNull(
            TargetDurationClipPlanner.unreachableReason(
                sourceRange = source,
                targetDurationMs = 99_000L,
                transform = transformWithSpeed(0.5f),
            ),
        )
    }

    @Test
    fun maximumAchievableAtDoubleSpeedIsHalfTheSource() {
        val maximum = TargetDurationClipPlanner.maximumAchievableDurationMs(
            sourceRange = source,
            transform = transformWithSpeed(2f),
        )

        assertEquals(62_000L, maximum)
    }

    @Test
    fun maximumAchievableAtNeutralSpeedIsTheWholeSource() {
        val maximum = TargetDurationClipPlanner.maximumAchievableDurationMs(sourceRange = source)

        assertEquals(124_000L, maximum)
    }

    @Test
    fun maximumAchievableIsBelowTheRequestedTargetSoTheOfferIsAnActualReduction() {
        val maximum = assertNotNull(
            TargetDurationClipPlanner.maximumAchievableDurationMs(
                sourceRange = source,
                transform = transformWithSpeed(2f),
            ),
        )

        assertTrue(maximum < 99_000L, "the offer must lower the target, got $maximum")
    }

    @Test
    fun theOfferedMaximumIsItselfReachable() {
        val maximum = assertNotNull(
            TargetDurationClipPlanner.maximumAchievableDurationMs(
                sourceRange = source,
                transform = transformWithSpeed(2f),
            ),
        )

        assertNull(
            TargetDurationClipPlanner.unreachableReason(
                sourceRange = source,
                targetDurationMs = maximum,
                transform = transformWithSpeed(2f),
            ),
        )
    }

    @Test
    fun slowMotionRaisesTheCeilingAboveTheSourceLength() {
        val maximum = assertNotNull(
            TargetDurationClipPlanner.maximumAchievableDurationMs(
                sourceRange = source,
                transform = transformWithSpeed(0.5f),
            ),
        )

        assertEquals(248_000L, maximum)
    }

    @Test
    fun aTargetBelowTheMinimumIsRejectedBeforeAnySpeedMaths() {
        assertEquals(
            TargetDurationUnreachableReason.TARGET_TOO_SHORT,
            TargetDurationClipPlanner.unreachableReason(
                sourceRange = source,
                targetDurationMs = 1_000L,
                transform = transformWithSpeed(2f),
            ),
        )
    }

    @Test
    fun noCeilingIsOfferedWhenEvenTheMinimumTargetIsOutOfReach() {
        // 1500ms of source at neutral speed yields a 1500ms ceiling, below the 2000ms minimum,
        // so there is no target worth offering.
        val tiny = TrimRange(0L, 1_500L)

        assertNull(
            TargetDurationClipPlanner.maximumAchievableDurationMs(sourceRange = tiny),
        )
    }

    @Test
    fun aShortSourceIsRejectedBeforeAnyCeilingMaths() {
        val subMinimum = TrimRange(0L, AdaptiveCutCompiler.MIN_RANGE_DURATION_MS - 1L)

        assertEquals(
            TargetDurationUnreachableReason.SOURCE_TOO_SHORT,
            TargetDurationClipPlanner.unreachableReason(
                sourceRange = subMinimum,
                targetDurationMs = 2_000L,
            ),
        )
    }

    private fun transformWithSpeed(speed: Float) = TransformSettings(
        enabled = true,
        speedEnabled = true,
        speed = speed,
    )
}
