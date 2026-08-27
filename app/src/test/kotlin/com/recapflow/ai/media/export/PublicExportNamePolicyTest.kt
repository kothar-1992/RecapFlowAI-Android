package com.recapflow.ai.media.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PublicExportNamePolicyTest {
    @Test
    fun normalizesUnsafeNameAndKeepsMp4Extension() {
        assertEquals(
            "RecapFlow_720p_test.mp4",
            PublicExportNamePolicy.displayName("/private/RecapFlow 720p@test.MP4"),
        )
    }

    @Test
    fun keepsUnicodeLettersAndProvidesFallback() {
        assertEquals("မြန်မာ.mp4", PublicExportNamePolicy.displayName("မြန်မာ.mp4"))
        assertEquals("RecapFlow_video.mp4", PublicExportNamePolicy.displayName("..."))
    }

    @Test
    fun createsDistinctFinalAndHiddenPendingNames() {
        assertEquals(
            "RecapFlow_1.mp4",
            PublicExportNamePolicy.collisionName("RecapFlow.mp4", 1),
        )
        assertTrue(PublicExportNamePolicy.pendingName("RecapFlow.mp4", 7L).startsWith('.'))
        assertTrue(PublicExportNamePolicy.pendingName("RecapFlow.mp4", 7L).endsWith(".pending"))
    }
}
