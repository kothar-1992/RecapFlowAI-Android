package com.recapflow.ai.media.importer

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.recapflow.ai.media.NativeErrorCode
import com.recapflow.ai.media.NativeMediaBridge
import com.recapflow.ai.media.PreparedMedia
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class MediaImportCoordinator(
    context: Context,
    private val onStateChanged: (ImportUiState) -> Unit,
) : Closeable {

    private val sourcePreparer = MediaSourcePreparer(context)
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val generation = AtomicInteger(0)

    @Volatile
    private var closed = false

    @Volatile
    var currentState: ImportUiState = ImportUiState.EngineChecking
        private set

    private var engineVersion: String? = null
    private var stateBeforePicker: ImportUiState? = null
    private var activeWorkingPath: String? = null

    fun initialize(resumeRequest: ImportResumeRequest?) {
        val operation = generation.incrementAndGet()
        emit(operation, ImportUiState.EngineChecking)
        worker.execute {
            val versionResult = NativeMediaBridge.nativeVersion()
            if (!versionResult.isSuccess) {
                emit(
                    operation,
                    ImportUiState.Error(
                        engineVersion = null,
                        title = "Media engine unavailable",
                        message = versionResult.message,
                        code = versionResult.code,
                        diagnostics = versionResult.ffmpegError,
                        recoverable = false,
                    ),
                )
                return@execute
            }

            val version = versionResult.value.orEmpty()
            engineVersion = version
            if (!version.contains("/ FFmpeg ")) {
                emit(
                    operation,
                    ImportUiState.Error(
                        engineVersion = version,
                        title = "FFmpeg is not enabled",
                        message = "Build with recapflow.ffmpeg.enabled=true to import video",
                        code = NativeErrorCode.NATIVE_ERROR,
                        recoverable = false,
                    ),
                )
                return@execute
            }

            val restored = resumeRequest?.let { request ->
                val uri = request.sourceUri
                val path = request.workingFilePath
                val name = request.displayName
                if (!uri.isNullOrBlank() && !path.isNullOrBlank() && !name.isNullOrBlank()) {
                    sourcePreparer.restore(uri, path, name)
                } else {
                    null
                }
            }

            if (restored != null) {
                activeWorkingPath = restored.workingFilePath
                probe(operation, version, restored)
                return@execute
            }

            val resumableUri = resumeRequest?.sourceUri
                ?.takeIf { it.isNotBlank() }
                ?.let(Uri::parse)
            if (resumableUri != null) {
                prepareAndProbe(operation, version, resumableUri)
            } else {
                emit(operation, ImportUiState.Empty(version))
            }
        }
    }

    fun beginPicking() {
        val version = engineVersion ?: return
        stateBeforePicker = currentState.takeUnless {
            it is ImportUiState.EngineChecking || it is ImportUiState.Picking
        }
        emitCurrent(ImportUiState.Picking(version))
    }

    fun pickerCancelled() {
        val fallback = stateBeforePicker
            ?: engineVersion?.let(ImportUiState::Empty)
            ?: ImportUiState.EngineChecking
        emitCurrent(fallback)
        stateBeforePicker = null
    }

    fun import(uri: Uri) {
        val version = engineVersion ?: return
        val operation = generation.incrementAndGet()
        stateBeforePicker = null
        worker.execute {
            prepareAndProbe(operation, version, uri)
        }
    }

    fun retry() {
        when (val state = currentState) {
            is ImportUiState.Error -> {
                val version = engineVersion ?: return
                val operation = generation.incrementAndGet()
                val prepared = state.preparedMedia
                if (prepared != null) {
                    worker.execute { probe(operation, version, prepared) }
                } else {
                    val uri = state.sourceUri?.let(Uri::parse) ?: return
                    worker.execute { prepareAndProbe(operation, version, uri) }
                }
            }
            else -> Unit
        }
    }

    private fun prepareAndProbe(operation: Int, version: String, uri: Uri) {
        var lastProgressUpdate = 0L
        try {
            var displayName = "Selected video"
            val prepared = sourcePreparer.prepare(
                uri = uri,
                onSourceResolved = { resolvedName, totalBytes ->
                    displayName = resolvedName
                    emit(
                        operation,
                        ImportUiState.Preparing(
                            engineVersion = version,
                            sourceUri = uri.toString(),
                            displayName = resolvedName,
                            copiedBytes = 0L,
                            totalBytes = totalBytes,
                        ),
                    )
                },
            ) { copiedBytes, totalBytes ->
                val now = SystemClock.elapsedRealtime()
                val complete = totalBytes != null && copiedBytes >= totalBytes
                if (complete || now - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL_MS) {
                    lastProgressUpdate = now
                    emit(
                        operation,
                        ImportUiState.Preparing(
                            engineVersion = version,
                            sourceUri = uri.toString(),
                            displayName = displayName,
                            copiedBytes = copiedBytes,
                            totalBytes = totalBytes,
                        ),
                    )
                }
            }
            probe(operation, version, prepared)
        } catch (error: MediaPreparationException) {
            emit(
                operation,
                ImportUiState.Error(
                    engineVersion = version,
                    title = preparationTitle(error.code),
                    message = error.message,
                    code = error.code,
                    diagnostics = error.cause?.message,
                    recoverable = error.recoverable,
                    sourceUri = uri.toString(),
                ),
            )
        } catch (error: RuntimeException) {
            emit(
                operation,
                ImportUiState.Error(
                    engineVersion = version,
                    title = "Video preparation failed",
                    message = error.message ?: error.javaClass.simpleName,
                    code = NativeErrorCode.INPUT_COPY_FAILED,
                    recoverable = true,
                    sourceUri = uri.toString(),
                ),
            )
        }
    }

    private fun probe(operation: Int, version: String, prepared: PreparedMedia) {
        emit(operation, ImportUiState.Probing(version, prepared))
        val result = NativeMediaBridge.probe(prepared)
        if (!result.isSuccess) {
            emit(
                operation,
                ImportUiState.Error(
                    engineVersion = version,
                    title = "Video analysis failed",
                    message = result.message,
                    code = result.code,
                    diagnostics = result.ffmpegError,
                    recoverable = result.recoverable,
                    sourceUri = prepared.sourceUri,
                    preparedMedia = prepared,
                ),
            )
            return
        }

        val previousPath = activeWorkingPath
        activeWorkingPath = prepared.workingFilePath
        if (previousPath != null && previousPath != activeWorkingPath) {
            sourcePreparer.deleteWorkingFile(previousPath)
        }
        emit(
            operation,
            ImportUiState.Ready(
                engineVersion = version,
                mediaInfo = checkNotNull(result.value),
            ),
        )
    }

    private fun emit(operation: Int, state: ImportUiState) {
        mainHandler.post {
            if (!closed && operation == generation.get()) {
                currentState = state
                onStateChanged(state)
            }
        }
    }

    private fun emitCurrent(state: ImportUiState) {
        if (closed) {
            return
        }
        currentState = state
        onStateChanged(state)
    }

    private fun preparationTitle(code: NativeErrorCode): String = when (code) {
        NativeErrorCode.STORAGE_FULL -> "Not enough storage"
        NativeErrorCode.CANCELLED -> "Preparation cancelled"
        NativeErrorCode.INVALID_INPUT -> "Video is unavailable"
        else -> "Video preparation failed"
    }

    override fun close() {
        closed = true
        generation.incrementAndGet()
        mainHandler.removeCallbacksAndMessages(null)
        worker.shutdownNow()
    }

    companion object {
        private const val PROGRESS_UPDATE_INTERVAL_MS = 120L
    }
}
