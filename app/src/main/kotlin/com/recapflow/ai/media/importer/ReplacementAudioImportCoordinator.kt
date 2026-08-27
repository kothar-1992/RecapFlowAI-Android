package com.recapflow.ai.media.importer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.OpenableColumns
import com.recapflow.ai.media.edit.ReplacementAudioAsset
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

sealed interface ReplacementAudioImportState {
    data object Idle : ReplacementAudioImportState
    data class Importing(val displayName: String) : ReplacementAudioImportState
    data class Ready(val asset: ReplacementAudioAsset) : ReplacementAudioImportState
    data class Error(val message: String) : ReplacementAudioImportState
}

/** Copies one user-selected audio document into the private, on-device edit workspace. */
class ReplacementAudioImportCoordinator(
    context: Context,
    private val onStateChanged: (ReplacementAudioImportState) -> Unit,
) : Closeable {

    private val appContext = context.applicationContext
    private val audioDirectory = File(appContext.cacheDir, AUDIO_DIRECTORY)
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val generation = AtomicInteger(0)

    @Volatile
    private var closed = false

    @Volatile
    var currentState: ReplacementAudioImportState = ReplacementAudioImportState.Idle
        private set

    private var activeWorkingPath: String? = null

    fun import(uri: Uri) {
        val operation = generation.incrementAndGet()
        worker.execute {
            val previousPath = activeWorkingPath
            val result = runCatching { prepare(uri, operation) }
            result.onSuccess { asset ->
                if (closed || generation.get() != operation) {
                    deleteWorkingFile(asset.workingFilePath)
                    return@onSuccess
                }
                if (previousPath != asset.workingFilePath) deleteWorkingFile(previousPath)
                activeWorkingPath = asset.workingFilePath
                emit(operation, ReplacementAudioImportState.Ready(asset))
            }.onFailure { error ->
                emit(
                    operation,
                    ReplacementAudioImportState.Error(
                        error.message ?: "The selected audio file could not be prepared",
                    ),
                )
            }
        }
    }

    fun restore(asset: ReplacementAudioAsset?): ReplacementAudioAsset? {
        asset ?: return null
        return runCatching {
            ensureAudioDirectory()
            val workingFile = File(asset.workingFilePath)
            if (
                workingFile.parentFile?.canonicalFile != audioDirectory.canonicalFile ||
                !workingFile.isFile ||
                workingFile.length() <= 0L ||
                asset.durationMs <= 0L
            ) {
                return@runCatching null
            }
            asset.copy(
                workingFilePath = workingFile.absolutePath,
                displayName = asset.displayName.ifBlank { workingFile.name },
                fileSizeBytes = workingFile.length(),
            )
        }.getOrNull()?.also { restored ->
            activeWorkingPath = restored.workingFilePath
            currentState = ReplacementAudioImportState.Ready(restored)
        }
    }

    fun clear(asset: ReplacementAudioAsset?) {
        generation.incrementAndGet()
        deleteWorkingFile(asset?.workingFilePath)
        activeWorkingPath = null
        currentState = ReplacementAudioImportState.Idle
        if (!closed) onStateChanged(currentState)
    }

    private fun prepare(uri: Uri, operation: Int): ReplacementAudioAsset {
        ensureAudioDirectory()
        val source = querySource(uri)
        ensureSpace(source.sizeBytes)
        emit(operation, ReplacementAudioImportState.Importing(source.displayName))

        val extension = source.displayName
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
            .takeIf { it.matches(EXTENSION_PATTERN) }
            ?.let { ".$it" }
            .orEmpty()
        val baseName = UUID.randomUUID().toString()
        val partialFile = File(audioDirectory, "$baseName.part")
        val completedFile = File(audioDirectory, "$baseName$extension")

        try {
            val input = appContext.contentResolver.openInputStream(uri)
                ?: throw FileNotFoundException("The selected provider returned no audio data")
            input.use { sourceStream ->
                FileOutputStream(partialFile).use { destination ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        if (Thread.currentThread().isInterrupted) {
                            throw InterruptedIOException("Audio preparation was cancelled")
                        }
                        val count = sourceStream.read(buffer)
                        if (count < 0) break
                        destination.write(buffer, 0, count)
                    }
                    destination.fd.sync()
                }
            }
            if (partialFile.length() <= 0L) {
                throw IOException("The selected audio file is empty")
            }
            if (!partialFile.renameTo(completedFile)) {
                throw IOException("Could not finalize the replacement audio")
            }
            val durationMs = readDurationMs(completedFile)
            if (durationMs <= 0L) {
                throw IOException("The selected file does not contain readable audio duration")
            }
            return ReplacementAudioAsset(
                workingFilePath = completedFile.absolutePath,
                displayName = source.displayName,
                durationMs = durationMs,
                fileSizeBytes = completedFile.length(),
            )
        } catch (error: SecurityException) {
            throw IOException("RecapFlowAI no longer has permission to read this audio", error)
        } finally {
            partialFile.delete()
            if (completedFile.isFile && runCatching { readDurationMs(completedFile) }.getOrDefault(0L) <= 0L) {
                completedFile.delete()
            }
        }
    }

    private fun readDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
        } finally {
            retriever.release()
        }
    }

    private fun querySource(uri: Uri): SourceDescription {
        var displayName: String? = null
        var sizeBytes: Long? = null
        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let { displayName = cursor.getString(it) }
                cursor.getColumnIndex(OpenableColumns.SIZE)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let { sizeBytes = cursor.getLong(it).takeIf { size -> size >= 0L } }
            }
        }
        return SourceDescription(
            displayName = displayName?.takeIf(String::isNotBlank) ?: DEFAULT_DISPLAY_NAME,
            sizeBytes = sizeBytes,
        )
    }

    private fun ensureAudioDirectory() {
        if (!audioDirectory.exists() && !audioDirectory.mkdirs()) {
            throw IOException("The private replacement-audio workspace could not be created")
        }
    }

    private fun ensureSpace(sourceSize: Long?) {
        sourceSize ?: return
        if (StatFs(audioDirectory.absolutePath).availableBytes < sourceSize + MIN_FREE_BYTES) {
            throw IOException("Not enough free storage to prepare this audio")
        }
    }

    private fun deleteWorkingFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path)
            if (file.parentFile?.canonicalFile == audioDirectory.canonicalFile) file.delete()
        }
    }

    private fun emit(operation: Int, state: ReplacementAudioImportState) {
        mainHandler.post {
            if (!closed && generation.get() == operation) {
                currentState = state
                onStateChanged(state)
            }
        }
    }

    override fun close() {
        closed = true
        generation.incrementAndGet()
        mainHandler.removeCallbacksAndMessages(null)
        worker.shutdownNow()
    }

    private data class SourceDescription(
        val displayName: String,
        val sizeBytes: Long?,
    )

    companion object {
        private const val AUDIO_DIRECTORY = "replacement_audio"
        private const val COPY_BUFFER_BYTES = 256 * 1024
        private const val MIN_FREE_BYTES = 16L * 1024L * 1024L
        private const val DEFAULT_DISPLAY_NAME = "Selected audio"
        private val EXTENSION_PATTERN = Regex("[a-z0-9]{1,8}")
    }
}
