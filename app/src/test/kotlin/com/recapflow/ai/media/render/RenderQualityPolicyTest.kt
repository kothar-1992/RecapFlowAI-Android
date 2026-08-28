package com.recapflow.ai.media.render

import com.recapflow.ai.media.MediaInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RenderQualityPolicyTest {
    @Test fun thirtyFps720pUsesFiveMegabitTarget() {
        val r = RenderQualityPolicy.forSource(mediaInfo(576,1024,30.0), RenderPreset.HD_720P)
        assertEquals(5_000_000, r.requestedVideoBitrate); assertEquals(30, r.targetFrameRate); assertTrue(r.isUpscaling)
    }
    @Test fun sixtyFps1080pUsesTwelveMegabitTarget() {
        val r = RenderQualityPolicy.forSource(mediaInfo(1080,1920,59.94), RenderPreset.FULL_HD_1080P)
        assertEquals(12_000_000, r.requestedVideoBitrate); assertEquals(60, r.targetFrameRate); assertFalse(r.isUpscaling)
    }
    @Test fun twoKUsesSixteenOrTwentyFourMegabitByFrameRate() {
        assertEquals(16_000_000, RenderQualityPolicy.forSource(mediaInfo(1080,1920,30.0), RenderPreset.QHD_2K).requestedVideoBitrate)
        assertEquals(24_000_000, RenderQualityPolicy.forSource(mediaInfo(1080,1920,60.0), RenderPreset.QHD_2K).requestedVideoBitrate)
    }
    @Test fun sourceContainerBitrateDoesNotInflateTarget() {
        val r = RenderQualityPolicy.forSource(mediaInfo(1080,1920,30.0, bitrate=80_000_000L), RenderPreset.FULL_HD_1080P)
        assertEquals(8_000_000, r.requestedVideoBitrate)
    }
    private fun mediaInfo(width:Int,height:Int,fps:Double,bitrate:Long=5_000_000L)=MediaInfo(
        sourceUri="content://test/source", workingFilePath="/tmp/source.mp4", displayName="source.mp4",
        fileSizeBytes=1L,durationMs=60_000L,width=width,height=height,rotationDegrees=0,frameRate=fps,
        videoCodec="h264",audioCodec="aac",audioSampleRate=48_000,audioChannels=2,bitrate=bitrate,containerFormat="mp4")
}
