package com.recapflow.ai.media

object NativeMediaBridge {

    private val libraryLoadResult: Result<Unit> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runCatching { System.loadLibrary(LIBRARY_NAME) }
    }

    fun nativeVersion(): NativeResult<String> {
        libraryLoadFailure()?.let { return it }

        return try {
            NativeResult.success(
                value = nativeVersionFromJni(),
                stage = NativeStage.INITIALIZATION,
                message = "Native bridge is ready",
            )
        } catch (error: LinkageError) {
            NativeResult.failure(
                code = NativeErrorCode.NATIVE_LIBRARY_LOAD_FAILED,
                stage = NativeStage.INITIALIZATION,
                message = error.safeMessage(),
                recoverable = false,
            )
        } catch (error: RuntimeException) {
            NativeResult.failure(
                code = NativeErrorCode.NATIVE_ERROR,
                stage = NativeStage.INITIALIZATION,
                message = error.safeMessage(),
                recoverable = false,
            )
        }
    }

    fun probe(source: PreparedMedia): NativeResult<MediaInfo> {
        libraryLoadFailure()?.let { return it }

        return try {
            val payload = probeFromJni(source.workingFilePath)
            val code = NativeErrorCode.fromValue(payload.code)
            val stage = NativeStage.fromValue(payload.stage)
            if (code != NativeErrorCode.OK) {
                NativeResult.failure(
                    code = code,
                    stage = stage,
                    message = payload.message,
                    ffmpegError = payload.ffmpegError,
                    recoverable = payload.recoverable,
                )
            } else {
                NativeResult.success(
                    value = MediaInfo(
                        sourceUri = source.sourceUri,
                        workingFilePath = source.workingFilePath,
                        displayName = source.displayName,
                        fileSizeBytes = source.fileSizeBytes,
                        durationMs = payload.durationMs,
                        width = payload.width,
                        height = payload.height,
                        rotationDegrees = payload.rotationDegrees,
                        frameRate = payload.frameRate,
                        videoCodec = payload.videoCodec,
                        audioCodec = payload.audioCodec,
                        audioSampleRate = payload.audioSampleRate,
                        audioChannels = payload.audioChannels,
                        bitrate = payload.bitrate,
                        containerFormat = payload.containerFormat,
                    ),
                    stage = stage,
                    message = payload.message,
                )
            }
        } catch (error: LinkageError) {
            NativeResult.failure(
                code = NativeErrorCode.NATIVE_LIBRARY_LOAD_FAILED,
                stage = NativeStage.PROBE,
                message = error.safeMessage(),
                recoverable = false,
            )
        } catch (error: RuntimeException) {
            NativeResult.failure(
                code = NativeErrorCode.NATIVE_ERROR,
                stage = NativeStage.PROBE,
                message = error.safeMessage(),
                recoverable = true,
            )
        }
    }

    private external fun nativeVersionFromJni(): String

    private external fun probeFromJni(inputPath: String): NativeProbePayload

    private fun libraryLoadFailure(): NativeResult<Nothing>? {
        val loadFailure = libraryLoadResult.exceptionOrNull() ?: return null
        return NativeResult.failure(
            code = NativeErrorCode.NATIVE_LIBRARY_LOAD_FAILED,
            stage = NativeStage.INITIALIZATION,
            message = loadFailure.safeMessage(),
            recoverable = false,
        )
    }

    private fun Throwable.safeMessage(): String =
        message?.takeIf(String::isNotBlank) ?: javaClass.simpleName

    private const val LIBRARY_NAME = "flowai"
}
