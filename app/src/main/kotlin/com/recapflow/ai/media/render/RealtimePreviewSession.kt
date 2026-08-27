package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.TransformSettings

/** Effect-list identity. Blur/logo geometry and opacity are deliberately absent because their
 * preview shaders consume realtime state without rebuilding the Media3 graph. */
data class PreviewGraphKey(
    val sourcePath: String,
    val transform: TransformSettings,
    val sourceBlurPresent: Boolean,
    val imageAssetPath: String?,
    val timelineOffsetUs: Long,
    val sourceDurationMs: Long,
    val sourceTimeOffsetUs: Long = 0L,
)

data class PreviewGraphUpdate internal constructor(
    val revision: Long,
    val generation: Long,
    val key: PreviewGraphKey,
    val reason: String,
)

/**
 * Main-thread state machine for the one retained source-preview player.
 *
 * It coalesces rapid graph requests, ignores an already-applied graph, rejects stale updates from
 * a previous source, and allows at most one no-effects recovery attempt per session generation.
 */
class RealtimePreviewSession {

    private var sourcePath: String? = null
    private var generation = 0L
    private var nextRevision = 0L
    private var appliedKey: PreviewGraphKey? = null
    private var applyingKey: PreviewGraphKey? = null
    private var pendingUpdate: PreviewGraphUpdate? = null
    private var recoveryClaimed = false

    fun begin(path: String, restart: Boolean = false): Long {
        if (sourcePath != path || restart) {
            sourcePath = path
            generation += 1L
            appliedKey = null
            applyingKey = null
            pendingUpdate = null
            recoveryClaimed = false
        }
        return generation
    }

    fun request(key: PreviewGraphKey, reason: String): Boolean {
        if (sourcePath != key.sourcePath) begin(key.sourcePath)
        if (appliedKey == key || applyingKey == key) {
            pendingUpdate = null
            return false
        }
        nextRevision += 1L
        pendingUpdate = PreviewGraphUpdate(
            revision = nextRevision,
            generation = generation,
            key = key,
            reason = reason,
        )
        return true
    }

    fun takePending(): PreviewGraphUpdate? = pendingUpdate.also { pendingUpdate = null }

    fun markApplying(key: PreviewGraphKey) {
        if (sourcePath == key.sourcePath) applyingKey = key
    }

    fun confirmApplied(): PreviewGraphKey? {
        val confirmed = applyingKey ?: return appliedKey
        if (sourcePath == confirmed.sourcePath) appliedKey = confirmed
        applyingKey = null
        return appliedKey
    }

    fun clearAppliedGraph() {
        appliedKey = null
        applyingKey = null
        pendingUpdate = null
    }

    fun clearPending() {
        pendingUpdate = null
    }

    fun currentGeneration(): Long = generation

    /** Transform currently installed or being installed; pending user input is excluded. */
    fun currentTransform(): TransformSettings? = (applyingKey ?: appliedKey)?.transform

    fun currentGraphSummary(): String {
        val key = applyingKey ?: appliedKey ?: pendingUpdate?.key ?: return "none"
        return "transform=${key.transform.enabled},blur=${key.sourceBlurPresent}," +
            "image=${key.imageAssetPath != null},durationMs=${key.sourceDurationMs}"
    }

    fun isCurrent(path: String, expectedGeneration: Long): Boolean =
        sourcePath == path && generation == expectedGeneration

    fun claimRecovery(path: String, expectedGeneration: Long): Boolean {
        if (!isCurrent(path, expectedGeneration) || recoveryClaimed) return false
        recoveryClaimed = true
        return true
    }
}
