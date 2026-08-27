package com.recapflow.ai.media.importer

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import com.recapflow.ai.media.NativeErrorCode
import com.recapflow.ai.media.PreparedMedia
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.util.UUID

internal class MediaSourcePreparer(context: Context) {

    private val applicationContext = context.applicationContext
    private val importDirectory = File(applicationContext.cacheDir, IMPORT_DIRECTORY)

    fun prepare(
        uri: Uri,
        onSourceResolved: (displayName: String, totalBytes: Long?) -> Unit,
        onProgress: (copiedBytes: Long, totalBytes: Long?) -> Unit,
    ): PreparedMedia {
        ensureImportDirectory()
        val source = try {
            querySource(uri)
        } catch (error: SecurityException) {
            throw MediaPreparationException(
                code = NativeErrorCode.INVALID_INPUT,
                message = "RecapFlowAI does not have permission to read this video",
                recoverable = true,
                cause = error,
            )
        }
        ensureSpace(source.sizeBytes)
        onSourceResolved(source.displayName, source.sizeBytes)

        val extension = source.displayName
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
            .takeIf { it.matches(EXTENSION_PATTERN) }
            ?.let { ".$it" }
            .orEmpty()
        val baseName = UUID.randomUUID().toString()
        val partialFile = File(importDirectory, "$baseName.part")
        val completedFile = File(importDirectory, "$baseName$extension")

        try {
            val input = applicationContext.contentResolver.openInputStream(uri)
                ?: throw FileNotFoundException("The selected provider returned no data")
            input.use { sourceStream ->
                FileOutputStream(partialFile).use { destination ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    var copiedBytes = 0L
                    while (true) {
                        if (Thread.currentThread().isInterrupted) {
                            throw InterruptedIOException("Video preparation was cancelled")
                        }
                        val count = sourceStream.read(buffer)
                        if (count < 0) {
                            break
                        }
                        destination.write(buffer, 0, count)
                        copiedBytes += count
                        onProgress(copiedBytes, source.sizeBytes)
                    }
                    destination.fd.sync()
                }
            }

            if (!partialFile.renameTo(completedFile)) {
                throw IOException("Could not finalize the prepared video")
            }

            return PreparedMedia(
                sourceUri = uri.toString(),
                workingFilePath = completedFile.absolutePath,
                displayName = source.displayName,
                fileSizeBytes = completedFile.length(),
            )
        } catch (error: InterruptedIOException) {
            throw MediaPreparationException(
                code = NativeErrorCode.CANCELLED,
                message = "Video preparation was cancelled",
                recoverable = true,
                cause = error,
            )
        } catch (error: FileNotFoundException) {
            throw MediaPreparationException(
                code = NativeErrorCode.INVALID_INPUT,
                message = "The selected video can no longer be opened",
                recoverable = true,
                cause = error,
            )
        } catch (error: SecurityException) {
            throw MediaPreparationException(
                code = NativeErrorCode.INVALID_INPUT,
                message = "RecapFlowAI does not have permission to read this video",
                recoverable = true,
                cause = error,
            )
        } catch (error: IOException) {
            val storageLow = availableBytes() < MIN_FREE_BYTES
            throw MediaPreparationException(
                code = if (storageLow) {
                    NativeErrorCode.STORAGE_FULL
                } else {
                    NativeErrorCode.INPUT_COPY_FAILED
                },
                message = if (storageLow) {
                    "Not enough free storage to prepare this video"
                } else {
                    "The selected video could not be copied into the local workspace"
                },
                recoverable = true,
                cause = error,
            )
        } finally {
            if (partialFile.exists()) {
                partialFile.delete()
            }
        }
    }

    fun restore(
        sourceUri: String,
        workingFilePath: String,
        displayName: String,
    ): PreparedMedia? {
        return runCatching {
            ensureImportDirectory()
            val workingFile = File(workingFilePath)
            val canonicalParent = workingFile.parentFile?.canonicalFile
            if (canonicalParent != importDirectory.canonicalFile ||
                !workingFile.isFile ||
                workingFile.length() <= 0L
            ) {
                return@runCatching null
            }

            PreparedMedia(
                sourceUri = sourceUri,
                workingFilePath = workingFile.absolutePath,
                displayName = displayName.ifBlank { workingFile.name },
                fileSizeBytes = workingFile.length(),
            )
        }.getOrNull()
    }

    fun deleteWorkingFile(path: String?) {
        if (path.isNullOrBlank()) {
            return
        }
        runCatching {
            val workingFile = File(path)
            if (workingFile.parentFile?.canonicalFile == importDirectory.canonicalFile) {
                workingFile.delete()
            }
        }
    }

    private fun querySource(uri: Uri): SourceDescription {
        var displayName: String? = null
        var sizeBytes: Long? = null
        applicationContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    displayName = cursor.getString(nameIndex)
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    sizeBytes = cursor.getLong(sizeIndex).takeIf { it >= 0L }
                }
            }
        }

        return SourceDescription(
            displayName = displayName?.takeIf { it.isNotBlank() }
                ?: DEFAULT_DISPLAY_NAME,
            sizeBytes = sizeBytes,
        )
    }

    private fun ensureImportDirectory() {
        if (!importDirectory.exists() && !importDirectory.mkdirs()) {
            throw MediaPreparationException(
                code = NativeErrorCode.INPUT_COPY_FAILED,
                message = "The local media workspace could not be created",
                recoverable = true,
            )
        }
    }

    private fun ensureSpace(sourceSize: Long?) {
        if (sourceSize == null) {
            return
        }
        val requiredBytes = sourceSize + MIN_FREE_BYTES
        if (availableBytes() < requiredBytes) {
            throw MediaPreparationException(
                code = NativeErrorCode.STORAGE_FULL,
                message = "Not enough free storage to prepare this video",
                recoverable = true,
            )
        }
    }

    private fun availableBytes(): Long =
        runCatching { StatFs(importDirectory.absolutePath).availableBytes }
            .getOrDefault(Long.MAX_VALUE)

    private data class SourceDescription(
        val displayName: String,
        val sizeBytes: Long?,
    )

    companion object {
        private const val IMPORT_DIRECTORY = "media_imports"
        private const val COPY_BUFFER_BYTES = 256 * 1024
        private const val MIN_FREE_BYTES = 32L * 1024L * 1024L
        private const val DEFAULT_DISPLAY_NAME = "Selected video"
        private val EXTENSION_PATTERN = Regex("[a-z0-9]{1,8}")
    }
}

internal class MediaPreparationException(
    val code: NativeErrorCode,
    override val message: String,
    val recoverable: Boolean,
    cause: Throwable? = null,
) : Exception(message, cause)
