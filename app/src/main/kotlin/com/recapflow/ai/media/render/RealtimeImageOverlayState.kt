package com.recapflow.ai.media.render

import com.recapflow.ai.media.edit.CompiledImageOverlay

/**
 * Thread-safe bridge from Editor controls to an already-created preview shader.
 *
 * Media3 may retain the active custom GL program while a new preview effect list is installed.
 * Keeping only immutable compiled settings inside that program therefore leaves position, size,
 * opacity, or active-time controls visually stale. Preview shaders read this snapshot once per
 * frame; export effects deliberately do not receive this mutable bridge.
 */
class RealtimeImageOverlayState {

    @Volatile
    private var current: CompiledImageOverlay? = null

    fun update(image: CompiledImageOverlay?) {
        current = image
    }

    internal fun snapshotFor(workingFilePath: String): CompiledImageOverlay? =
        current?.takeIf { it.asset.workingFilePath == workingFilePath }
}
