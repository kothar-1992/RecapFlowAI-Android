package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.TransformSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RealtimePreviewSessionTest {

    @Test
    fun rapidRequestsCoalesceToLatestGraph() {
        val session = RealtimePreviewSession()
        session.begin(SOURCE)

        session.request(key(timelineOffsetUs = 1_000L), "first")
        session.request(key(timelineOffsetUs = 2_000L), "latest")

        val update = session.takePending()
        assertEquals(2_000L, update?.key?.timelineOffsetUs)
        assertEquals("latest", update?.reason)
        assertNull(session.takePending())
    }

    @Test
    fun identicalAppliedGraphDoesNotScheduleReplacement() {
        val session = RealtimePreviewSession()
        session.begin(SOURCE)
        val key = key(timelineOffsetUs = 0L)
        session.markApplying(key)
        session.confirmApplied()

        assertFalse(session.request(key, "blur geometry only"))
        assertNull(session.takePending())
    }

    @Test
    fun recoveryIsBoundedToOneAttemptPerGeneration() {
        val session = RealtimePreviewSession()
        val generation = session.begin(SOURCE)

        assertTrue(session.claimRecovery(SOURCE, generation))
        assertFalse(session.claimRecovery(SOURCE, generation))

        val nextGeneration = session.begin(SOURCE, restart = true)
        assertTrue(session.claimRecovery(SOURCE, nextGeneration))
        assertFalse(session.isCurrent(SOURCE, generation))
    }

    @Test
    fun graphIsNotAppliedUntilAFrameConfirmsIt() {
        val session = RealtimePreviewSession()
        session.begin(SOURCE)
        val key = key(timelineOffsetUs = 5_000L)

        session.markApplying(key)
        assertFalse(session.request(key, "duplicate while applying"))
        assertEquals(key, session.confirmApplied())
        assertFalse(session.request(key, "duplicate after first frame"))
    }

    private fun key(timelineOffsetUs: Long) = PreviewGraphKey(
        sourcePath = SOURCE,
        transform = TransformSettings(),
        sourceBlurPresent = false,
        imageAssetPath = null,
        timelineOffsetUs = timelineOffsetUs,
        sourceDurationMs = 10_000L,
    )

    private companion object {
        const val SOURCE = "/private/source.mp4"
    }
}
