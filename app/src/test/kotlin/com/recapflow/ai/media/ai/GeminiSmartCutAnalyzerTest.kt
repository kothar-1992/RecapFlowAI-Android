package com.recapflow.ai.media.ai

import com.recapflow.ai.media.edit.TrimRange
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.test.*

class GeminiSmartCutAnalyzerTest {
    private val key = "synthetic-test-key-no-real-credential"
    private val model = GeminiSmartCutAnalyzer.DEFAULT_MODEL
    private val base = GeminiSmartCutAnalyzer.BASE
    private fun envelope(candidates: String = "[]", status: String = "completed") = JSONObject()
        .put("status", status).put("outputs", JSONArray().put(JSONObject().put("type", "text")
            .put("text", "{\"candidates\":$candidates}"))).toString()

    private val candidate = """{"startMs":2000,"endMs":5000,"reason":"PAUSE","confidence":0.95,"evidence":"No speech; no meaningful scene action","completeThought":false}"""

    private fun withMedia(action: (File) -> Unit) {
        val file = File.createTempFile("smart-cut", ".mp4")
        try { file.writeBytes(byteArrayOf(1, 2, 3)); action(file) } finally { file.delete() }
    }

    private fun uploadResponses(): MutableList<GeminiHttpResponse> = mutableListOf(
        GeminiHttpResponse(200, "", mapOf("x-goog-upload-url" to "$base/upload/session1")),
        GeminiHttpResponse(200, """{"file":{"name":"files/owned1","state":"ACTIVE","uri":"$base/v1beta/files/owned1"}}"""),
    )

    @Test fun fullLifecycleStreamsFileAndDeletesOnlyOwnedUpload() = withMedia { file ->
        val responses = uploadResponses().apply { add(GeminiHttpResponse(200, envelope("[$candidate]"))); add(GeminiHttpResponse(200, "{}")) }
        val requests = mutableListOf<GeminiHttpRequest>()
        val analyzer = GeminiSmartCutAnalyzer(GeminiTransport { request, _ -> requests += request; responses.removeAt(0) })
        val result = analyzer.analyze(file, "video/mp4", 10_000, key, model, SmartCutAnalysisMode.CLEAN_PAUSES, GeminiCancellation())
        assertEquals(1, result.candidates.size)
        assertFalse(result.cleanupFailed)
        assertSame(file, requests[1].file)
        assertFalse(requests[0].toString().contains(key))
        assertTrue(requests.all { !it.url.contains(key) })
        val requestBody = JSONObject(requests[2].json!!)
        assertFalse(requestBody.getBoolean("store"))
        assertFalse(requestBody.has("tools"))
        assertEquals("DELETE", requests.last().method)
        assertEquals("$base/v1beta/files/owned1", requests.last().url)
    }

    @Test fun quotaFailureDeletesUploadAndDoesNotRepeatBillableAnalysis() = withMedia { file ->
        val responses = uploadResponses().apply { add(GeminiHttpResponse(429, "quota $key")); add(GeminiHttpResponse(200, "{}")) }
        val requests = mutableListOf<GeminiHttpRequest>()
        val analyzer = GeminiSmartCutAnalyzer(GeminiTransport { request, _ -> requests += request; responses.removeAt(0) })
        val error = assertFailsWith<GeminiException> {
            analyzer.analyze(file, "video/mp4", 10_000, key, model, SmartCutAnalysisMode.CLEAN_PAUSES, GeminiCancellation())
        }
        assertEquals(GeminiFailure.QUOTA, error.failure)
        assertFalse(error.toString().contains(key))
        assertEquals(1, requests.count { it.url.endsWith("interactions") })
        assertEquals("DELETE", requests.last().method)
    }

    @Test fun cancellationAfterUploadStillCleansUp() = withMedia { file ->
        val token = GeminiCancellation()
        val requests = mutableListOf<GeminiHttpRequest>()
        val responses = uploadResponses().apply { add(GeminiHttpResponse(200, "{}")) }
        val analyzer = GeminiSmartCutAnalyzer(GeminiTransport { request, _ ->
            requests += request
            responses.removeAt(0).also { if (request.file != null) token.cancel() }
        })
        val error = assertFailsWith<GeminiException> {
            analyzer.analyze(file, "video/mp4", 10_000, key, model, SmartCutAnalysisMode.CLEAN_PAUSES, token)
        }
        assertEquals(GeminiFailure.CANCELLED, error.failure)
        assertEquals("DELETE", requests.last().method)
        assertFalse(error.cleanupFailed)
    }

    @Test fun cleanupFailureIsVisibleAlongsideSuccessfulAnalysis() = withMedia { file ->
        val responses = uploadResponses().apply { add(GeminiHttpResponse(200, envelope())); add(GeminiHttpResponse(503, "error")) }
        val analyzer = GeminiSmartCutAnalyzer(GeminiTransport { _, _ -> responses.removeAt(0) })
        assertTrue(analyzer.analyze(file, "video/mp4", 10_000, key, model, SmartCutAnalysisMode.CLEAN_PAUSES, GeminiCancellation()).cleanupFailed)
    }

    @Test fun replacedMediaRejectsDraftAndStillDeletesUpload() = withMedia { file ->
        val responses = uploadResponses().apply { add(GeminiHttpResponse(200, envelope())); add(GeminiHttpResponse(200, "{}")) }
        val requests = mutableListOf<GeminiHttpRequest>()
        val analyzer = GeminiSmartCutAnalyzer(GeminiTransport { request, _ ->
            requests += request
            if (request.url.endsWith("interactions")) file.writeBytes(byteArrayOf(4, 5, 6))
            responses.removeAt(0)
        })
        val error = assertFailsWith<GeminiException> {
            analyzer.analyze(file, "video/mp4", 10_000, key, model, SmartCutAnalysisMode.CLEAN_PAUSES, GeminiCancellation())
        }
        assertEquals(GeminiFailure.STALE_SOURCE, error.failure)
        assertEquals("DELETE", requests.last().method)
    }

    @Test fun foreignUploadUrlNeverReceivesKeyOrFile() = withMedia { file ->
        var requests = 0
        val analyzer = GeminiSmartCutAnalyzer(GeminiTransport { _, _ ->
            requests++; GeminiHttpResponse(200, "", mapOf("x-goog-upload-url" to "https://example.com/steal"))
        })
        assertEquals(GeminiFailure.INVALID_RESPONSE, assertFailsWith<GeminiException> {
            analyzer.analyze(file, "video/mp4", 10_000, key, model, SmartCutAnalysisMode.CLEAN_PAUSES, GeminiCancellation())
        }.failure)
        assertEquals(1, requests)
    }

    @Test fun parserRejectsFractionalStringAndOutsideWindowTimestamps() {
        for (bad in listOf(candidate.replace("2000", "2000.5"), candidate.replace("2000", "\"2000\""), candidate.replace("5000", "15000"))) {
            assertEquals(GeminiFailure.INVALID_RESPONSE, assertFailsWith<GeminiException> {
                GeminiSmartCutAnalyzer.parseInteraction(envelope("[$bad]"), TrimRange(0, 10_000))
            }.failure)
        }
    }

    @Test fun unfinishedResponseIsNotAppliedAsPartialPlan() {
        assertFailsWith<GeminiException> { GeminiSmartCutAnalyzer.parseInteraction(envelope("[$candidate]", "in_progress"), TrimRange(0, 10_000)) }
    }

    @Test fun longVideoWindowsKeepOriginalSourceOffsets() {
        val windows = GeminiSmartCutAnalyzer.windows(250_000)
        assertEquals(listOf(TrimRange(0, 120_000), TrimRange(120_000, 240_000), TrimRange(240_000, 250_000)), windows)
        val request = GeminiSmartCutAnalyzer.analysisRequest(model, "$base/v1beta/files/owned1", "video/mp4", windows[1], SmartCutAnalysisMode.SHORT_RECAP)
        val processing = request.getJSONArray("input").getJSONObject(0).getJSONObject("processing")
        assertEquals(120.0, processing.getDouble("start_offset"))
        assertEquals(240.0, processing.getDouble("end_offset"))
    }

    @Test fun errorPolicyDoesNotTreatAllLoadingFailuresAsRegionOrVpn() {
        assertEquals(GeminiFailure.REGION, GeminiErrorPolicy.classify(400, "User location is not supported"))
        assertEquals(GeminiFailure.KEY, GeminiErrorPolicy.classify(400, "API key not valid"))
        assertEquals(GeminiFailure.REQUEST, GeminiErrorPolicy.classify(403, "Permission denied"))
        assertEquals(GeminiFailure.QUOTA, GeminiErrorPolicy.classify(429, "Resource exhausted"))
        assertEquals(GeminiFailure.SERVER, GeminiErrorPolicy.classify(503, "Unavailable"))
    }
}
