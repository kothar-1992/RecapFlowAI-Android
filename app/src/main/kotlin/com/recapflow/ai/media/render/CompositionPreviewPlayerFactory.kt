package com.recapflow.ai.media.render

import android.content.Context
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MultipleInputVideoGraph
import androidx.media3.transformer.CompositionPlayer

/** Creates the preview player required by the compiled shared Composition topology. */
@OptIn(ExperimentalApi::class)
@UnstableApi
object CompositionPreviewPlayerFactory {
    fun create(
        context: Context,
        compiled: CompiledMedia3Composition,
    ): CompositionPlayer = CompositionPlayer.Builder(context)
        .apply {
            if (compiled.requiresMultipleInputVideoGraph) {
                setVideoGraphFactory(MultipleInputVideoGraph.Factory())
            }
        }
        .build()
}
