package com.recapflow.ai.media.export

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.Future

class PublicExportCoordinator(
    context: Context,
    private val onStateChanged: (PublicExportUiState) -> Unit,
) : Closeable {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var activeFuture: Future<*>? = null
    private var generation = 0L
    private var lastHandledSourcePath: String? = null
    private var closed = false

    var currentState: PublicExportUiState = PublicExportUiState.Idle
        private set

    fun needsLegacyWritePermission(): Boolean =
        Build.VERSION.SDK_INT == Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED

    @MainThread
    fun publish(sourcePath: String, force: Boolean = false) {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (closed || (!force && lastHandledSourcePath == sourcePath)) return

        cancelActiveWork()
        lastHandledSourcePath = sourcePath
        val source = File(sourcePath)
        val displayName = PublicExportNamePolicy.displayName(source.name)
        if (!source.isFile || !source.canRead() || source.length() <= 0L) {
            emit(
                PublicExportUiState.Failed(
                    sourcePath = sourcePath,
                    displayName = displayName,
                    message = "The completed private render is no longer readable.",
                ),
            )
            return
        }
        if (needsLegacyWritePermission()) {
            emit(PublicExportUiState.PermissionRequired(sourcePath, displayName))
            return
        }

        val requestGeneration = ++generation
        emit(PublicExportUiState.Publishing(sourcePath, displayName))
        activeFuture = executor.submit {
            val result = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    publishWithMediaStore(source, displayName)
                } else {
                    publishLegacy(source, displayName, requestGeneration)
                }
            }
            mainHandler.post {
                if (closed || requestGeneration != generation) return@post
                activeFuture = null
                result.fold(
                    onSuccess = ::emit,
                    onFailure = { error ->
                        if (error !is CancellationException) {
                            emit(
                                PublicExportUiState.Failed(
                                    sourcePath = sourcePath,
                                    displayName = displayName,
                                    message = error.message ?: "Could not publish the video.",
                                ),
                            )
                        }
                    },
                )
            }
        }
    }

    @MainThread
    fun permissionDenied() {
        val pending = currentState as? PublicExportUiState.PermissionRequired ?: return
        emit(
            PublicExportUiState.Failed(
                sourcePath = pending.sourcePath,
                displayName = pending.displayName,
                message = "Storage permission was not granted. The private render is still safe.",
            ),
        )
    }

    @MainThread
    fun reset() {
        check(Looper.myLooper() == Looper.getMainLooper())
        cancelActiveWork()
        lastHandledSourcePath = null
        emit(PublicExportUiState.Idle)
    }

    private fun publishWithMediaStore(
        source: File,
        displayName: String,
    ): PublicExportUiState.Published {
        val resolver = appContext.contentResolver
        var insertedUri: Uri? = null
        var finalized = false
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, VIDEO_MIME_TYPE)
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/$PUBLIC_DIRECTORY",
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val destinationUri = resolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values,
            )
                ?: throw IOException("Android could not create a public video entry.")
            insertedUri = destinationUri
            resolver.openOutputStream(destinationUri, "w")?.use { output ->
                FileInputStream(source).use { input ->
                    copyCancellable(input, output, source.length())
                }
            } ?: throw IOException("Android could not open the public video destination.")
            throwIfCancelled()
            val finalizedRows = resolver.update(
                destinationUri,
                ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
                null,
                null,
            )
            if (finalizedRows <= 0) {
                throw IOException("Android could not finalize the public video entry.")
            }
            finalized = true
            return PublicExportUiState.Published(
                sourcePath = source.absolutePath,
                contentUri = destinationUri,
                displayName = displayName,
                publicLocation = "${Environment.DIRECTORY_MOVIES}/$PUBLIC_DIRECTORY/$displayName",
            )
        } finally {
            if (!finalized) {
                insertedUri?.let { uri -> runCatching { resolver.delete(uri, null, null) } }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun publishLegacy(
        source: File,
        displayName: String,
        requestGeneration: Long,
    ): PublicExportUiState.Published {
        val moviesDirectory = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES,
        )
        val publicDirectory = File(moviesDirectory, PUBLIC_DIRECTORY)
        if (!publicDirectory.exists() && !publicDirectory.mkdirs()) {
            throw IOException("Could not create Movies/$PUBLIC_DIRECTORY.")
        }
        val target = uniqueLegacyTarget(publicDirectory, displayName)
        val pending = File(
            publicDirectory,
            PublicExportNamePolicy.pendingName(target.name, requestGeneration),
        )
        try {
            FileInputStream(source).use { input ->
                FileOutputStream(pending).use { output ->
                    copyCancellable(input, output, source.length())
                    output.flush()
                    output.fd.sync()
                }
            }
            throwIfCancelled()
            if (!pending.renameTo(target)) {
                throw IOException("Could not finalize ${target.name} in Movies/$PUBLIC_DIRECTORY.")
            }
        } finally {
            if (pending.exists()) pending.delete()
        }

        MediaScannerConnection.scanFile(
            appContext,
            arrayOf(target.absolutePath),
            arrayOf(VIDEO_MIME_TYPE),
            null,
        )
        val contentUri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            target,
        )
        return PublicExportUiState.Published(
            sourcePath = source.absolutePath,
            contentUri = contentUri,
            displayName = target.name,
            publicLocation = "${Environment.DIRECTORY_MOVIES}/$PUBLIC_DIRECTORY/${target.name}",
        )
    }

    private fun uniqueLegacyTarget(directory: File, displayName: String): File {
        var candidate = File(directory, displayName)
        var copyIndex = 1
        while (candidate.exists()) {
            candidate = File(
                directory,
                PublicExportNamePolicy.collisionName(displayName, copyIndex++),
            )
        }
        return candidate
    }

    private fun copyCancellable(
        input: FileInputStream,
        output: java.io.OutputStream,
        expectedBytes: Long,
    ) {
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        var copiedBytes = 0L
        while (true) {
            throwIfCancelled()
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            copiedBytes += count
        }
        if (copiedBytes != expectedBytes || copiedBytes <= 0L) {
            throw IOException("The public copy did not match the completed render.")
        }
    }

    private fun throwIfCancelled() {
        if (Thread.currentThread().isInterrupted) throw CancellationException()
    }

    @MainThread
    private fun cancelActiveWork() {
        generation += 1L
        activeFuture?.cancel(true)
        activeFuture = null
    }

    @MainThread
    private fun emit(state: PublicExportUiState) {
        currentState = state
        onStateChanged(state)
    }

    override fun close() {
        if (closed) return
        closed = true
        generation += 1L
        activeFuture?.cancel(true)
        activeFuture = null
        executor.shutdownNow()
        mainHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        const val PUBLIC_DIRECTORY = "RecapFlowAI"
        private const val VIDEO_MIME_TYPE = "video/mp4"
        private const val COPY_BUFFER_BYTES = 1024 * 1024
    }
}
