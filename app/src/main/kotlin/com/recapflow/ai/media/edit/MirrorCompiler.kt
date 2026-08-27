package com.recapflow.ai.media.edit

/**
 * Compiles the optional horizontal mirror into axis scale factors understood by
 * Media3's matrix transformation. Returning null is the explicit Off behavior.
 */
object MirrorCompiler {

    fun compile(settings: TransformSettings): CompiledMirror? {
        if (!settings.enabled || !settings.mirrorEnabled) return null
        return CompiledMirror(scaleX = -1f, scaleY = 1f)
    }
}

data class CompiledMirror(
    val scaleX: Float,
    val scaleY: Float,
)
