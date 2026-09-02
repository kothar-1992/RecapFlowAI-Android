package com.recapflow.ai.media.edit

/**
 * Resolves the visual Mirror choice for each reviewed clip.
 *
 * "Random" is deterministic by design: the same EditPlan + source ranges always produce the
 * same mirror decisions so CompositionPlayer preview and Transformer export cannot disagree.
 * The global Mirror toggle remains unchanged. Random-per-clip mode is a separate, mutually
 * exclusive UI choice and only becomes active when the timeline contains at least two clips.
 */
object PerClipMirrorPolicy {

    fun resolvedSettings(
        settings: TransformSettings,
        range: TrimRange,
        rangeIndex: Int,
        clipCount: Int,
    ): TransformSettings {
        val resolvedMirror = shouldMirror(settings, range, rangeIndex, clipCount)
        return settings.copy(
            mirrorEnabled = resolvedMirror,
            randomMirrorPerClipEnabled = false,
        )
    }

    fun shouldMirror(
        settings: TransformSettings,
        range: TrimRange,
        rangeIndex: Int,
        clipCount: Int,
    ): Boolean {
        if (!settings.enabled) return false
        if (!settings.randomMirrorPerClipEnabled) return settings.mirrorEnabled
        if (clipCount < 2) return false
        return (stableMix(range, rangeIndex) and 1L) == 0L
    }

    private fun stableMix(range: TrimRange, rangeIndex: Int): Long {
        var value = range.startMs * 31L
        value = value xor (range.endMs * 17L)
        value = value xor (rangeIndex.toLong() * GOLDEN_GAMMA)
        value = (value xor (value ushr 30)) * MIX_1
        value = (value xor (value ushr 27)) * MIX_2
        return value xor (value ushr 31)
    }

    private const val GOLDEN_GAMMA = -7046029254386353131L
    private const val MIX_1 = -4658895280553007687L
    private const val MIX_2 = -7723592293110705685L
}
