package com.recapflow.ai.media.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PausedPreviewRefreshPolicyTest {

    @Test
    fun `30 fps refresh moves roughly two frames`() {
        assertEquals(67L, PausedPreviewRefreshPolicy.nudgeMs(30.0))
        assertEquals(
            1_067L,
            PausedPreviewRefreshPolicy.refreshTargetMs(
                anchorMs = 1_000L,
                durationMs = 10_000L,
                frameRate = 30.0,
                preferForward = true,
            ),
        )
    }

    @Test
    fun `refresh stays inside media boundaries`() {
        assertEquals(
            34L,
            PausedPreviewRefreshPolicy.refreshTargetMs(
                anchorMs = 0L,
                durationMs = 100L,
                frameRate = 60.0,
                preferForward = false,
            ),
        )
        assertEquals(
            66L,
            PausedPreviewRefreshPolicy.refreshTargetMs(
                anchorMs = 100L,
                durationMs = 100L,
                frameRate = 60.0,
                preferForward = true,
            ),
        )
    }

    @Test
    fun `settle restores only an automatic refresh pulse`() {
        assertTrue(PausedPreviewRefreshPolicy.mayRestoreAnchor(1_000L, 1_067L, 30.0))
        assertFalse(PausedPreviewRefreshPolicy.mayRestoreAnchor(1_000L, 2_000L, 30.0))
    }
}
