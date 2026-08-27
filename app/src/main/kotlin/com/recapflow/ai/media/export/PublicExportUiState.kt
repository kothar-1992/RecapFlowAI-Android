package com.recapflow.ai.media.export

import android.net.Uri

sealed interface PublicExportUiState {
    data object Idle : PublicExportUiState

    data class PermissionRequired(
        val sourcePath: String,
        val displayName: String,
    ) : PublicExportUiState

    data class Publishing(
        val sourcePath: String,
        val displayName: String,
    ) : PublicExportUiState

    data class Published(
        val sourcePath: String,
        val contentUri: Uri,
        val displayName: String,
        val publicLocation: String,
    ) : PublicExportUiState

    data class Failed(
        val sourcePath: String,
        val displayName: String,
        val message: String,
    ) : PublicExportUiState
}
