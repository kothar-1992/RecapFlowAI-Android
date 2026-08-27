package com.recapflow.ai.media.importer

import com.recapflow.ai.media.MediaInfo
import com.recapflow.ai.media.NativeErrorCode
import com.recapflow.ai.media.PreparedMedia

sealed interface ImportUiState {
    data object EngineChecking : ImportUiState

    data class Empty(
        val engineVersion: String,
    ) : ImportUiState

    data class Picking(
        val engineVersion: String,
    ) : ImportUiState

    data class Preparing(
        val engineVersion: String,
        val sourceUri: String,
        val displayName: String,
        val copiedBytes: Long,
        val totalBytes: Long?,
    ) : ImportUiState

    data class Probing(
        val engineVersion: String,
        val preparedMedia: PreparedMedia,
    ) : ImportUiState

    data class Ready(
        val engineVersion: String,
        val mediaInfo: MediaInfo,
    ) : ImportUiState

    data class Error(
        val engineVersion: String?,
        val title: String,
        val message: String,
        val code: NativeErrorCode,
        val diagnostics: String? = null,
        val recoverable: Boolean,
        val sourceUri: String? = null,
        val preparedMedia: PreparedMedia? = null,
    ) : ImportUiState
}

data class ImportResumeRequest(
    val sourceUri: String?,
    val workingFilePath: String?,
    val displayName: String?,
)
