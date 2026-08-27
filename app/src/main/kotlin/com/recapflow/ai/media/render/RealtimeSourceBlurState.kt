package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.CompiledSourceSubtitleBlur

/**
 * Thread-safe preview-only bridge for source-subtitle blur controls.
 *
 * Media3 can retain a custom GL program while the editor changes rectangle, strength, or active
 * time. The shader reads this immutable snapshot for every frame. Export never receives this
 * mutable state and continues to compile from the captured [com.recapflow.ai.media.edit.EditPlan].
 */
class RealtimeSourceBlurState {

    @Volatile
    private var current: CompiledSourceSubtitleBlur? = null

    fun update(blur: CompiledSourceSubtitleBlur?) {
        current = blur
    }

    internal fun snapshot(): CompiledSourceSubtitleBlur? = current
}
