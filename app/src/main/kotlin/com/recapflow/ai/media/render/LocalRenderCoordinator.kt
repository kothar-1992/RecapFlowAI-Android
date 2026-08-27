package com.recapflow.ai.media.render

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.MainThread
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.recapflow.ai.media.MediaInfo
import com.recapflow.ai.media.edit.AudioCompiler
import com.recapflow.ai.media.edit.EditPlan
import com.recapflow.ai.media.edit.EditPlanValidator
import com.recapflow.ai.media.edit.TransformCompiler
import java.io.Closeable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.Future

@UnstableApi
class LocalRenderCoordinator(
    context: Context,
    private val onStateChanged: (RenderUiState) -> Unit,
) : Closeable {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val progressHolder = ProgressHolder()
    private val preparationExecutor = Executors.newSingleThreadExecutor()

    private var transformer: Transformer? = null
    private var preparationFuture: Future<*>? = null
    private var activeFreezeFrame: File? = null
    private var activeOutput: File? = null
    private var activePreset: RenderPreset? = null
    private var activeQualityRequest: RenderQualityRequest? = null
    private var activeSourceDurationMs = 0L
    private var activePlannedDurationMs = 0L
    private var activeExpectedAudio = true
    private var activeExpectedWidth: Int? = null
    private var activeExpectedHeight: Int? = null
    private var startedAtMs = 0L
    private var closed = false

    var currentState: RenderUiState = RenderUiState.Idle(detectCapability())
        private set

    @MainThread
    fun start(mediaInfo: MediaInfo, editPlan: EditPlan) {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (closed || transformer != null || preparationFuture != null) {
            return
        }

        val preset = editPlan.exportPreset
        val qualityRequest = RenderQualityPolicy.forSource(mediaInfo, preset)
        val planIssues = EditPlanValidator.validate(editPlan)
        if (mediaInfo.workingFilePath != editPlan.sourcePath || planIssues.isNotEmpty()) {
            emit(
                RenderUiState.Failed(
                    preset = preset,
                    message = planIssues.firstOrNull()?.description ?: "Edit plan source does not match",
                    diagnostics = buildString {
                        if (mediaInfo.workingFilePath != editPlan.sourcePath) {
                            append("sourceMismatch")
                        }
                        if (planIssues.isNotEmpty()) {
                            if (isNotEmpty()) append("; ")
                            append(planIssues.joinToString { it.name })
                        }
                    },
                    elapsedMs = 0L,
                ),
            )
            return
        }

        val compiledAudio = AudioCompiler.compile(editPlan.audio)
        val removeAudio = compiledAudio?.removeAudio == true
        val replacementAudio = compiledAudio?.replacement
        if (!mediaInfo.hasAudio && !removeAudio) {
            emit(
                RenderUiState.Failed(
                    preset = preset,
                    message = if (compiledAudio?.mixesSourceAudio == true) {
                        "Mix requires a source video with original audio"
                    } else {
                        "The selected H.264 + AAC export requires a source with an audio stream"
                    },
                    diagnostics = "sourceAudio=false; mix=${compiledAudio?.mixesSourceAudio == true}",
                    elapsedMs = 0L,
                ),
            )
            return
        }

        val compositionPlan = Media3CompositionPlanCompiler.compile(mediaInfo, editPlan)

        val capability = detectCapability(needsAudio = !removeAudio || replacementAudio != null)
        if (!capability.available) {
            emit(
                RenderUiState.Failed(
                    preset = preset,
                    message = capability.reason ?: "Required media encoder is unavailable",
                    diagnostics = "H.264=${capability.videoEncoderName}; " +
                        when {
                            compiledAudio?.mixesSourceAudio == true ->
                                "mixAAC=${capability.audioEncoderName}"
                            replacementAudio != null ->
                                "replacementAAC=${capability.audioEncoderName}"
                            removeAudio -> "audio=removed"
                            else -> "AAC=${capability.audioEncoderName}"
                        },
                    elapsedMs = 0L,
                ),
            )
            return
        }

        val input = File(mediaInfo.workingFilePath)
        if (!input.isFile || !input.canRead()) {
            emit(
                RenderUiState.Failed(
                    preset = preset,
                    message = "The prepared source video is no longer available",
                    diagnostics = input.absolutePath,
                    elapsedMs = 0L,
                ),
            )
            return
        }

        if (replacementAudio != null) {
            val replacementInput = File(replacementAudio.workingFilePath)
            if (!replacementInput.isFile || !replacementInput.canRead()) {
                emit(
                    RenderUiState.Failed(
                        preset = preset,
                        message = "The selected external audio is no longer available",
                        diagnostics = replacementInput.absolutePath,
                        elapsedMs = 0L,
                    ),
                )
                return
            }
        }

        val output = try {
            createOutputFile(preset)
        } catch (error: RuntimeException) {
            emit(
                RenderUiState.Failed(
                    preset = preset,
                    message = "Could not create the private export directory",
                    diagnostics = error.message ?: error.javaClass.simpleName,
                    elapsedMs = 0L,
                ),
            )
            return
        }
        if (output.exists() && !output.delete()) {
            emit(
                RenderUiState.Failed(
                    preset = preset,
                    message = "Could not prepare the output file",
                    diagnostics = output.absolutePath,
                    elapsedMs = 0L,
                ),
            )
            return
        }

        activeOutput = output
        activePreset = preset
        activeQualityRequest = qualityRequest
        activeSourceDurationMs = mediaInfo.durationMs
        activePlannedDurationMs = compositionPlan.plannedDurationMs
        activeExpectedAudio = compositionPlan.outputHasAudio
        TransformCompiler.compile(editPlan.transform, preset).let { transform ->
            activeExpectedWidth = transform?.targetWidth
            activeExpectedHeight = transform?.targetHeight
        }
        startedAtMs = SystemClock.elapsedRealtime()
        emit(RenderUiState.Preparing(preset, output.absolutePath))

        if (compositionPlan.freeze == null) {
            startTransformer(
                mediaInfo,
                editPlan,
                input,
                output,
                freezeFrame = null,
                qualityRequest = qualityRequest,
                compositionPlan = compositionPlan,
            )
        } else {
            preparationFuture = preparationExecutor.submit {
                val frameResult = runCatching {
                    FreezeFrameAssetFactory.create(
                        context = appContext,
                        mediaInfo = mediaInfo,
                        positionMs = compositionPlan.freeze.sourceFrameTimeMs,
                        preset = preset,
                    )
                }
                mainHandler.post {
                    preparationFuture = null
                    if (closed || activeOutput != output) {
                        frameResult.getOrNull()?.delete()
                        return@post
                    }
                    frameResult.fold(
                        onSuccess = { frame ->
                            activeFreezeFrame = frame
                            startTransformer(
                                mediaInfo,
                                editPlan,
                                input,
                                output,
                                frame,
                                qualityRequest,
                                compositionPlan,
                            )
                        },
                        onFailure = ::finishFailure,
                    )
                }
            }
        }
    }

    @MainThread
    private fun startTransformer(
        mediaInfo: MediaInfo,
        editPlan: EditPlan,
        input: File,
        output: File,
        freezeFrame: File?,
        qualityRequest: RenderQualityRequest,
        compositionPlan: Media3CompositionPlan,
    ) {
        try {
            val compiledComposition = Media3CompositionCompiler.compile(
                mediaInfo = mediaInfo,
                editPlan = editPlan,
                input = input,
                freezeFrame = freezeFrame,
                plan = compositionPlan,
            )

            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    finishSuccess(exportResult)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    finishFailure(exportException)
                }
            }

            val encoderFactory = DefaultEncoderFactory.Builder(appContext)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder()
                        .setBitrate(qualityRequest.requestedVideoBitrate)
                        .setBitrateMode(
                            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR,
                        )
                        .build(),
                )
                .setEnableFallback(true)
                .build()

            Log.i(
                RENDER_LOG_TAG,
                "Starting ${editPlan.exportPreset.displayName} H.264 export; " +
                    "requestedVideoBitrate=${qualityRequest.requestedVideoBitrate}; " +
                    "bitrateMode=CBR; " +
                    "sourceShortSide=${qualityRequest.sourceShortSidePixels}; " +
                    "upscale=${qualityRequest.isUpscaling}; " +
                    "composition=${compiledComposition.plan.summary}",
            )

            transformer = Transformer.Builder(appContext)
                .setEncoderFactory(encoderFactory)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .apply {
                    if (compiledComposition.plan.outputHasAudio) {
                        setAudioMimeType(MimeTypes.AUDIO_AAC)
                    }
                }
                .addListener(listener)
                .build()

            transformer?.start(compiledComposition.composition, output.absolutePath)
            pollProgress()
        } catch (error: RuntimeException) {
            finishFailure(error)
        }
    }

    @MainThread
    fun cancel() {
        check(Looper.myLooper() == Looper.getMainLooper())
        val preset = activePreset ?: return
        val elapsed = elapsedMs()
        mainHandler.removeCallbacks(progressRunnable)
        preparationFuture?.cancel(true)
        preparationFuture = null
        transformer?.cancel()
        transformer = null
        activeOutput?.deleteIncompleteOutput()
        deleteActiveFreezeFrame()
        activeOutput = null
        activePreset = null
        activeQualityRequest = null
        activeSourceDurationMs = 0L
        activePlannedDurationMs = 0L
        activeExpectedAudio = true
        activeExpectedWidth = null
        activeExpectedHeight = null
        emit(RenderUiState.Cancelled(preset, elapsed))
    }

    @MainThread
    fun reset(mediaHasAudio: Boolean = true) {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (transformer != null || preparationFuture != null) {
            cancel()
        }
        val capability = if (mediaHasAudio) {
            detectCapability(needsAudio = true)
        } else {
            RenderCapability(
                available = false,
                reason = "The selected H.264 + AAC export requires a source with an audio stream",
            )
        }
        emit(RenderUiState.Idle(capability))
    }

    private fun pollProgress() {
        mainHandler.removeCallbacks(progressRunnable)
        mainHandler.post(progressRunnable)
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            val activeTransformer = transformer ?: return
            val preset = activePreset ?: return
            val output = activeOutput ?: return
            val state = activeTransformer.getProgress(progressHolder)
            val percent = if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                progressHolder.progress.coerceIn(0, 100)
            } else {
                null
            }
            if (percent != null && percent >= FINALIZING_PROGRESS_PERCENT) {
                emit(RenderUiState.Finalizing(preset, output.absolutePath, elapsedMs()))
            } else {
                emit(
                    RenderUiState.Rendering(
                        preset = preset,
                        outputPath = output.absolutePath,
                        progressPercent = percent,
                        elapsedMs = elapsedMs(),
                    ),
                )
            }
            mainHandler.postDelayed(this, PROGRESS_POLL_INTERVAL_MS)
        }
    }

    private fun finishSuccess(result: ExportResult) {
        mainHandler.removeCallbacks(progressRunnable)
        val preset = activePreset ?: return
        val output = activeOutput ?: return
        val sourceDurationMs = activeSourceDurationMs
        val plannedDurationMs = activePlannedDurationMs
        val expectedAudio = activeExpectedAudio
        val expectedWidth = activeExpectedWidth
        val expectedHeight = activeExpectedHeight
        val qualityRequest = activeQualityRequest
        transformer = null

        val actualSize = output.takeIf(File::isFile)?.length() ?: 0L
        if (actualSize <= 0L) {
            val elapsed = elapsedMs()
            output.deleteIncompleteOutput()
            clearActiveRender()
            emit(
                RenderUiState.Failed(
                    preset = preset,
                    message = "Render finished without a playable output file",
                    diagnostics = output.absolutePath,
                    elapsedMs = elapsed,
                ),
            )
            return
        }

        // Resolution, codec, duration and audio-policy validation is intentionally performed
        // after Transformer completion but before Completed/public export. A device encoder may
        // reject requested settings; an unexpected fallback must never be labelled as the user's
        // selected 720p, 1080p or 2K output.
        preparationFuture = preparationExecutor.submit {
            val inspected = runCatching { RenderedOutputInspector.inspect(output) }
            mainHandler.post {
                preparationFuture = null
                if (closed || activeOutput != output) return@post
                inspected.fold(
                    onSuccess = { metadata ->
                        val validation = RenderedOutputValidationPolicy.validate(
                            metadata = metadata,
                            preset = preset,
                            expectedDurationMs = plannedDurationMs,
                            expectedAudio = expectedAudio,
                            expectedWidth = expectedWidth,
                            expectedHeight = expectedHeight,
                            requestedVideoBitrate = qualityRequest?.requestedVideoBitrate
                                ?: preset.minimumVideoBitrate,
                            averageVideoBitrate = result.averageVideoBitrate.takeIf { it > 0 },
                        )
                        if (validation.isValid) {
                            finishValidatedSuccess(
                                result = result,
                                output = output,
                                preset = preset,
                                sourceDurationMs = sourceDurationMs,
                                plannedDurationMs = plannedDurationMs,
                                qualityRequest = qualityRequest,
                                metadata = metadata,
                                validation = validation,
                            )
                        } else {
                            finishValidationFailure(
                                output = output,
                                preset = preset,
                                errors = validation.errors,
                            )
                        }
                    },
                    onFailure = { error ->
                        finishValidationFailure(
                            output = output,
                            preset = preset,
                            errors = listOf(error.message ?: error.javaClass.simpleName),
                        )
                    },
                )
            }
        }
    }

    private fun finishValidatedSuccess(
        result: ExportResult,
        output: File,
        preset: RenderPreset,
        sourceDurationMs: Long,
        plannedDurationMs: Long,
        qualityRequest: RenderQualityRequest?,
        metadata: RenderedOutputMetadata,
        validation: RenderedOutputValidation,
    ) {
        val actualSize = output.length()
        clearActiveRender()

        Log.i(
            RENDER_LOG_TAG,
            "Validated ${preset.displayName} H.264 export; " +
                "displayDimensions=${metadata.displayWidth}x${metadata.displayHeight}; " +
                    "codedDimensions=${metadata.width}x${metadata.height}; " +
                    "rotation=${metadata.rotationDegrees}; durationMs=${metadata.durationMs}; " +
                "requestedVideoBitrate=${qualityRequest?.requestedVideoBitrate}; " +
                "averageVideoBitrate=${result.averageVideoBitrate}; " +
                "bytes=$actualSize; encoder=${result.videoEncoderName}",
        )

        emit(
            RenderUiState.Completed(
                preset = preset,
                outputPath = output.absolutePath,
                elapsedMs = elapsedMs(),
                sourceDurationMs = sourceDurationMs,
                plannedDurationMs = plannedDurationMs,
                outputSizeBytes = result.fileSizeBytes.takeIf { it > 0L } ?: actualSize,
                videoEncoderName = result.videoEncoderName,
                requestedVideoBitrate = qualityRequest?.requestedVideoBitrate
                    ?: preset.minimumVideoBitrate,
                averageVideoBitrate = result.averageVideoBitrate.takeIf { it > 0 },
                sourceShortSidePixels = qualityRequest?.sourceShortSidePixels ?: 0,
                sourceWasUpscaled = qualityRequest?.isUpscaling == true,
                sourceWasPreviousRecapFlowExport =
                    qualityRequest?.isPreviousRecapFlowExport == true,
                outputWidth = metadata.displayWidth,
                outputHeight = metadata.displayHeight,
                outputDurationMs = metadata.durationMs,
                outputHasAudio = metadata.audioMimeType != null,
                validationWarnings = validation.warnings,
            ),
        )
    }

    private fun finishValidationFailure(
        output: File,
        preset: RenderPreset,
        errors: List<String>,
    ) {
        val elapsed = elapsedMs()
        output.deleteIncompleteOutput()
        clearActiveRender()
        emit(
            RenderUiState.Failed(
                preset = preset,
                message = "Rendered file did not match the selected ${preset.displayName} quality",
                diagnostics = errors.joinToString(separator = "; "),
                elapsedMs = elapsed,
            ),
        )
    }

    private fun clearActiveRender() {
        transformer = null
        activePreset = null
        activeQualityRequest = null
        activeOutput = null
        activeSourceDurationMs = 0L
        activePlannedDurationMs = 0L
        activeExpectedAudio = true
        activeExpectedWidth = null
        activeExpectedHeight = null
        deleteActiveFreezeFrame()
    }

    private fun finishFailure(error: Throwable) {
        mainHandler.removeCallbacks(progressRunnable)
        val preset = activePreset ?: return
        val output = activeOutput
        transformer = null
        activePreset = null
        activeQualityRequest = null
        activeOutput = null
        activeSourceDurationMs = 0L
        activePlannedDurationMs = 0L
        activeExpectedAudio = true
        activeExpectedWidth = null
        activeExpectedHeight = null
        output?.deleteIncompleteOutput()
        deleteActiveFreezeFrame()

        val exportError = error as? ExportException
        emit(
            RenderUiState.Failed(
                preset = preset,
                message = error.message ?: "Local render failed",
                diagnostics = buildString {
                    if (exportError != null) {
                        append(exportError.errorCodeName)
                        exportError.codecInfo?.name?.let { append("; codec=").append(it) }
                    } else {
                        append(error.javaClass.simpleName)
                    }
                },
                elapsedMs = elapsedMs(),
            ),
        )
    }

    private fun createOutputFile(preset: RenderPreset): File {
        val root = appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(appContext.filesDir, "exports")
        check(root.exists() || root.mkdirs()) { "Could not create the export directory" }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(root, "RecapFlow_${preset.displayName}_$timestamp.mp4")
    }

    private fun detectCapability(needsAudio: Boolean = true): RenderCapability {
        return try {
            val encoders = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
                .filter { it.isEncoder }
            val videoEncoder = encoders.firstOrNull { it.supportsMime(VIDEO_AVC_MIME) }
            val audioEncoder = encoders.firstOrNull { it.supportsMime(AUDIO_AAC_MIME) }
            when {
                videoEncoder == null -> RenderCapability(
                    available = false,
                    reason = "This device does not expose an H.264 encoder",
                )
                needsAudio && audioEncoder == null -> RenderCapability(
                    available = false,
                    videoEncoderName = videoEncoder.name,
                    reason = "This device does not expose an AAC encoder",
                )
                else -> RenderCapability(
                    available = true,
                    videoEncoderName = videoEncoder.name,
                    audioEncoderName = audioEncoder?.name,
                )
            }
        } catch (error: RuntimeException) {
            RenderCapability(
                available = false,
                reason = error.message ?: "Could not inspect device encoders",
            )
        }
    }

    private fun MediaCodecInfo.supportsMime(mimeType: String): Boolean =
        supportedTypes.any { it.equals(mimeType, ignoreCase = true) }

    private fun File.deleteIncompleteOutput() {
        if (isFile && extension.equals("mp4", ignoreCase = true)) {
            delete()
        }
    }

    private fun deleteActiveFreezeFrame() {
        activeFreezeFrame?.takeIf(File::isFile)?.delete()
        activeFreezeFrame = null
    }

    private fun elapsedMs(): Long =
        (SystemClock.elapsedRealtime() - startedAtMs).coerceAtLeast(0L)

    private fun emit(state: RenderUiState) {
        if (!closed) {
            currentState = state
            onStateChanged(state)
        }
    }

    override fun close() {
        if (closed) {
            return
        }
        if (transformer != null || preparationFuture != null) {
            preparationFuture?.cancel(true)
            transformer?.cancel()
            activeOutput?.deleteIncompleteOutput()
        }
        preparationFuture = null
        deleteActiveFreezeFrame()
        preparationExecutor.shutdownNow()
        closed = true
        transformer = null
        activeOutput = null
        activePreset = null
        activeQualityRequest = null
        activeSourceDurationMs = 0L
        activePlannedDurationMs = 0L
        activeExpectedAudio = true
        activeExpectedWidth = null
        activeExpectedHeight = null
        mainHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val VIDEO_AVC_MIME = "video/avc"
        private const val AUDIO_AAC_MIME = "audio/mp4a-latm"
        private const val RENDER_LOG_TAG = "RecapFlowRender"
        private const val FINALIZING_PROGRESS_PERCENT = 99
        private const val PROGRESS_POLL_INTERVAL_MS = 350L
    }
}
