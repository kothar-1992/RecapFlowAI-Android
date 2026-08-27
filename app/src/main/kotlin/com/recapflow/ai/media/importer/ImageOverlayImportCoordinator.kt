package com.recapflow.ai.media.importer

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.OpenableColumns
import com.recapflow.ai.media.edit.ImageOverlayAsset
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

sealed interface ImageOverlayImportState {
    data object Idle : ImageOverlayImportState
    data class Importing(val displayName: String) : ImageOverlayImportState
    data class Ready(val asset: ImageOverlayAsset) : ImageOverlayImportState
    data class Error(val message: String) : ImageOverlayImportState
}

/** Copies and validates one user-selected static image inside the private edit workspace. */
class ImageOverlayImportCoordinator(
    context: Context,
    private val onStateChanged: (ImageOverlayImportState) -> Unit,
) : Closeable {

    private val appContext = context.applicationContext
    private val imageDirectory = File(appContext.cacheDir, IMAGE_DIRECTORY)
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val generation = AtomicInteger(0)

    @Volatile
    private var closed = false

    @Volatile
    var currentState: ImageOverlayImportState = ImageOverlayImportState.Idle
        private set

    private var activeWorkingPath: String? = null

    fun import(uri: Uri) {
        val operation = generation.incrementAndGet()
        worker.execute {
            val previousPath = activeWorkingPath
            runCatching { prepare(uri, operation) }
                .onSuccess { asset ->
                    if (closed || generation.get() != operation) {
                        deleteWorkingFile(asset.workingFilePath)
                        return@onSuccess
                    }
                    if (previousPath != asset.workingFilePath) deleteWorkingFile(previousPath)
                    activeWorkingPath = asset.workingFilePath
                    emit(operation, ImageOverlayImportState.Ready(asset))
                }
                .onFailure { error ->
                    emit(
                        operation,
                        ImageOverlayImportState.Error(
                            error.message ?: "The selected image could not be prepared",
                        ),
                    )
                }
        }
    }

    fun restore(asset: ImageOverlayAsset?): ImageOverlayAsset? {
        asset ?: return null
        return runCatching {
            ensureImageDirectory()
            val file = File(asset.workingFilePath)
            if (
                file.parentFile?.canonicalFile != imageDirectory.canonicalFile ||
                !file.isFile ||
                file.length() <= 0L
            ) {
                return@runCatching null
            }
            val metadata = readImageMetadata(file)
            asset.copy(
                workingFilePath = file.absolutePath,
                displayName = asset.displayName.ifBlank { file.name },
                mimeType = metadata.mimeType,
                pixelWidth = metadata.width,
                pixelHeight = metadata.height,
                fileSizeBytes = file.length(),
            )
        }.getOrNull()?.also { restored ->
            activeWorkingPath = restored.workingFilePath
            currentState = ImageOverlayImportState.Ready(restored)
        }
    }

    fun clear(asset: ImageOverlayAsset?) {
        generation.incrementAndGet()
        deleteWorkingFile(asset?.workingFilePath)
        activeWorkingPath = null
        currentState = ImageOverlayImportState.Idle
        if (!closed) onStateChanged(currentState)
    }

    private fun prepare(uri: Uri, operation: Int): ImageOverlayAsset {
        ensureImageDirectory()
        val source = querySource(uri)
        if (source.sizeBytes != null && source.sizeBytes > MAX_SOURCE_BYTES) {
            throw IOException("Choose an image smaller than 20 MB")
        }
        ensureSpace(source.sizeBytes)
        emit(operation, ImageOverlayImportState.Importing(source.displayName))

        val extension = source.displayName.substringAfterLast('.', "")
            .lowercase()
            .takeIf { it in SUPPORTED_EXTENSIONS }
            ?.let { ".$it" }
            .orEmpty()
        val baseName = UUID.randomUUID().toString()
        val partial = File(imageDirectory, "$baseName.part")
        val completed = File(imageDirectory, "$baseName$extension")
        try {
            val input = appContext.contentResolver.openInputStream(uri)
                ?: throw FileNotFoundException("The selected provider returned no image data")
            input.use { sourceStream ->
                FileOutputStream(partial).use { destination ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    var copied = 0L
                    while (true) {
                        if (Thread.currentThread().isInterrupted) {
                            throw InterruptedIOException("Image preparation was cancelled")
                        }
                        val count = sourceStream.read(buffer)
                        if (count < 0) break
                        copied += count
                        if (copied > MAX_SOURCE_BYTES) {
                            throw IOException("Choose an image smaller than 20 MB")
                        }
                        destination.write(buffer, 0, count)
                    }
                    destination.fd.sync()
                }
            }
            if (partial.length() <= 0L) throw IOException("The selected image is empty")
            if (!partial.renameTo(completed)) throw IOException("Could not finalize the image")
            val metadata = readImageMetadata(completed)
            return ImageOverlayAsset(
                workingFilePath = completed.absolutePath,
                displayName = source.displayName,
                mimeType = metadata.mimeType,
                pixelWidth = metadata.width,
                pixelHeight = metadata.height,
                fileSizeBytes = completed.length(),
            )
        } catch (error: SecurityException) {
            throw IOException("RecapFlowAI no longer has permission to read this image", error)
        } finally {
            partial.delete()
            if (completed.isFile && runCatching { readImageMetadata(completed) }.isFailure) {
                completed.delete()
            }
        }
    }

    private fun readImageMetadata(file: File): ImageMetadata {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        val mimeType = options.outMimeType?.lowercase().orEmpty()
        if (
            options.outWidth <= 0 ||
            options.outHeight <= 0 ||
            mimeType !in SUPPORTED_MIME_TYPES
        ) {
            throw IOException("Choose a readable PNG, JPEG, or WebP image")
        }
        if (options.outWidth > MAX_PIXEL_SIDE || options.outHeight > MAX_PIXEL_SIDE) {
            throw IOException("Choose an image no larger than 8192 pixels per side")
        }
        return ImageMetadata(options.outWidth, options.outHeight, mimeType)
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

    private fun ensureImageDirectory() {
        if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
            throw IOException("The private image-overlay workspace could not be created")
        }
    }

    private fun ensureSpace(sourceSize: Long?) {
        sourceSize ?: return
        if (StatFs(imageDirectory.absolutePath).availableBytes < sourceSize + MIN_FREE_BYTES) {
            throw IOException("Not enough free storage to prepare this image")
        }
    }

    private fun deleteWorkingFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path)
            if (file.parentFile?.canonicalFile == imageDirectory.canonicalFile) file.delete()
        }
    }

    private fun emit(operation: Int, state: ImageOverlayImportState) {
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

    private data class SourceDescription(val displayName: String, val sizeBytes: Long?)
    private data class ImageMetadata(val width: Int, val height: Int, val mimeType: String)

    companion object {
        private const val IMAGE_DIRECTORY = "image_overlays"
        private const val COPY_BUFFER_BYTES = 128 * 1024
        private const val MIN_FREE_BYTES = 8L * 1024L * 1024L
        private const val MAX_SOURCE_BYTES = 20L * 1024L * 1024L
        private const val MAX_PIXEL_SIDE = 8_192
        private const val DEFAULT_DISPLAY_NAME = "Selected image"
        private val SUPPORTED_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")
        private val SUPPORTED_MIME_TYPES = setOf("image/png", "image/jpeg", "image/webp")
    }
}
