package com.recapflow.ai.media.export

object PublicExportNamePolicy {
    private const val MAX_STEM_LENGTH = 96

    fun displayName(sourceFileName: String): String {
        val fileName = sourceFileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .ifBlank { "RecapFlow_video.mp4" }
        val rawStem = if (fileName.endsWith(".mp4", ignoreCase = true)) {
            fileName.dropLast(4)
        } else {
            fileName.substringBeforeLast('.', fileName)
        }
        val safeStem = rawStem
            .map { character ->
                when {
                    character.isLetterOrDigit() -> character
                    character == '-' || character == '_' -> character
                    else -> '_'
                }
            }
            .joinToString(separator = "")
            .trim('_')
            .take(MAX_STEM_LENGTH)
            .ifBlank { "RecapFlow_video" }
        return "$safeStem.mp4"
    }

    fun collisionName(requestedName: String, copyIndex: Int): String {
        require(copyIndex >= 1)
        val normalized = displayName(requestedName)
        val stem = normalized.removeSuffix(".mp4")
        return "${stem}_$copyIndex.mp4"
    }

    fun pendingName(requestedName: String, generation: Long): String =
        ".${displayName(requestedName)}.$generation.pending"
}
