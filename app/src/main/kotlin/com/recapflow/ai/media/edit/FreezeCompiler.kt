package com.recapflow.ai.media.edit

/** Compiles the reversible intro-freeze operation used by preview and export. */
object FreezeCompiler {
    const val MIN_DURATION_MS = 1_000L
    const val MAX_DURATION_MS = 3_000L
    const val DEFAULT_DURATION_MS = 2_000L

    val supportedDurationsMs = setOf(1_000L, 2_000L, 3_000L)

    fun compile(settings: TransformSettings): CompiledFreeze? {
        val freeze = settings.freeze
        if (!settings.enabled || !freeze.enabled) return null
        if (freeze.durationMs !in supportedDurationsMs) return null
        return CompiledFreeze(freeze.durationMs)
    }
}

data class CompiledFreeze(
    val durationMs: Long,
)
