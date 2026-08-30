package com.recapflow.ai

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.CompositionPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.recapflow.ai.databinding.ActivityMainBinding
import com.recapflow.ai.device.DeviceProfileReader
import com.recapflow.ai.media.MediaInfo
import com.recapflow.ai.media.edit.AdaptiveCutCompiler
import com.recapflow.ai.media.edit.AdaptiveCutDraftEngine
import com.recapflow.ai.media.edit.AdaptiveCutPreset
import com.recapflow.ai.media.edit.AdaptiveCutSettings
import com.recapflow.ai.media.edit.AspectRatioPreset
import com.recapflow.ai.media.edit.AudioCompiler
import com.recapflow.ai.media.edit.AudioPolicy
import com.recapflow.ai.media.edit.AudioSettings
import com.recapflow.ai.media.edit.BlurRectangle
import com.recapflow.ai.media.edit.ClipTransitionSettings
import com.recapflow.ai.media.edit.ColorSettings
import com.recapflow.ai.media.edit.CropCompiler
import com.recapflow.ai.media.edit.CropRectangle
import com.recapflow.ai.media.edit.CropSettings
import com.recapflow.ai.media.edit.DurationFitAdvisor
import com.recapflow.ai.media.edit.EditPlan
import com.recapflow.ai.media.edit.EditProfile
import com.recapflow.ai.media.edit.EditPlanIssue
import com.recapflow.ai.media.edit.EditPlanValidator
import com.recapflow.ai.media.edit.FreezeCompiler
import com.recapflow.ai.media.edit.FreezeSettings
import com.recapflow.ai.media.edit.ImageOverlayAsset
import com.recapflow.ai.media.edit.ImageOverlayPositionPreset
import com.recapflow.ai.media.edit.ImageOverlaySettings
import com.recapflow.ai.media.edit.OverlayCompiler
import com.recapflow.ai.media.edit.OverlaySettings
import com.recapflow.ai.media.edit.PreviewAspectOwner
import com.recapflow.ai.media.edit.PreviewAspectPolicy
import com.recapflow.ai.media.edit.ReplacementAudioAsset
import com.recapflow.ai.media.edit.ReplacementAudioTimeline
import com.recapflow.ai.media.edit.ScaleMode
import com.recapflow.ai.media.edit.SpeedCompiler
import com.recapflow.ai.media.edit.SourceSubtitleBlurSettings
import com.recapflow.ai.media.edit.TransformCompiler
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TransitionCompiler
import com.recapflow.ai.media.edit.TransitionMode
import com.recapflow.ai.media.edit.TransitionSettings
import com.recapflow.ai.media.edit.TrimRange
import com.recapflow.ai.media.edit.ZoomMode
import com.recapflow.ai.media.edit.ZoomSettings
import com.recapflow.ai.media.export.PublicExportCoordinator
import com.recapflow.ai.media.export.PublicExportUiState
import com.recapflow.ai.media.importer.ImportResumeRequest
import com.recapflow.ai.media.importer.ImportUiState
import com.recapflow.ai.media.importer.ImageOverlayImportCoordinator
import com.recapflow.ai.media.importer.ImageOverlayImportState
import com.recapflow.ai.media.importer.MediaImportCoordinator
import com.recapflow.ai.media.importer.ReplacementAudioImportCoordinator
import com.recapflow.ai.media.importer.ReplacementAudioImportState
import com.recapflow.ai.media.render.CompositionPreviewTimelinePolicy
import com.recapflow.ai.media.render.CompositionPreviewPlayerFactory
import com.recapflow.ai.media.render.Media3CompositionCompiler
import com.recapflow.ai.media.render.Media3CompositionPlan
import com.recapflow.ai.media.render.Media3CompositionPlanCompiler
import com.recapflow.ai.media.render.LocalRenderCoordinator
import com.recapflow.ai.media.render.PausedPreviewRefreshPolicy
import com.recapflow.ai.media.render.PreviewGraphKey
import com.recapflow.ai.media.render.PreviewGeometryChangePolicy
import com.recapflow.ai.media.render.PreviewGeometryPolicy
import com.recapflow.ai.media.render.PreviewUiState
import com.recapflow.ai.media.render.RealtimeImageOverlayState
import com.recapflow.ai.media.render.RealtimePreviewSession
import com.recapflow.ai.media.render.RealtimeSourceBlurState
import com.recapflow.ai.media.render.RenderPreset
import com.recapflow.ai.media.render.RenderQualityPolicy
import com.recapflow.ai.media.render.RenderUiState
import com.recapflow.ai.media.render.RenderedOutputValidationPolicy
import com.recapflow.ai.media.render.TransformVideoEffects
import com.recapflow.ai.preferences.AudioPreference
import com.recapflow.ai.preferences.EditorPreferencesPolicy
import com.recapflow.ai.preferences.EditorPreferencesSnapshot
import com.recapflow.ai.preferences.EditorPreferencesStore
import com.recapflow.ai.preferences.EditorSection
import com.recapflow.ai.preferences.OverlayPreference
import com.recapflow.ai.ui.ClipTransitionEditorController
import com.recapflow.ai.ui.MediaFormatters
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalApi::class)
@UnstableApi
class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding
        get() = checkNotNull(_binding) { "Activity has been destroyed" }
    private val editor
        get() = binding.editorContent

    private lateinit var importCoordinator: MediaImportCoordinator
    private lateinit var replacementAudioImportCoordinator: ReplacementAudioImportCoordinator
    private lateinit var imageOverlayImportCoordinator: ImageOverlayImportCoordinator
    private lateinit var renderCoordinator: LocalRenderCoordinator
    private lateinit var publicExportCoordinator: PublicExportCoordinator
    private lateinit var editorPreferencesStore: EditorPreferencesStore
    private lateinit var previewPlayer: ExoPlayer
    private var compositionPreviewPlayer: CompositionPlayer? = null
    private var compositionPreviewPlan: Media3CompositionPlan? = null
    private var compositionPreviewEditPlan: EditPlan? = null
    private var compositionPreviewSourcePath: String? = null
    private var compositionPreviewBlockedPath: String? = null
    private lateinit var replacementAudioPlayer: ExoPlayer
    private val freezePreviewHandler = Handler(Looper.getMainLooper())
    private val adaptivePreviewHandler = Handler(Looper.getMainLooper())
    private val replacementAudioSyncHandler = Handler(Looper.getMainLooper())
    private val sourceBlurPreviewHandler = Handler(Looper.getMainLooper())
    private val previewRecoveryHandler = Handler(Looper.getMainLooper())
    private val editorPreferencesHandler = Handler(Looper.getMainLooper())
    private val clipTransitionPreviewHandler = Handler(Looper.getMainLooper())
    private lateinit var clipTransitionEditorController: ClipTransitionEditorController
    private val realtimeSourceBlurState = RealtimeSourceBlurState()
    private val realtimeImageOverlayState = RealtimeImageOverlayState()
    private val realtimePreviewSession = RealtimePreviewSession()
    private var technicalDetailsExpanded = false
    private var previewPath: String? = null
    private var previewUiState: PreviewUiState = PreviewUiState.LiveEffects
    private var previewCapabilityRestored = false
    private val previewFallbackActive: Boolean
        get() = previewUiState !is PreviewUiState.LiveEffects
    private var activeMediaInfo: MediaInfo? = null
    private var restoredTrimStartMs: Long? = null
    private var restoredTrimEndMs: Long? = null
    private var selectedDestination = MainDestination.HOME
    private var transformEnabled = false
    private var transformDetailsVisible = true
    private var transformAspectRatio = AspectRatioPreset.ORIGINAL
    private var transformScaleMode = ScaleMode.FIT
    private var cropEnabled = false
    private var cropRectangle = CropRectangle()
    private var mirrorEnabled = false
    private var colorEnabled = false
    private var colorBrightness = 0f
    private var colorContrast = 0f
    private var colorSaturation = 0f
    private var colorTemperature = 0f
    private var zoomEnabled = false
    private var zoomMode = ZoomMode.IN
    private var speedEnabled = false
    private var speedMultiplier = DEFAULT_SPEED_MULTIPLIER
    private var freezeEnabled = false
    private var freezeDurationMs = FreezeCompiler.DEFAULT_DURATION_MS
    private var transitionEnabled = false
    private var transitionMode = TransitionMode.FADE_IN_OUT
    private var transitionDurationMs = TransitionCompiler.DEFAULT_DURATION_MS
    private var audioEnabled = false
    private var audioPolicy = AudioPolicy.KEEP_ORIGINAL
    private var audioVolume = AudioCompiler.UNITY_LINEAR_GAIN
    private var mixSourceVolume = AudioCompiler.DEFAULT_MIX_SOURCE_LINEAR_GAIN
    private var mixAddedVolume = AudioCompiler.DEFAULT_MIX_LINEAR_GAIN
    private var overlayEnabled = false
    private var overlayDetailsVisible = true
    private var sourceSubtitleBlurEnabled = false
    private var sourceSubtitleBlurRectangle = BlurRectangle()
    private var sourceSubtitleBlurStrength = OverlayCompiler.DEFAULT_BLUR_STRENGTH
    private var sourceSubtitleBlurStartMs = 0L
    private var sourceSubtitleBlurEndMs = 0L
    private var sourceSubtitleBlurRangeInitialized = false
    private var sourceSubtitleBlurRangeFollowsTrim = true
    private var imageOverlayEnabled = false
    private var imageOverlayAsset: ImageOverlayAsset? = null
    private var imageOverlayCenterX = OverlayCompiler.DEFAULT_IMAGE_CENTER_X
    private var imageOverlayCenterY = OverlayCompiler.DEFAULT_IMAGE_CENTER_Y
    private var imageOverlayWidthFraction = OverlayCompiler.DEFAULT_IMAGE_WIDTH_FRACTION
    private var imageOverlayOpacity = OverlayCompiler.DEFAULT_IMAGE_OPACITY
    private var imageOverlayStartMs = 0L
    private var imageOverlayEndMs = 0L
    private var imageOverlayRangeInitialized = false
    private var imageOverlayRangeFollowsTrim = true
    private var imageOverlayImporting = false
    private var imageOverlayImportName: String? = null
    private var replacementAudioAsset: ReplacementAudioAsset? = null
    private var replacementAudioImporting = false
    private var replacementAudioImportName: String? = null
    private var replacementPreviewPath: String? = null
    private var replacementPreviewErrorShown = false
    private var adaptivePreset = AdaptiveCutPreset.BALANCED
    private var adaptiveDraftRanges: List<TrimRange> = emptyList()
    private var adaptiveApplied = false
    private var adaptiveCandidateIndex = 0
    private var adaptivePreviewActive = false
    private var adaptiveSequencePreviewActive = false
    private var freezePreviewActive = false
    private var freezePreviewStartedAtElapsedMs = 0L
    private var selectedReviewEditorTab = ReviewEditorTab.CLIPS
    private var selectedRenderPreset = RenderPreset.DEFAULT
    private var previewOverlayScale = DEFAULT_PREVIEW_OVERLAY_SCALE
    private var previewOverlayCenterXFraction = DEFAULT_PREVIEW_CENTER_X_FRACTION
    private var previewOverlayCenterYFraction = PREVIEW_POSITION_UNSET
    private var previewBaseWidth = 0
    private var previewBaseHeight = 0
    private var sourceBlurPreviewUpdatePosted = false
    private var sourceBlurPreviewDirty = false
    private var sourceBlurPreviewReason = SOURCE_BLUR_PREVIEW_REASON_DEFAULT
    private var sourceBlurGestureCommitRunnable: Runnable? = null
    private var previewReadyTimeoutPath: String? = null
    private var previewReadyTimeoutGeneration = 0L
    private var previewLastValidPositionMs = 0L
    private var previewLastPlayWhenReady = false
    private var pausedPreviewRefreshAnchorMs: Long? = null
    private var pausedPreviewRefreshPreferForward = true
    private val settlePausedPreviewFrameRefresh = Runnable {
        val anchorMs = pausedPreviewRefreshAnchorMs ?: return@Runnable
        pausedPreviewRefreshAnchorMs = null
        val info = activeMediaInfo ?: return@Runnable
        if (
            previewFallbackActive ||
            compositionPreviewActive ||
            previewPath != info.workingFilePath ||
            previewPlayer.isPlaying ||
            previewPlayer.playbackState == Player.STATE_IDLE
        ) {
            return@Runnable
        }
        val currentPositionMs = runCatching { previewPlayer.currentPosition.coerceAtLeast(0L) }
            .getOrDefault(anchorMs)
        if (!PausedPreviewRefreshPolicy.mayRestoreAnchor(anchorMs, currentPositionMs, info.frameRate)) {
            return@Runnable
        }
        runCatching {
            previewPlayer.seekTo(anchorMs.coerceIn(0L, info.durationMs.coerceAtLeast(0L)))
        }.onFailure { error ->
            Log.w(TAG_PREVIEW, "Paused preview settle seek was rejected", error)
        }
    }
    /**
     * Media3 effect topology currently installed on [previewPlayer]. Parameter-only changes can
     * stay on the retained player, but adding/removing an Effect while decoding is a common
     * failure point on device-specific GPU/codec stacks. Topology changes therefore rebuild the
     * preview decoder only; they never render/transcode media and never mutate the EditPlan.
     */
    private var previewEffectSignature: List<String> = emptyList()
    private val persistEditorPreferences = Runnable {
        if (::editorPreferencesStore.isInitialized && _binding != null) {
            editorPreferencesStore.saveLastSession(currentEditorPreferencesSnapshot())
            renderEditorPreferenceControls()
        }
    }

    private val sourceBlurPreviewUpdate = Runnable {
        sourceBlurPreviewUpdatePosted = false
        val info = activeMediaInfo ?: return@Runnable
        if (!sourceBlurPreviewDirty || previewFallbackActive) return@Runnable
        if (renderCoordinator.currentState.isActiveRender()) return@Runnable
        val pendingUpdate = realtimePreviewSession.takePending()
        if (pendingUpdate == null) {
            sourceBlurPreviewDirty = false
            redrawPausedPreviewFrame()
            return@Runnable
        }
        if (!realtimePreviewSession.isCurrent(info.workingFilePath, pendingUpdate.generation)) {
            sourceBlurPreviewDirty = false
            return@Runnable
        }
        val applied = applyLiveTransformPreview(
            info = info,
            reason = pendingUpdate.reason,
            requestedKey = pendingUpdate.key,
        )
        sourceBlurPreviewDirty = !applied && !previewFallbackActive
    }

    private val previewReadyTimeout = Runnable {
        val failedPath = previewReadyTimeoutPath ?: return@Runnable
        val expectedGeneration = previewReadyTimeoutGeneration
        if (!realtimePreviewSession.isCurrent(failedPath, expectedGeneration)) return@Runnable
        if (activePreviewPlaybackState() == Player.STATE_READY) return@Runnable
        Log.w(
            TAG_PREVIEW,
            "Preview readiness timeout path=$failedPath generation=$expectedGeneration " +
                "state=${activePreviewPlaybackState()} composition=$compositionPreviewActive",
        )
        if (activeMediaInfo?.workingFilePath == failedPath) {
            if (compositionPreviewActive) {
                fallbackFromCompositionPreview("readiness timeout")
            } else {
                recoverPreviewSession(
                    failedPath = failedPath,
                    expectedGeneration = expectedGeneration,
                    reason = "readiness timeout",
                )
            }
        } else {
            showNonSourcePreviewUnavailable("readiness timeout")
        }
    }

    private val legacyMediaPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // ACTION_OPEN_DOCUMENT remains the privacy-preserving fallback even if
        // an older device denies broad media access.
        openVideoPicker()
    }

    private val legacyPublicExportPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val completed = renderCoordinator.currentState as? RenderUiState.Completed
        if (granted && completed != null) {
            publicExportCoordinator.publish(completed.outputPath, force = true)
        } else {
            publicExportCoordinator.permissionDenied()
        }
    }

    private val videoPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) {
            importCoordinator.pickerCancelled()
            return@registerForActivityResult
        }

        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        importCoordinator.import(uri)
    }

    private val replacementAudioPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        replacementAudioImportCoordinator.import(uri)
    }

    private val imageOverlayPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        imageOverlayImportCoordinator.import(uri)
    }

    private val previewListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> schedulePreviewReadyTimeout()
                Player.STATE_READY -> {
                    cancelPreviewReadyTimeout()
                    previewLastValidPositionMs = previewPlayer.currentPosition.coerceAtLeast(0L)
                    previewLastPlayWhenReady = previewPlayer.playWhenReady
                    renderPreviewUiState()
                }
                Player.STATE_IDLE -> cancelPreviewReadyTimeout()
            }
            if (
                playbackState == Player.STATE_ENDED &&
                adaptiveSequencePreviewActive
            ) {
                val info = activeMediaInfo ?: return
                val restorePositionMs = adaptiveDraftRanges.lastOrNull()?.endMs
                    ?: currentTrimRange(info).endMs
                adaptiveSequencePreviewActive = false
                renderAdaptiveCutControls()
                restoreSourceAfterAdaptivePreview(restorePositionMs)
                return
            }
            if (playbackState != Player.STATE_READY) return
            syncReplacementAudioPreview(forceSeek = true)
        }

        override fun onRenderedFirstFrame() {
            if (previewUiState is PreviewUiState.LiveEffects) {
                realtimePreviewSession.confirmApplied()
            }
            renderPreviewUiState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncReplacementAudioPreview(forceSeek = true)
            if (!adaptiveSequencePreviewActive) return
            val rangeIndex = mediaItem?.mediaId
                ?.removePrefix(ADAPTIVE_SEQUENCE_MEDIA_ID_PREFIX)
                ?.toIntOrNull()
                ?: previewPlayer.currentMediaItemIndex
            if (rangeIndex !in adaptiveDraftRanges.indices) return
            adaptiveCandidateIndex = rangeIndex
            applyAdaptiveSequenceEffects(rangeIndex)
            renderAdaptiveCutControls()
        }

        override fun onPlayerError(error: PlaybackException) {
            val sourceInfoForLog = activeMediaInfo
            Log.e(
                TAG_PREVIEW,
                "Player error code=${error.errorCodeName}(${error.errorCode}); " +
                    "sourcePreview=${previewPath == activeMediaInfo?.workingFilePath}; " +
                    "source=${sourceInfoForLog?.width}x${sourceInfoForLog?.height}; " +
                    "codec=${sourceInfoForLog?.videoCodec}; " +
                    "blurEnabled=${overlayEnabled && sourceSubtitleBlurEnabled}; " +
                    "graph=${realtimePreviewSession.currentGraphSummary()}",
                error,
            )
            cancelSourceBlurPreviewUpdate(clearDirty = true)
            cancelPreviewReadyTimeout()
            pauseReplacementAudioPreview()
            val info = activeMediaInfo
            val failedPath = previewPath
            if (adaptiveSequencePreviewActive && info != null) {
                adaptiveSequencePreviewActive = false
                adaptivePreviewActive = false
                renderAdaptiveCutControls()
                Snackbar.make(
                    binding.mainRoot,
                    R.string.adaptive_sequence_preview_error,
                    Snackbar.LENGTH_LONG,
                ).show()
                restoreSourceAfterAdaptivePreview(currentTrimRange(info).startMs)
                return
            }
            if (info != null && failedPath == info.workingFilePath) {
                recoverPreviewSession(
                    failedPath = failedPath,
                    expectedGeneration = realtimePreviewSession.currentGeneration(),
                    reason = "player error ${error.errorCodeName}(${error.errorCode})",
                )
            } else {
                showNonSourcePreviewUnavailable("player error ${error.errorCodeName}")
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            previewLastPlayWhenReady = previewPlayer.playWhenReady
            if (previewPlayer.playbackState == Player.STATE_READY) {
                previewLastValidPositionMs = previewPlayer.currentPosition.coerceAtLeast(0L)
            }
            syncReplacementAudioPreview(forceSeek = true)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (previewPlayer.playbackState == Player.STATE_READY) {
                previewLastValidPositionMs = newPosition.positionMs.coerceAtLeast(0L)
            }
            syncReplacementAudioPreview(forceSeek = true)
        }
    }

    private val compositionPreviewListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> schedulePreviewReadyTimeout()
                Player.STATE_READY -> {
                    cancelPreviewReadyTimeout()
                    activeMediaInfo?.let { info ->
                        previewLastValidPositionMs = activePreviewSourcePositionMs(info)
                    }
                    previewLastPlayWhenReady = activePreviewPlayWhenReady()
                    renderPreviewUiState()
                }
                Player.STATE_IDLE -> cancelPreviewReadyTimeout()
            }
        }

        override fun onRenderedFirstFrame() {
            if (previewUiState is PreviewUiState.LiveEffects) {
                realtimePreviewSession.confirmApplied()
            }
            renderPreviewUiState()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(
                TAG_PREVIEW,
                "CompositionPlayer error code=${error.errorCodeName}(${error.errorCode}) " +
                    "graph=${realtimePreviewSession.currentGraphSummary()}",
                error,
            )
            cancelSourceBlurPreviewUpdate(clearDirty = true)
            cancelPreviewReadyTimeout()
            fallbackFromCompositionPreview(
                reason = "player error ${error.errorCodeName}(${error.errorCode})",
                error = error,
            )
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            previewLastPlayWhenReady = activePreviewPlayWhenReady()
            if (activePreviewPlaybackState() == Player.STATE_READY) {
                activeMediaInfo?.let { info ->
                    previewLastValidPositionMs = activePreviewSourcePositionMs(info)
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (activePreviewPlaybackState() == Player.STATE_READY) {
                activeMediaInfo?.let { info ->
                    previewLastValidPositionMs = activePreviewSourcePositionMs(info)
                }
            }
        }
    }

    private val replacementAudioPreviewListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                syncReplacementAudioPreview(forceSeek = true)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            pauseReplacementAudioPreview()
            if (!replacementPreviewErrorShown && _binding != null) {
                replacementPreviewErrorShown = true
                Snackbar.make(
                    binding.mainRoot,
                    R.string.audio_replace_preview_error,
                    Snackbar.LENGTH_LONG,
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        previewCapabilityRestored = savedInstanceState
            ?.getBoolean(KEY_PREVIEW_SOURCE_ONLY, false)
            ?: false
        if (previewCapabilityRestored) {
            previewUiState = PreviewUiState.SourceOnly("restored source-only preview session")
        }
        editorPreferencesStore = EditorPreferencesStore(this)

        restoredTrimStartMs = savedInstanceState?.takeIf { it.containsKey(KEY_TRIM_START_MS) }
            ?.getLong(KEY_TRIM_START_MS)
        restoredTrimEndMs = savedInstanceState?.takeIf { it.containsKey(KEY_TRIM_END_MS) }
            ?.getLong(KEY_TRIM_END_MS)
        transformEnabled = savedInstanceState?.getBoolean(KEY_TRANSFORM_ENABLED) ?: false
        transformDetailsVisible = savedInstanceState
            ?.takeIf { it.containsKey(KEY_TRANSFORM_DETAILS_VISIBLE) }
            ?.getBoolean(KEY_TRANSFORM_DETAILS_VISIBLE)
            ?: true
        transformAspectRatio = savedInstanceState?.getString(KEY_TRANSFORM_ASPECT_RATIO)
            ?.let { savedName ->
                AspectRatioPreset.entries.firstOrNull { it.name == savedName }
            }
            ?: AspectRatioPreset.ORIGINAL
        transformScaleMode = savedInstanceState?.getString(KEY_TRANSFORM_SCALE_MODE)
            ?.let { savedName ->
                ScaleMode.entries.firstOrNull { it.name == savedName }
            }
            ?: ScaleMode.FIT
        cropEnabled = savedInstanceState?.getBoolean(KEY_CROP_ENABLED) ?: false
        mirrorEnabled = savedInstanceState?.getBoolean(KEY_MIRROR_ENABLED) ?: false
        colorEnabled = savedInstanceState?.getBoolean(KEY_COLOR_ENABLED) ?: false
        colorBrightness = savedInstanceState?.getFloat(KEY_COLOR_BRIGHTNESS) ?: 0f
        colorContrast = savedInstanceState?.getFloat(KEY_COLOR_CONTRAST) ?: 0f
        colorSaturation = savedInstanceState?.getFloat(KEY_COLOR_SATURATION) ?: 0f
        colorTemperature = savedInstanceState?.getFloat(KEY_COLOR_TEMPERATURE) ?: 0f
        zoomEnabled = savedInstanceState?.getBoolean(KEY_ZOOM_ENABLED) ?: false
        zoomMode = savedInstanceState?.getString(KEY_ZOOM_MODE)
            ?.let { savedName -> ZoomMode.entries.firstOrNull { it.name == savedName } }
            ?.takeUnless { it == ZoomMode.OFF }
            ?: ZoomMode.IN
        speedEnabled = savedInstanceState?.getBoolean(KEY_SPEED_ENABLED) ?: false
        speedMultiplier = savedInstanceState?.getFloat(KEY_SPEED_MULTIPLIER)
            ?.takeIf { it in SpeedCompiler.supportedPresets }
            ?: DEFAULT_SPEED_MULTIPLIER
        freezeEnabled = savedInstanceState?.getBoolean(KEY_FREEZE_ENABLED) ?: false
        freezeDurationMs = savedInstanceState?.getLong(KEY_FREEZE_DURATION_MS)
            ?.takeIf { it in FreezeCompiler.supportedDurationsMs }
            ?: FreezeCompiler.DEFAULT_DURATION_MS
        transitionEnabled = savedInstanceState?.getBoolean(KEY_TRANSITION_ENABLED) ?: false
        transitionMode = savedInstanceState?.getString(KEY_TRANSITION_MODE)
            ?.let { savedName -> TransitionMode.entries.firstOrNull { it.name == savedName } }
            ?.takeUnless { it == TransitionMode.OFF }
            ?: TransitionMode.FADE_IN_OUT
        transitionDurationMs = savedInstanceState?.getLong(KEY_TRANSITION_DURATION_MS)
            ?.takeIf { it in TransitionCompiler.supportedDurationsMs }
            ?: TransitionCompiler.DEFAULT_DURATION_MS
        audioEnabled = savedInstanceState?.getBoolean(KEY_AUDIO_ENABLED) ?: false
        audioPolicy = savedInstanceState?.getString(KEY_AUDIO_POLICY)
            ?.let { savedName -> AudioPolicy.entries.firstOrNull { it.name == savedName } }
            ?.takeIf {
                it == AudioPolicy.KEEP_ORIGINAL ||
                    it == AudioPolicy.MUTE ||
                    it == AudioPolicy.REPLACE ||
                    it == AudioPolicy.MIX
            }
            ?: AudioPolicy.KEEP_ORIGINAL
        audioVolume = savedInstanceState
            ?.takeIf { it.containsKey(KEY_AUDIO_VOLUME) }
            ?.getFloat(KEY_AUDIO_VOLUME)
            ?.takeIf { it in AudioCompiler.MIN_LINEAR_GAIN..AudioCompiler.MAX_LINEAR_GAIN }
            ?: AudioCompiler.UNITY_LINEAR_GAIN
        mixSourceVolume = savedInstanceState
            ?.takeIf { it.containsKey(KEY_MIX_SOURCE_VOLUME) }
            ?.getFloat(KEY_MIX_SOURCE_VOLUME)
            ?.takeIf { it in AudioCompiler.MIN_LINEAR_GAIN..AudioCompiler.MAX_LINEAR_GAIN }
            ?: AudioCompiler.DEFAULT_MIX_SOURCE_LINEAR_GAIN
        mixAddedVolume = savedInstanceState
            ?.takeIf { it.containsKey(KEY_MIX_ADDED_VOLUME) }
            ?.getFloat(KEY_MIX_ADDED_VOLUME)
            ?.takeIf { it in AudioCompiler.MIN_LINEAR_GAIN..AudioCompiler.MAX_LINEAR_GAIN }
            ?: AudioCompiler.DEFAULT_MIX_LINEAR_GAIN
        overlayEnabled = savedInstanceState?.getBoolean(KEY_OVERLAY_ENABLED) ?: false
        overlayDetailsVisible = savedInstanceState
            ?.takeIf { it.containsKey(KEY_OVERLAY_DETAILS_VISIBLE) }
            ?.getBoolean(KEY_OVERLAY_DETAILS_VISIBLE)
            ?: true
        sourceSubtitleBlurEnabled = savedInstanceState
            ?.getBoolean(KEY_SOURCE_BLUR_ENABLED)
            ?: false
        sourceSubtitleBlurStrength = savedInstanceState
            ?.getFloat(KEY_SOURCE_BLUR_STRENGTH)
            ?.takeIf {
                it in OverlayCompiler.MIN_BLUR_STRENGTH..OverlayCompiler.MAX_BLUR_STRENGTH
            }
            ?: OverlayCompiler.DEFAULT_BLUR_STRENGTH
        sourceSubtitleBlurRectangle = savedInstanceState?.takeIf {
            it.containsKey(KEY_SOURCE_BLUR_LEFT) &&
                it.containsKey(KEY_SOURCE_BLUR_TOP) &&
                it.containsKey(KEY_SOURCE_BLUR_RIGHT) &&
                it.containsKey(KEY_SOURCE_BLUR_BOTTOM)
        }?.let {
            BlurRectangle(
                left = it.getFloat(KEY_SOURCE_BLUR_LEFT),
                top = it.getFloat(KEY_SOURCE_BLUR_TOP),
                right = it.getFloat(KEY_SOURCE_BLUR_RIGHT),
                bottom = it.getFloat(KEY_SOURCE_BLUR_BOTTOM),
            )
        }?.takeIf(BlurRectangle::isValid) ?: BlurRectangle()
        sourceSubtitleBlurRangeInitialized = savedInstanceState?.let {
            it.containsKey(KEY_SOURCE_BLUR_START_MS) && it.containsKey(KEY_SOURCE_BLUR_END_MS)
        } == true
        savedInstanceState?.takeIf { sourceSubtitleBlurRangeInitialized }?.let { state ->
            sourceSubtitleBlurStartMs = state.getLong(KEY_SOURCE_BLUR_START_MS)
            sourceSubtitleBlurEndMs = state.getLong(KEY_SOURCE_BLUR_END_MS)
        }
        sourceSubtitleBlurRangeFollowsTrim = savedInstanceState
            ?.takeIf { it.containsKey(KEY_SOURCE_BLUR_RANGE_FOLLOWS_TRIM) }
            ?.getBoolean(KEY_SOURCE_BLUR_RANGE_FOLLOWS_TRIM)
            ?: true // migration: pre-1C ranges had no intent flag; default them back to Trim-linked
        imageOverlayEnabled = savedInstanceState?.getBoolean(KEY_IMAGE_OVERLAY_ENABLED) ?: false
        imageOverlayCenterX = savedInstanceState
            ?.getFloat(KEY_IMAGE_OVERLAY_CENTER_X)
            ?.takeIf { it in 0f..1f }
            ?: OverlayCompiler.DEFAULT_IMAGE_CENTER_X
        imageOverlayCenterY = savedInstanceState
            ?.getFloat(KEY_IMAGE_OVERLAY_CENTER_Y)
            ?.takeIf { it in 0f..1f }
            ?: OverlayCompiler.DEFAULT_IMAGE_CENTER_Y
        imageOverlayWidthFraction = savedInstanceState
            ?.getFloat(KEY_IMAGE_OVERLAY_WIDTH_FRACTION)
            ?.takeIf {
                it in OverlayCompiler.MIN_IMAGE_WIDTH_FRACTION..
                    OverlayCompiler.MAX_IMAGE_WIDTH_FRACTION
            }
            ?: OverlayCompiler.DEFAULT_IMAGE_WIDTH_FRACTION
        imageOverlayOpacity = savedInstanceState
            ?.getFloat(KEY_IMAGE_OVERLAY_OPACITY)
            ?.takeIf {
                it in OverlayCompiler.MIN_IMAGE_OPACITY..OverlayCompiler.MAX_IMAGE_OPACITY
            }
            ?: OverlayCompiler.DEFAULT_IMAGE_OPACITY
        imageOverlayRangeInitialized = savedInstanceState?.let {
            it.containsKey(KEY_IMAGE_OVERLAY_START_MS) &&
                it.containsKey(KEY_IMAGE_OVERLAY_END_MS)
        } == true
        savedInstanceState?.takeIf { imageOverlayRangeInitialized }?.let { state ->
            imageOverlayStartMs = state.getLong(KEY_IMAGE_OVERLAY_START_MS)
            imageOverlayEndMs = state.getLong(KEY_IMAGE_OVERLAY_END_MS)
        }
        imageOverlayRangeFollowsTrim = savedInstanceState
            ?.takeIf { it.containsKey(KEY_IMAGE_OVERLAY_RANGE_FOLLOWS_TRIM) }
            ?.getBoolean(KEY_IMAGE_OVERLAY_RANGE_FOLLOWS_TRIM)
            ?: true // migration: pre-1C ranges had no intent flag; default them back to Trim-linked
        val savedImageOverlay = savedInstanceState
            ?.takeIf {
                it.containsKey(KEY_IMAGE_OVERLAY_PATH) &&
                    it.containsKey(KEY_IMAGE_OVERLAY_PIXEL_WIDTH) &&
                    it.containsKey(KEY_IMAGE_OVERLAY_PIXEL_HEIGHT)
            }
            ?.let { state ->
                ImageOverlayAsset(
                    workingFilePath = state.getString(KEY_IMAGE_OVERLAY_PATH).orEmpty(),
                    displayName = state.getString(KEY_IMAGE_OVERLAY_NAME).orEmpty(),
                    mimeType = state.getString(KEY_IMAGE_OVERLAY_MIME_TYPE).orEmpty(),
                    pixelWidth = state.getInt(KEY_IMAGE_OVERLAY_PIXEL_WIDTH),
                    pixelHeight = state.getInt(KEY_IMAGE_OVERLAY_PIXEL_HEIGHT),
                    fileSizeBytes = state.getLong(KEY_IMAGE_OVERLAY_SIZE_BYTES),
                )
            }
        val savedReplacementAudio = savedInstanceState
            ?.takeIf {
                it.containsKey(KEY_REPLACEMENT_AUDIO_PATH) &&
                    it.containsKey(KEY_REPLACEMENT_AUDIO_DURATION_MS)
            }
            ?.let { state ->
                ReplacementAudioAsset(
                    workingFilePath = state.getString(KEY_REPLACEMENT_AUDIO_PATH).orEmpty(),
                    displayName = state.getString(KEY_REPLACEMENT_AUDIO_NAME).orEmpty(),
                    durationMs = state.getLong(KEY_REPLACEMENT_AUDIO_DURATION_MS),
                    fileSizeBytes = state.getLong(KEY_REPLACEMENT_AUDIO_SIZE_BYTES),
                )
            }
        adaptivePreset = savedInstanceState?.getString(KEY_ADAPTIVE_PRESET)
            ?.let { savedName -> AdaptiveCutPreset.entries.firstOrNull { it.name == savedName } }
            ?: AdaptiveCutPreset.BALANCED
        val adaptiveStarts = savedInstanceState?.getLongArray(KEY_ADAPTIVE_RANGE_STARTS)
        val adaptiveEnds = savedInstanceState?.getLongArray(KEY_ADAPTIVE_RANGE_ENDS)
        adaptiveDraftRanges = if (
            adaptiveStarts != null &&
            adaptiveEnds != null &&
            adaptiveStarts.size == adaptiveEnds.size
        ) {
            adaptiveStarts.indices.map { index ->
                TrimRange(adaptiveStarts[index], adaptiveEnds[index])
            }
        } else {
            emptyList()
        }
        adaptiveApplied = savedInstanceState?.getBoolean(KEY_ADAPTIVE_APPLIED) == true &&
            adaptiveDraftRanges.isNotEmpty()
        adaptiveCandidateIndex = savedInstanceState?.getInt(KEY_ADAPTIVE_CANDIDATE_INDEX)
            ?.coerceIn(0, (adaptiveDraftRanges.size - 1).coerceAtLeast(0))
            ?: 0
        cropRectangle = savedInstanceState?.takeIf {
            it.containsKey(KEY_CROP_LEFT) &&
                it.containsKey(KEY_CROP_TOP) &&
                it.containsKey(KEY_CROP_RIGHT) &&
                it.containsKey(KEY_CROP_BOTTOM)
        }?.let {
            CropRectangle(
                left = it.getFloat(KEY_CROP_LEFT),
                top = it.getFloat(KEY_CROP_TOP),
                right = it.getFloat(KEY_CROP_RIGHT),
                bottom = it.getFloat(KEY_CROP_BOTTOM),
            )
        }?.takeIf(CropRectangle::isValid) ?: CropRectangle()
        selectedReviewEditorTab = savedInstanceState?.getString(KEY_REVIEW_EDITOR_TAB)
            ?.let { savedName ->
                ReviewEditorTab.entries.firstOrNull { it.name == savedName }
            }
            ?: ReviewEditorTab.CLIPS
        selectedRenderPreset = savedInstanceState?.getString(KEY_RENDER_PRESET)
            ?.let { savedName -> RenderPreset.entries.firstOrNull { it.name == savedName } }
            ?: RenderPreset.DEFAULT
        previewOverlayScale = savedInstanceState
            ?.getFloat(KEY_PREVIEW_OVERLAY_SCALE, DEFAULT_PREVIEW_OVERLAY_SCALE)
            ?.coerceAtLeast(MIN_PREVIEW_OVERLAY_SCALE)
            ?: DEFAULT_PREVIEW_OVERLAY_SCALE
        previewOverlayCenterXFraction = savedInstanceState
            ?.getFloat(KEY_PREVIEW_OVERLAY_CENTER_X, DEFAULT_PREVIEW_CENTER_X_FRACTION)
            ?.coerceIn(0f, 1f)
            ?: DEFAULT_PREVIEW_CENTER_X_FRACTION
        previewOverlayCenterYFraction = savedInstanceState
            ?.takeIf { it.containsKey(KEY_PREVIEW_OVERLAY_CENTER_Y) }
            ?.getFloat(KEY_PREVIEW_OVERLAY_CENTER_Y)
            ?.coerceIn(0f, 1f)
            ?: PREVIEW_POSITION_UNSET

        if (savedInstanceState == null && editorPreferencesStore.autoRestoreEnabled) {
            editorPreferencesStore.loadLastSession()?.let { snapshot ->
                applyEditorPreferencesToState(snapshot, assetDependentSettings = false)
            }
        }

        importCoordinator = MediaImportCoordinator(this, ::renderState)
        replacementAudioImportCoordinator = ReplacementAudioImportCoordinator(
            this,
            ::renderReplacementAudioImportState,
        )
        replacementAudioAsset = replacementAudioImportCoordinator.restore(savedReplacementAudio)
        imageOverlayImportCoordinator = ImageOverlayImportCoordinator(
            this,
            ::renderImageOverlayImportState,
        )
        imageOverlayAsset = imageOverlayImportCoordinator.restore(savedImageOverlay)
        renderCoordinator = LocalRenderCoordinator(this, ::renderRenderState)
        publicExportCoordinator = PublicExportCoordinator(this, ::renderPublicExportState)
        previewPlayer = createPreviewPlayer()
        replacementAudioPlayer = ExoPlayer.Builder(this).build().also { player ->
            player.repeatMode = Player.REPEAT_MODE_ALL
            player.addListener(replacementAudioPreviewListener)
        }
        bindActions()
        bindNavigation(savedInstanceState)
        renderPublicExportState(publicExportCoordinator.currentState)
        renderState(ImportUiState.EngineChecking)
        importCoordinator.initialize(savedInstanceState.toResumeRequest())
    }

    private fun createPreviewPlayer(): ExoPlayer = ExoPlayer.Builder(this).build().also { player ->
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.addListener(previewListener)
        editor.videoPreview.player = player
    }

    /** A decoder/effect failure can leave the current player instance unusable for fallback. */
    private fun replacePreviewPlayer() {
        if (::previewPlayer.isInitialized) {
            editor.videoPreview.player = null
            previewPlayer.removeListener(previewListener)
            runCatching { previewPlayer.release() }
                .onFailure { Log.w(TAG_PREVIEW, "Failed to release rejected preview player", it) }
        }
        previewPlayer = createPreviewPlayer()
        previewEffectSignature = emptyList()
    }

    private val compositionPreviewActive: Boolean
        get() = compositionPreviewPlayer != null &&
            compositionPreviewSourcePath == activeMediaInfo?.workingFilePath

    private fun activePreviewPlaybackState(): Int =
        compositionPreviewPlayer?.takeIf { compositionPreviewActive }?.playbackState
            ?: previewPlayer.playbackState

    private fun activePreviewIsPlaying(): Boolean =
        compositionPreviewPlayer?.takeIf { compositionPreviewActive }?.isPlaying
            ?: previewPlayer.isPlaying

    private fun activePreviewPlayWhenReady(): Boolean =
        compositionPreviewPlayer?.takeIf { compositionPreviewActive }?.playWhenReady
            ?: previewPlayer.playWhenReady

    private fun activePreviewPause() {
        compositionPreviewPlayer?.takeIf { compositionPreviewActive }?.pause() ?: previewPlayer.pause()
    }

    private fun activePreviewPlay() {
        compositionPreviewPlayer?.takeIf { compositionPreviewActive }?.play() ?: previewPlayer.play()
    }

    private fun activePreviewSourcePositionMs(info: MediaInfo = checkNotNull(activeMediaInfo)): Long {
        val compositionPlayer = compositionPreviewPlayer
        val plan = compositionPreviewPlan
        val editPlan = compositionPreviewEditPlan
        if (compositionPreviewActive && compositionPlayer != null && plan != null && editPlan != null) {
            return CompositionPreviewTimelinePolicy.outputToSourceMs(
                outputPositionMs = compositionPlayer.currentPosition.coerceAtLeast(0L),
                plan = plan,
                editPlan = editPlan,
            ).coerceIn(0L, info.durationMs)
        }
        return previewPlayer.currentPosition.coerceAtLeast(0L)
    }

    private fun activePreviewSeekToSourcePosition(info: MediaInfo, sourcePositionMs: Long) {
        val compositionPlayer = compositionPreviewPlayer
        val plan = compositionPreviewPlan
        val editPlan = compositionPreviewEditPlan
        if (compositionPreviewActive && compositionPlayer != null && plan != null && editPlan != null) {
            val selectedSourceMs = CompositionPreviewTimelinePolicy.nearestSelectedSourcePosition(
                sourcePositionMs,
                plan.selectedRanges,
            )
            val outputPositionMs = CompositionPreviewTimelinePolicy.sourceToOutputMs(
                sourcePositionMs = selectedSourceMs,
                plan = plan,
                editPlan = editPlan,
            )
            compositionPlayer.seekTo(outputPositionMs.coerceAtLeast(0L))
        } else {
            previewPlayer.seekTo(sourcePositionMs.coerceIn(0L, info.durationMs))
        }
    }

    private fun releaseCompositionPreview(attachExoPlayer: Boolean = true, reason: String) {
        val player = compositionPreviewPlayer ?: return
        compositionPreviewPlayer = null
        compositionPreviewPlan = null
        compositionPreviewEditPlan = null
        compositionPreviewSourcePath = null
        runCatching {
            player.removeListener(compositionPreviewListener)
            player.release()
        }.onFailure { Log.w(TAG_PREVIEW, "CompositionPlayer release failed reason=$reason", it) }
        if (attachExoPlayer && _binding != null && ::previewPlayer.isInitialized) {
            editor.videoPreview.player = previewPlayer
        }
        Log.d(TAG_PREVIEW, "CompositionPlayer released reason=$reason")
    }

    private fun compositionPreviewEligible(info: MediaInfo): Boolean {
        if (!BuildConfig.ENABLE_COMPOSITION_PLAYER_PREVIEW) return false
        if (previewFallbackActive) return false
        if (compositionPreviewBlockedPath == info.workingFilePath) return false
        if (adaptivePreviewActive || adaptiveSequencePreviewActive || freezePreviewActive) return false
        if (FreezeCompiler.compile(currentTransformSettings()) != null) return false
        return File(info.workingFilePath).isFile
    }

    private fun prepareCompositionPreview(
        info: MediaInfo,
        autoPlay: Boolean,
        sourcePositionMs: Long,
        reason: String,
    ): Boolean {
        if (!compositionPreviewEligible(info)) return false
        val editPlan = currentEditPlan(RenderPreset.HD_720P)
        val plan = Media3CompositionPlanCompiler.compile(info, editPlan)
        if (plan.freeze != null) return false
        val compiled = Media3CompositionCompiler.compileForPreview(
            mediaInfo = info,
            editPlan = editPlan,
            input = File(info.workingFilePath),
            plan = plan,
        )
        val selectedSourceMs = CompositionPreviewTimelinePolicy.nearestSelectedSourcePosition(
            sourcePositionMs,
            plan.selectedRanges,
        )
        val outputPositionMs = CompositionPreviewTimelinePolicy.sourceToOutputMs(
            sourcePositionMs = selectedSourceMs,
            plan = plan,
            editPlan = editPlan,
        )

        releaseCompositionPreview(attachExoPlayer = false, reason = "replace composition: $reason")
        runCatching {
            previewPlayer.playWhenReady = false
            previewPlayer.stop()
            previewPlayer.clearMediaItems()
        }
        val player = CompositionPreviewPlayerFactory.create(this, compiled)
        compositionPreviewPlayer = player
        compositionPreviewPlan = plan
        compositionPreviewEditPlan = editPlan
        compositionPreviewSourcePath = info.workingFilePath
        player.addListener(compositionPreviewListener)
        editor.videoPreview.player = player
        realtimePreviewSession.markApplying(currentPreviewGraphKey(info))
        player.setComposition(compiled.composition, outputPositionMs.coerceAtLeast(0L))
        player.playWhenReady = autoPlay
        player.prepare()
        schedulePreviewReadyTimeout(
            path = info.workingFilePath,
            generation = realtimePreviewSession.currentGeneration(),
        )
        stopReplacementAudioPreview(clearMedia = false)
        Log.i(
            TAG_PREVIEW,
            "CompositionPlayer preview prepared reason=$reason sourceMs=$selectedSourceMs " +
                "outputMs=$outputPositionMs ${plan.summary}",
        )
        return true
    }

    private fun fallbackFromCompositionPreview(reason: String, error: Throwable? = null) {
        val info = activeMediaInfo ?: return
        if (!compositionPreviewActive) return
        val sourcePositionMs = runCatching { activePreviewSourcePositionMs(info) }
            .getOrDefault(previewLastValidPositionMs)
        val resumePlayback = runCatching { activePreviewPlayWhenReady() }
            .getOrDefault(previewLastPlayWhenReady)
        compositionPreviewBlockedPath = info.workingFilePath
        Log.w(
            TAG_PREVIEW,
            "CompositionPlayer fallback to ExoPlayer reason=$reason sourceMs=$sourcePositionMs",
            error,
        )
        releaseCompositionPreview(attachExoPlayer = true, reason = "fallback: $reason")
        runCatching {
            replacePreviewPlayer()
            prepareExoPreview(
                workingFilePath = info.workingFilePath,
                autoPlay = resumePlayback,
                startPositionMs = sourcePositionMs,
            )
        }.onFailure { exoError ->
            Log.e(TAG_PREVIEW, "ExoPlayer fallback after CompositionPlayer failed", exoError)
            recoverPreviewSession(
                failedPath = info.workingFilePath,
                expectedGeneration = realtimePreviewSession.currentGeneration(),
                reason = "composition fallback: ${exoError.javaClass.simpleName}",
                preferredPositionMs = sourcePositionMs,
            )
        }
    }

    private fun bindActions() {
        bindPreviewOverlayControls()
        editor.retryLivePreviewButton.setOnClickListener { retryLivePreviewEffects() }
        editor.trimRangeSlider.values = listOf(0f, 1f)
        editor.trimRangeSlider.addOnChangeListener { _, _, fromUser ->
            updateTrimSummary()
            if (fromUser) {
                onUserChangedTrim()
            }
        }
        editor.resetTrimButton.setOnClickListener { resetTrimToFullSource() }
        bindAdaptiveCutControls()
        bindClipTransitionControls()
        bindReviewEditorTabs()
        bindTransformControls()
        bindAudioControls()
        bindOverlayControls()
        bindExportQualityControls()
        bindEditorPreferenceControls()
        binding.homeContent.homeImportButton.setOnClickListener {
            navigateTo(MainDestination.EDITOR)
            requestVideoAccessThenPick()
        }
        binding.homeContent.homeContinueButton.setOnClickListener {
            navigateTo(MainDestination.EDITOR)
        }
        binding.homeContent.homeChooseAnotherButton.setOnClickListener {
            navigateTo(MainDestination.EDITOR)
            requestVideoAccessThenPick()
        }
        editor.importButton.setOnClickListener { requestVideoAccessThenPick() }
        editor.chooseAnotherButton.setOnClickListener { requestVideoAccessThenPick() }
        editor.chooseAnotherErrorButton.setOnClickListener { requestVideoAccessThenPick() }
        editor.retryButton.setOnClickListener { importCoordinator.retry() }
        editor.copyDiagnosticsButton.setOnClickListener { copyDiagnostics() }
        editor.technicalDetailsButton.setOnClickListener {
            technicalDetailsExpanded = !technicalDetailsExpanded
            renderTechnicalDetailsVisibility()
        }
        editor.nextGateButton.setOnClickListener { startNextRender() }
        editor.playOutputButton.setOnClickListener { playRenderedOutput() }
        editor.cancelRenderButton.setOnClickListener { confirmRenderCancellation() }
        editor.exportSaveButton.setOnClickListener { requestPublicExport() }
        editor.exportOpenButton.setOnClickListener { openPublishedExport() }
        editor.exportShareButton.setOnClickListener { sharePublishedExport() }
        editor.editorSheetScroll.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                updatePreviewUnderlayEffect(scrollY)
            },
        )
    }

    private fun bindPreviewOverlayControls() {
        var dragStartRawX = 0f
        var dragStartRawY = 0f
        var dragStartLeft = 0
        var dragStartTop = 0
        editor.previewDragHandle.setOnTouchListener { handle, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartRawX = event.rawX
                    dragStartRawY = event.rawY
                    val params = editor.previewCard.layoutParams as FrameLayout.LayoutParams
                    dragStartLeft = params.leftMargin
                    dragStartTop = params.topMargin
                    handle.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    movePreviewOverlay(
                        left = dragStartLeft + (event.rawX - dragStartRawX).roundToInt(),
                        top = dragStartTop + (event.rawY - dragStartRawY).roundToInt(),
                    )
                    true
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    handle.parent?.requestDisallowInterceptTouchEvent(false)
                    if (event.actionMasked == MotionEvent.ACTION_UP) handle.performClick()
                    scheduleEditorPreferencesSave()
                    true
                }
                else -> false
            }
        }

        var resizeStartRawX = 0f
        var resizeStartRawY = 0f
        var resizeStartScale = DEFAULT_PREVIEW_OVERLAY_SCALE
        var resizeAnchorLeft = 0
        var resizeAnchorTop = 0
        editor.previewResizeHandle.setOnTouchListener { handle, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    resizeStartRawX = event.rawX
                    resizeStartRawY = event.rawY
                    resizeStartScale = previewOverlayScale
                    val params = editor.previewCard.layoutParams as FrameLayout.LayoutParams
                    resizeAnchorLeft = params.leftMargin
                    resizeAnchorTop = params.topMargin
                    handle.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (previewBaseWidth > 0 && previewBaseHeight > 0) {
                        val horizontalDelta = (event.rawX - resizeStartRawX) / previewBaseWidth
                        val verticalDelta = (event.rawY - resizeStartRawY) / previewBaseHeight
                        val dominantDelta = if (abs(horizontalDelta) >= abs(verticalDelta)) {
                            horizontalDelta
                        } else {
                            verticalDelta
                        }
                        previewOverlayScale = (resizeStartScale + dominantDelta).coerceIn(
                            MIN_PREVIEW_OVERLAY_SCALE,
                            maxPreviewOverlayScale(),
                        )
                        val requestedWidth = previewBaseWidth * previewOverlayScale
                        val requestedHeight = previewBaseHeight * previewOverlayScale
                        previewOverlayCenterXFraction = (
                            resizeAnchorLeft + requestedWidth / 2f
                        ) / editor.root.width.coerceAtLeast(1).toFloat()
                        previewOverlayCenterYFraction = (
                            resizeAnchorTop + requestedHeight / 2f
                        ) / editor.root.height.coerceAtLeast(1).toFloat()
                        applyPreviewOverlayLayout()
                    }
                    true
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    handle.parent?.requestDisallowInterceptTouchEvent(false)
                    if (event.actionMasked == MotionEvent.ACTION_UP) handle.performClick()
                    scheduleEditorPreferencesSave()
                    true
                }
                else -> false
            }
        }

        editor.previewResetButton.setOnClickListener {
            previewOverlayScale = DEFAULT_PREVIEW_OVERLAY_SCALE
            previewOverlayCenterXFraction = DEFAULT_PREVIEW_CENTER_X_FRACTION
            previewOverlayCenterYFraction = PREVIEW_POSITION_UNSET
            applyPreviewOverlayLayout()
            scheduleEditorPreferencesSave()
            Snackbar.make(
                binding.mainRoot,
                R.string.preview_overlay_reset_confirmation,
                Snackbar.LENGTH_SHORT,
            ).show()
        }
    }

    private fun bindAdaptiveCutControls() {
        editor.adaptivePresetGroup.check(adaptivePresetButtonId(adaptivePreset))
        editor.adaptiveApplySwitch.isChecked = adaptiveApplied
        renderAdaptiveCutControls()

        editor.adaptivePresetGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val selected = when (checkedId) {
                R.id.adaptiveGentleButton -> AdaptiveCutPreset.GENTLE
                R.id.adaptiveCompactButton -> AdaptiveCutPreset.COMPACT
                else -> AdaptiveCutPreset.BALANCED
            }
            if (adaptivePreset != selected) {
                val wasApplied = adaptiveApplied
                adaptivePreset = selected
                adaptiveDraftRanges = emptyList()
                adaptiveCandidateIndex = 0
                adaptiveApplied = false
                editor.adaptiveApplySwitch.isChecked = false
                cancelAdaptivePreview()
                renderAdaptiveCutControls()
                if (wasApplied) onUserChangedAdaptiveCuts()
                scheduleEditorPreferencesSave()
            }
        }
        editor.generateAdaptiveDraftButton.setOnClickListener {
            val info = activeMediaInfo ?: return@setOnClickListener
            cancelAdaptivePreview()
            adaptiveDraftRanges = AdaptiveCutDraftEngine.generate(
                currentTrimRange(info),
                adaptivePreset,
            )
            val wasApplied = adaptiveApplied
            adaptiveApplied = false
            adaptiveCandidateIndex = 0
            editor.adaptiveApplySwitch.isChecked = false
            renderAdaptiveCutControls()
            seekToAdaptiveCandidate()
            if (wasApplied) onUserChangedAdaptiveCuts()
        }
        editor.adaptivePreviousButton.setOnClickListener {
            if (adaptiveDraftRanges.isEmpty()) return@setOnClickListener
            cancelAdaptivePreview()
            adaptiveCandidateIndex = (adaptiveCandidateIndex - 1)
                .coerceAtLeast(0)
            renderAdaptiveCutControls()
            seekToAdaptiveCandidate()
        }
        editor.adaptiveNextButton.setOnClickListener {
            if (adaptiveDraftRanges.isEmpty()) return@setOnClickListener
            cancelAdaptivePreview()
            adaptiveCandidateIndex = (adaptiveCandidateIndex + 1)
                .coerceAtMost(adaptiveDraftRanges.lastIndex)
            renderAdaptiveCutControls()
            seekToAdaptiveCandidate()
        }
        editor.adaptivePreviewButton.setOnClickListener { previewAdaptiveCandidate() }
        editor.adaptiveSequencePreviewButton.setOnClickListener {
            if (adaptiveSequencePreviewActive) {
                cancelAdaptivePreview()
            } else {
                previewAdaptiveSequence()
            }
        }
        editor.adaptiveApplySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (adaptiveApplied == isChecked) return@setOnCheckedChangeListener
            adaptiveApplied = isChecked && adaptiveDraftRanges.isNotEmpty()
            renderAdaptiveCutControls()
            onUserChangedAdaptiveCuts()
        }
        editor.adaptiveClearButton.setOnClickListener {
            val wasApplied = adaptiveApplied
            cancelAdaptivePreview()
            adaptiveDraftRanges = emptyList()
            adaptiveCandidateIndex = 0
            adaptiveApplied = false
            editor.adaptiveApplySwitch.isChecked = false
            renderAdaptiveCutControls()
            if (wasApplied) onUserChangedAdaptiveCuts()
        }
    }

    private fun renderAdaptiveCutControls() {
        val renderActive = if (::renderCoordinator.isInitialized) {
            renderCoordinator.currentState.isActiveRender()
        } else {
            false
        }
        val hasDraft = adaptiveDraftRanges.isNotEmpty()
        val selectedRange = adaptiveDraftRanges.getOrNull(adaptiveCandidateIndex)
        editor.adaptivePresetGroup.setChildrenEnabled(!renderActive)
        editor.generateAdaptiveDraftButton.isEnabled = !renderActive && activeMediaInfo != null
        editor.adaptiveReviewGroup.isVisible = hasDraft
        editor.adaptivePreviousButton.isEnabled = !renderActive && adaptiveCandidateIndex > 0
        editor.adaptiveNextButton.isEnabled = !renderActive &&
            adaptiveCandidateIndex < adaptiveDraftRanges.lastIndex
        editor.adaptivePreviewButton.isEnabled = !renderActive && selectedRange != null
        editor.adaptivePreviewButton.setText(
            if (adaptivePreviewActive) R.string.adaptive_previewing else R.string.adaptive_preview,
        )
        editor.adaptiveSequencePreviewButton.isEnabled = !renderActive && hasDraft
        editor.adaptiveSequencePreviewButton.setText(
            if (adaptiveSequencePreviewActive) {
                R.string.adaptive_sequence_stop
            } else {
                R.string.adaptive_sequence_preview
            },
        )
        editor.adaptiveApplySwitch.isEnabled = !renderActive && hasDraft
        editor.adaptiveClearButton.isEnabled = !renderActive

        val keptDurationMs = adaptiveDraftRanges.sumOf { it.durationMs }
        val trimDurationMs = activeMediaInfo?.let { currentTrimRange(it) }?.durationMs
            ?: keptDurationMs
        val removedDurationMs = (trimDurationMs - keptDurationMs).coerceAtLeast(0L)
        editor.adaptiveDraftSummary.text = if (hasDraft) {
            getString(
                if (adaptiveApplied) {
                    R.string.adaptive_applied_summary
                } else {
                    R.string.adaptive_draft_summary
                },
                adaptiveDraftRanges.size,
                MediaFormatters.duration(keptDurationMs),
                MediaFormatters.duration(removedDurationMs),
            )
        } else {
            getString(R.string.adaptive_empty_summary)
        }
        editor.adaptiveApplyNote.setText(
            if (transformEnabled && transitionEnabled) {
                R.string.adaptive_apply_note_with_transition
            } else {
                R.string.adaptive_apply_note
            },
        )
        if (selectedRange != null) {
            editor.adaptiveCandidateTitle.text = getString(
                R.string.adaptive_candidate_title,
                adaptiveCandidateIndex + 1,
                adaptiveDraftRanges.size,
            )
            editor.adaptiveCandidateRange.text = getString(
                R.string.adaptive_candidate_range,
                MediaFormatters.duration(selectedRange.startMs),
                MediaFormatters.duration(selectedRange.endMs),
                MediaFormatters.duration(selectedRange.durationMs),
            )
        }
    }

    private fun previewAdaptiveCandidate() {
        val info = activeMediaInfo ?: return
        val range = adaptiveDraftRanges.getOrNull(adaptiveCandidateIndex) ?: return
        if (renderCoordinator.currentState.isActiveRender()) return
        cancelFreezePreview()
        cancelAdaptivePreview()
        if (compositionPreviewActive) {
            releaseCompositionPreview(attachExoPlayer = true, reason = "adaptive candidate inspection")
            prepareExoPreview(
                info.workingFilePath,
                autoPlay = true,
                startPositionMs = range.startMs,
            )
        } else if (previewPath != info.workingFilePath) {
            prepareExoPreview(
                info.workingFilePath,
                autoPlay = true,
                startPositionMs = range.startMs,
            )
        } else {
            previewPlayer.seekTo(range.startMs)
            previewPlayer.play()
        }
        applyAdaptiveCandidateEffects(range)
        adaptivePreviewActive = true
        syncReplacementAudioPreview(forceSeek = true)
        renderAdaptiveCutControls()
        adaptivePreviewHandler.post(adaptivePreviewCompletion)
    }

    private val adaptivePreviewCompletion = object : Runnable {
        override fun run() {
            if (!adaptivePreviewActive) return
            val range = adaptiveDraftRanges.getOrNull(adaptiveCandidateIndex)
            if (
                range == null ||
                previewPlayer.currentPosition >= range.endMs ||
                previewPlayer.playbackState == Player.STATE_ENDED
            ) {
                previewPlayer.pause()
                adaptivePreviewActive = false
                renderAdaptiveCutControls()
                restoreSourceAfterAdaptivePreview(range?.endMs ?: 0L)
                return
            }
            adaptivePreviewHandler.postDelayed(this, ADAPTIVE_PREVIEW_POLL_MS)
        }
    }

    private fun seekToAdaptiveCandidate() {
        val info = activeMediaInfo ?: return
        val range = adaptiveDraftRanges.getOrNull(adaptiveCandidateIndex) ?: return
        if (previewPath == info.workingFilePath) {
            previewPlayer.pause()
            applyAdaptiveCandidateEffects(range)
            previewPlayer.seekTo(range.startMs)
        }
    }

    private fun previewAdaptiveSequence() {
        val info = activeMediaInfo ?: return
        if (adaptiveDraftRanges.isEmpty() || renderCoordinator.currentState.isActiveRender()) return
        cancelFreezePreview()
        cancelAdaptivePreview()
        if (compositionPreviewActive) {
            releaseCompositionPreview(attachExoPlayer = true, reason = "adaptive sequence inspection")
        }
        previewPath = info.workingFilePath
        editor.videoPreview.player = previewPlayer
        configureSourcePreviewLayout(info)

        val sourceUri = File(info.workingFilePath).toURI().toString()
        val mediaItems = adaptiveDraftRanges.mapIndexed { index, range ->
            MediaItem.Builder()
                .setMediaId("$ADAPTIVE_SEQUENCE_MEDIA_ID_PREFIX$index")
                .setUri(sourceUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(range.startMs)
                        .setEndPositionMs(range.endMs)
                        .build(),
                )
                .build()
        }
        adaptiveCandidateIndex = 0
        adaptiveSequencePreviewActive = true
        realtimePreviewSession.begin(info.workingFilePath, restart = true)
        realtimePreviewSession.clearAppliedGraph()
        applyAdaptiveSequenceEffects(0)
        previewPlayer.setPlaybackSpeed(currentPreviewSpeed())
        previewPlayer.volume = currentPreviewVolume()
        previewPlayer.setMediaItems(mediaItems, true)
        previewPlayer.playWhenReady = true
        previewPlayer.prepare()
        schedulePreviewReadyTimeout()
        syncReplacementAudioPreview(forceSeek = true)
        renderAdaptiveCutControls()
    }

    private fun applyAdaptiveCandidateEffects(range: TrimRange) {
        val info = activeMediaInfo ?: return
        if (previewFallbackActive) {
            previewPlayer.setVideoEffects(emptyList())
            return
        }
        previewPlayer.setVideoEffects(
            TransformVideoEffects.forPreview(
                settings = currentTransformSettings(),
                sourceWidth = info.width,
                sourceHeight = info.height,
                overlays = currentOverlaySettings(),
                timelineOffsetUs = range.startMs * 1_000L,
                sourceDurationMs = range.durationMs,
                realtimeSourceBlurState = realtimeSourceBlurState,
                realtimeImageOverlayState = realtimeImageOverlayState,
            ),
        )
        previewPlayer.setPlaybackSpeed(currentPreviewSpeed())
    }

    private fun applyAdaptiveSequenceEffects(rangeIndex: Int) {
        val info = activeMediaInfo ?: return
        val range = adaptiveDraftRanges.getOrNull(rangeIndex) ?: return
        if (previewFallbackActive) {
            previewPlayer.setVideoEffects(emptyList())
            return
        }
        previewPlayer.setVideoEffects(
            TransformVideoEffects.forPreview(
                settings = currentTransformSettings(),
                sourceWidth = info.width,
                sourceHeight = info.height,
                overlays = currentOverlaySettings(),
                timelineOffsetUs = 0L,
                sourceDurationMs = range.durationMs,
                sourceTimeOffsetUs = range.startMs * 1_000L,
                realtimeSourceBlurState = realtimeSourceBlurState,
                realtimeImageOverlayState = realtimeImageOverlayState,
            ),
        )
    }

    private fun restoreSourceAfterAdaptivePreview(positionMs: Long) {
        val info = activeMediaInfo ?: return
        if (renderCoordinator.currentState.isActiveRender()) return
        val trim = currentTrimRange(info)
        preparePreview(
            workingFilePath = info.workingFilePath,
            autoPlay = false,
            startPositionMs = positionMs.coerceIn(trim.startMs, trim.endMs),
        )
    }

    private fun cancelAdaptivePreview(restoreSource: Boolean = true) {
        adaptivePreviewHandler.removeCallbacks(adaptivePreviewCompletion)
        val wasActive = adaptivePreviewActive || adaptiveSequencePreviewActive
        val restorePositionMs = when {
            adaptiveSequencePreviewActive -> adaptiveDraftRanges
                .getOrNull(previewPlayer.currentMediaItemIndex)
                ?.let { range ->
                    (range.startMs + previewPlayer.currentPosition.coerceAtLeast(0L))
                        .coerceAtMost(range.endMs)
                }
            else -> previewPlayer.currentPosition
        } ?: 0L
        adaptivePreviewActive = false
        adaptiveSequencePreviewActive = false
        if (wasActive && _binding != null) {
            renderAdaptiveCutControls()
            if (restoreSource) restoreSourceAfterAdaptivePreview(restorePositionMs)
        }
    }

    private fun onUserChangedAdaptiveCuts() {
        scheduleEditorPreferencesSave()
        val info = activeMediaInfo ?: return
        cancelAdaptivePreview()
        clipTransitionEditorController.reconcile()
        if (!renderCoordinator.currentState.isActiveRender() &&
            renderCoordinator.currentState !is RenderUiState.Idle
        ) {
            renderCoordinator.reset(mediaHasAudio = renderNeedsAudioCapability(info))
            restoreSourcePreviewAfterStoppedRender()
        }
        requestSourceBlurPreviewUpdate(
            reason = "adaptive cuts",
            immediate = false,
            force = true,
        )
        renderAdaptiveCutControls()
        renderTransformControls()
        updateTrimSummary()
    }

    private fun clearAdaptiveDraft() {
        cancelAdaptivePreview()
        adaptiveDraftRanges = emptyList()
        adaptiveCandidateIndex = 0
        adaptiveApplied = false
        if (_binding != null) {
            if (editor.adaptiveApplySwitch.isChecked) {
                editor.adaptiveApplySwitch.isChecked = false
            }
            renderAdaptiveCutControls()
            renderTransformControls()
            if (::clipTransitionEditorController.isInitialized) {
                clipTransitionEditorController.reconcile()
            }
        }
    }

    private fun bindClipTransitionControls() {
        val parent = editor.editCard.getChildAt(0) as? ViewGroup
            ?: error("Clips editor card must expose a ViewGroup content root")
        clipTransitionEditorController = ClipTransitionEditorController(
            context = this,
            parent = parent,
            selectedRangesProvider = {
                activeMediaInfo?.let(::currentSelectedClipRanges).orEmpty()
            },
            onSettingsChanged = ::onUserChangedClipTransitions,
            onPreviewBoundary = ::previewClipTransitionBoundary,
        )
    }

    private fun currentSelectedClipRanges(info: MediaInfo): List<TrimRange> {
        val trim = currentTrimRange(info)
        return AdaptiveCutCompiler.compile(
            AdaptiveCutSettings(
                enabled = adaptiveApplied,
                preset = adaptivePreset,
                reviewedRanges = adaptiveDraftRanges,
            ),
            trim,
        ) ?: listOf(trim)
    }

    private fun onUserChangedClipTransitions() {
        val info = activeMediaInfo ?: return
        clipTransitionPreviewHandler.removeCallbacks(clipTransitionPreviewCompletion)
        cancelFreezePreview()
        cancelAdaptivePreview()
        clipTransitionEditorController.reconcile()
        if (!renderCoordinator.currentState.isActiveRender() &&
            renderCoordinator.currentState !is RenderUiState.Idle
        ) {
            renderCoordinator.reset(mediaHasAudio = renderNeedsAudioCapability(info))
            restoreSourcePreviewAfterStoppedRender()
        }
        requestSourceBlurPreviewUpdate(
            reason = "clip crossfade boundary",
            immediate = true,
            force = true,
        )
        updateTrimSummary()
    }

    private fun previewClipTransitionBoundary(
        left: TrimRange,
        right: TrimRange,
        enabled: Boolean,
    ) {
        val info = activeMediaInfo ?: return
        if (renderCoordinator.currentState.isActiveRender()) return
        clipTransitionPreviewHandler.removeCallbacks(clipTransitionPreviewCompletion)
        cancelFreezePreview()
        cancelAdaptivePreview()
        if (enabled && !compositionPreviewActive) {
            requestSourceBlurPreviewUpdate(
                reason = "prepare Crossfade boundary preview",
                immediate = true,
                force = true,
            )
            if (!compositionPreviewActive) {
                Snackbar.make(
                    binding.mainRoot,
                    R.string.clip_transition_preview_unavailable,
                    Snackbar.LENGTH_LONG,
                ).show()
                return
            }
        }
        val sourceLeadMs = (CLIP_TRANSITION_PREVIEW_LEAD_MS * currentPreviewSpeed())
            .roundToLong()
        val startSourceMs = (left.endMs - sourceLeadMs).coerceIn(left.startMs, left.endMs)
        activePreviewPause()
        activePreviewSeekToSourcePosition(info, startSourceMs)
        activePreviewPlay()
        clipTransitionPreviewHandler.postDelayed(
            clipTransitionPreviewCompletion,
            CLIP_TRANSITION_PREVIEW_WINDOW_MS,
        )
        Log.d(
            TAG_PREVIEW,
            "Boundary preview ${left.endMs}->${right.startMs} enabled=$enabled start=$startSourceMs",
        )
    }

    private val clipTransitionPreviewCompletion = Runnable {
        if (_binding != null) activePreviewPause()
    }

    private fun bindReviewEditorTabs() {
        editor.reviewEditorTabGroup.check(
            when (selectedReviewEditorTab) {
                ReviewEditorTab.CLIPS -> R.id.reviewClipsTabButton
                ReviewEditorTab.TRANSFORM -> R.id.reviewTransformTabButton
                ReviewEditorTab.AUDIO -> R.id.reviewAudioTabButton
                ReviewEditorTab.OVERLAY -> R.id.reviewOverlayTabButton
                ReviewEditorTab.EXPORT -> R.id.reviewExportTabButton
            },
        )
        renderReviewEditorTab()
        editor.reviewEditorTabGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedReviewEditorTab = when (checkedId) {
                R.id.reviewTransformTabButton -> ReviewEditorTab.TRANSFORM
                R.id.reviewAudioTabButton -> ReviewEditorTab.AUDIO
                R.id.reviewOverlayTabButton -> ReviewEditorTab.OVERLAY
                R.id.reviewExportTabButton -> ReviewEditorTab.EXPORT
                else -> ReviewEditorTab.CLIPS
            }
            scheduleEditorPreferencesSave()
            renderReviewEditorTab()
        }
    }

    private fun renderReviewEditorTab() {
        editor.editCard.isVisible = selectedReviewEditorTab == ReviewEditorTab.CLIPS
        editor.transformCard.isVisible = selectedReviewEditorTab == ReviewEditorTab.TRANSFORM
        editor.audioCard.isVisible = selectedReviewEditorTab == ReviewEditorTab.AUDIO
        editor.overlayCard.isVisible = selectedReviewEditorTab == ReviewEditorTab.OVERLAY
        editor.exportCard.isVisible = selectedReviewEditorTab == ReviewEditorTab.EXPORT
        editor.renderCard.isVisible = selectedReviewEditorTab == ReviewEditorTab.EXPORT
        renderSourceBlurGuide()
    }

    private fun bindExportQualityControls() {
        editor.exportQualityGroup.check(renderPresetButtonId(selectedRenderPreset))
        editor.exportApplyDurationButton.setOnClickListener { applyDurationFitSuggestion() }
        renderExportQualityControls()
        editor.exportQualityGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val selected = when (checkedId) {
                R.id.exportQuality720Button -> RenderPreset.HD_720P
                R.id.exportQuality2kButton -> RenderPreset.QHD_2K
                else -> RenderPreset.FULL_HD_1080P
            }
            if (selected == selectedRenderPreset) return@addOnButtonCheckedListener
            selectedRenderPreset = selected
            onUserChangedRenderPreset()
        }
    }

    private fun onUserChangedRenderPreset() {
        scheduleEditorPreferencesSave()
        val info = activeMediaInfo
        if (
            info != null &&
            !renderCoordinator.currentState.isActiveRender() &&
            renderCoordinator.currentState !is RenderUiState.Idle
        ) {
            publicExportCoordinator.reset()
            renderCoordinator.reset(mediaHasAudio = renderNeedsAudioCapability(info))
            restoreSourcePreviewAfterStoppedRender()
        } else {
            renderRenderState(renderCoordinator.currentState)
        }
        renderPublicExportState(publicExportCoordinator.currentState)
        renderExportQualityControls()
    }

    private fun renderExportQualityControls() {
        val renderActive = ::renderCoordinator.isInitialized &&
            renderCoordinator.currentState.isActiveRender()
        val publishActive = ::publicExportCoordinator.isInitialized &&
            publicExportCoordinator.currentState is PublicExportUiState.Publishing
        editor.exportQualityGroup.setChildrenEnabled(!renderActive && !publishActive)
        editor.exportQualityDetail.setText(
            when (selectedRenderPreset) {
                RenderPreset.HD_720P -> R.string.export_quality_720p_detail
                RenderPreset.FULL_HD_1080P -> R.string.export_quality_1080p_detail
                RenderPreset.QHD_2K -> R.string.export_quality_2k_detail
            },
        )
        renderDurationFitAdvisor()
    }

    private fun renderDurationFitAdvisor() {
        val info = activeMediaInfo
        if (info == null || !::renderCoordinator.isInitialized) {
            editor.exportDurationAdvisorTitle.setText(R.string.export_duration_advisor_title)
            editor.exportDurationAdvisorDetail.setText(R.string.export_duration_advisor_waiting)
            editor.exportApplyDurationButton.isVisible = false
            return
        }

        val assessment = DurationFitAdvisor.assess(currentEditPlan(selectedRenderPreset))
        val toleranceMs = RenderedOutputValidationPolicy.allowedDurationDriftMs(
            assessment.plannedDurationMs,
        )
        editor.exportDurationAdvisorTitle.setText(R.string.export_duration_advisor_title)
        when {
            assessment.isWholeSecondAligned -> {
                editor.exportDurationAdvisorDetail.text = getString(
                    R.string.export_duration_aligned,
                    exactDurationText(assessment.plannedDurationMs),
                    toleranceMs,
                )
                editor.exportApplyDurationButton.isVisible = false
            }
            assessment.canApply -> {
                editor.exportDurationAdvisorDetail.text = getString(
                    R.string.export_duration_suggestion,
                    exactDurationText(assessment.plannedDurationMs),
                    MediaFormatters.duration(assessment.suggestedDurationMs),
                    if (assessment.adjustmentMs >= 0L) "+" else "−",
                    abs(assessment.adjustmentMs),
                    toleranceMs,
                )
                editor.exportApplyDurationButton.isVisible = true
                editor.exportApplyDurationButton.text = getString(
                    R.string.export_duration_apply,
                    MediaFormatters.duration(assessment.suggestedDurationMs),
                )
                val renderActive = renderCoordinator.currentState.isActiveRender()
                val publishActive = ::publicExportCoordinator.isInitialized &&
                    publicExportCoordinator.currentState is PublicExportUiState.Publishing
                editor.exportApplyDurationButton.isEnabled = !renderActive && !publishActive
            }
            else -> {
                editor.exportDurationAdvisorDetail.text = getString(
                    R.string.export_duration_unavailable,
                    exactDurationText(assessment.plannedDurationMs),
                    MediaFormatters.duration(assessment.suggestedDurationMs),
                    if (assessment.adjustmentMs >= 0L) "+" else "−",
                    abs(assessment.adjustmentMs),
                )
                editor.exportApplyDurationButton.isVisible = false
            }
        }
    }

    private fun applyDurationFitSuggestion() {
        val info = activeMediaInfo ?: return
        if (renderCoordinator.currentState.isActiveRender()) return
        if (publicExportCoordinator.currentState is PublicExportUiState.Publishing) return

        val assessment = DurationFitAdvisor.assess(currentEditPlan(selectedRenderPreset))
        val update = assessment.update
        if (update == null) {
            Snackbar.make(
                binding.mainRoot,
                R.string.export_duration_update_unavailable,
                Snackbar.LENGTH_SHORT,
            ).show()
            renderDurationFitAdvisor()
            return
        }

        if (update.reviewedRanges != null && adaptiveApplied) {
            adaptiveDraftRanges = update.reviewedRanges
            adaptiveCandidateIndex = adaptiveCandidateIndex.coerceIn(
                0,
                adaptiveDraftRanges.lastIndex,
            )
            onUserChangedAdaptiveCuts()
        } else {
            editor.trimRangeSlider.values = listOf(
                update.trimRange.startMs / 1_000f,
                update.trimRange.endMs / 1_000f,
            )
            onUserChangedTrim()
        }

        Snackbar.make(
            binding.mainRoot,
            getString(
                R.string.export_duration_updated,
                MediaFormatters.duration(assessment.suggestedDurationMs),
            ),
            Snackbar.LENGTH_SHORT,
        ).show()
        renderDurationFitAdvisor()
    }

    private fun bindAudioControls() {
        editor.audioEnabledSwitch.isChecked = audioEnabled
        editor.audioPolicyGroup.check(audioPolicyButtonId(audioPolicy))
        editor.audioVolumeSlider.value = audioVolume * 100f
        editor.mixSourceVolumeSlider.value = mixSourceVolume * 100f
        editor.mixAddedVolumeSlider.value = mixAddedVolume * 100f
        renderAudioControls()

        editor.audioEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (audioEnabled != isChecked) {
                audioEnabled = isChecked
                renderAudioControls()
                onUserChangedAudio()
            }
        }
        editor.audioPolicyGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val selected = when (checkedId) {
                R.id.audioMuteButton -> AudioPolicy.MUTE
                R.id.audioReplaceButton -> AudioPolicy.REPLACE
                R.id.audioMixButton -> AudioPolicy.MIX
                else -> AudioPolicy.KEEP_ORIGINAL
            }
            if (audioPolicy != selected) {
                audioPolicy = selected
                renderAudioControls()
                onUserChangedAudio()
            }
        }
        editor.audioVolumeSlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val selectedVolume = (value / 100f)
                .coerceIn(AudioCompiler.MIN_LINEAR_GAIN, AudioCompiler.MAX_LINEAR_GAIN)
            if (abs(audioVolume - selectedVolume) > 0.0001f) {
                audioVolume = selectedVolume
                renderAudioControls()
                onUserChangedAudio()
            }
        }
        editor.audioVolumeResetButton.setOnClickListener {
            if (audioVolume != AudioCompiler.UNITY_LINEAR_GAIN) {
                audioVolume = AudioCompiler.UNITY_LINEAR_GAIN
                editor.audioVolumeSlider.value = audioVolume * 100f
                renderAudioControls()
                onUserChangedAudio()
            }
        }
        editor.mixSourceVolumeSlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val selectedVolume = (value / 100f)
                .coerceIn(AudioCompiler.MIN_LINEAR_GAIN, AudioCompiler.MAX_LINEAR_GAIN)
            if (abs(mixSourceVolume - selectedVolume) > 0.0001f) {
                mixSourceVolume = selectedVolume
                renderAudioControls()
                onUserChangedAudio()
            }
        }
        editor.mixAddedVolumeSlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val selectedVolume = (value / 100f)
                .coerceIn(AudioCompiler.MIN_LINEAR_GAIN, AudioCompiler.MAX_LINEAR_GAIN)
            if (abs(mixAddedVolume - selectedVolume) > 0.0001f) {
                mixAddedVolume = selectedVolume
                renderAudioControls()
                onUserChangedAudio()
            }
        }
        editor.mixVolumeResetButton.setOnClickListener {
            val sourceChanged = mixSourceVolume != AudioCompiler.DEFAULT_MIX_SOURCE_LINEAR_GAIN
            val addedChanged = mixAddedVolume != AudioCompiler.DEFAULT_MIX_LINEAR_GAIN
            if (sourceChanged || addedChanged) {
                mixSourceVolume = AudioCompiler.DEFAULT_MIX_SOURCE_LINEAR_GAIN
                mixAddedVolume = AudioCompiler.DEFAULT_MIX_LINEAR_GAIN
                editor.mixSourceVolumeSlider.value = mixSourceVolume * 100f
                editor.mixAddedVolumeSlider.value = mixAddedVolume * 100f
                renderAudioControls()
                onUserChangedAudio()
            }
        }
        editor.replacementAudioChooseButton.setOnClickListener {
            openReplacementAudioPicker()
        }
        editor.replacementAudioClearButton.setOnClickListener {
            val selected = replacementAudioAsset
            replacementAudioAsset = null
            stopReplacementAudioPreview(clearMedia = true)
            replacementAudioImportCoordinator.clear(selected)
            renderAudioControls()
            onUserChangedAudio()
        }
    }

    private fun renderAudioControls() {
        val renderActive = if (::renderCoordinator.isInitialized) {
            renderCoordinator.currentState.isActiveRender()
        } else {
            false
        }
        editor.audioEnabledSwitch.isEnabled = !renderActive
        editor.audioPolicyGroup.setChildrenEnabled(audioEnabled && !renderActive)
        editor.audioPolicyGroup.alpha = if (audioEnabled) 1f else 0.46f
        val replaceSelected = audioEnabled && audioPolicy == AudioPolicy.REPLACE
        val mixSelected = audioEnabled && audioPolicy == AudioPolicy.MIX
        val externalAudioSelected = replaceSelected || mixSelected
        val volumeAvailable = audioEnabled &&
            (audioPolicy == AudioPolicy.KEEP_ORIGINAL || audioPolicy == AudioPolicy.REPLACE)
        editor.replacementAudioControlsGroup.isVisible = externalAudioSelected
        editor.replacementAudioProgress.isVisible =
            externalAudioSelected && replacementAudioImporting
        editor.replacementAudioChooseButton.isEnabled =
            externalAudioSelected && !replacementAudioImporting && !renderActive
        editor.replacementAudioClearButton.isVisible = replacementAudioAsset != null
        editor.replacementAudioClearButton.isEnabled =
            externalAudioSelected && !replacementAudioImporting && !renderActive
        editor.externalAudioTrackLabel.setText(
            if (mixSelected) R.string.audio_mix_track_label else R.string.audio_replace_track_label,
        )
        editor.externalAudioDurationPolicy.setText(
            if (mixSelected) {
                R.string.audio_mix_duration_policy
            } else {
                R.string.audio_replace_duration_policy
            },
        )
        editor.replacementAudioChooseButton.setText(
            if (replacementAudioAsset == null) {
                R.string.audio_replace_choose
            } else {
                R.string.audio_replace_change
            },
        )
        editor.replacementAudioSummary.text = when {
            replacementAudioImporting -> getString(
                R.string.audio_replace_importing,
                replacementAudioImportName
                    ?: replacementAudioAsset?.displayName
                    ?: getString(R.string.audio_replace_track_label),
            )
            replacementAudioAsset == null -> getString(R.string.audio_replace_required)
            else -> getString(
                R.string.audio_replace_selected,
                checkNotNull(replacementAudioAsset).displayName,
                MediaFormatters.duration(checkNotNull(replacementAudioAsset).durationMs),
                MediaFormatters.fileSize(this, checkNotNull(replacementAudioAsset).fileSizeBytes),
            )
        }
        editor.audioVolumeControlsGroup.isVisible = volumeAvailable
        editor.audioVolumeSlider.isEnabled = volumeAvailable && !renderActive
        editor.audioVolumeResetButton.isEnabled =
            volumeAvailable && !renderActive && audioVolume != AudioCompiler.UNITY_LINEAR_GAIN
        editor.audioVolumeValue.text = getString(
            R.string.audio_volume_value,
            (audioVolume * 100f).roundToInt(),
        )
        editor.audioMixVolumeControlsGroup.isVisible = mixSelected
        editor.mixSourceVolumeSlider.isEnabled = mixSelected && !renderActive
        editor.mixAddedVolumeSlider.isEnabled = mixSelected && !renderActive
        editor.mixVolumeResetButton.isEnabled = mixSelected && !renderActive &&
            (
                mixSourceVolume != AudioCompiler.DEFAULT_MIX_SOURCE_LINEAR_GAIN ||
                    mixAddedVolume != AudioCompiler.DEFAULT_MIX_LINEAR_GAIN
                )
        editor.mixSourceVolumeValue.text = getString(
            R.string.audio_mix_source_volume_value,
            (mixSourceVolume * 100f).roundToInt(),
        )
        editor.mixAddedVolumeValue.text = getString(
            R.string.audio_mix_added_volume_value,
            (mixAddedVolume * 100f).roundToInt(),
        )
        editor.audioBadge.setText(
            if (audioEnabled) R.string.audio_badge_on else R.string.audio_badge_off,
        )
        editor.audioSummary.text = when {
            !audioEnabled -> getString(R.string.audio_off_summary)
            audioPolicy == AudioPolicy.MUTE -> getString(R.string.audio_mute_summary)
            audioPolicy == AudioPolicy.REPLACE && replacementAudioAsset == null ->
                getString(R.string.audio_replace_missing_summary)
            audioPolicy == AudioPolicy.REPLACE -> getString(
                R.string.audio_replace_summary,
                checkNotNull(replacementAudioAsset).displayName,
                (audioVolume * 100f).roundToInt(),
            )
            audioPolicy == AudioPolicy.MIX && replacementAudioAsset == null ->
                getString(R.string.audio_mix_missing_summary)
            audioPolicy == AudioPolicy.MIX -> getString(
                R.string.audio_mix_summary,
                checkNotNull(replacementAudioAsset).displayName,
                (mixSourceVolume * 100f).roundToInt(),
                (mixAddedVolume * 100f).roundToInt(),
            )
            else -> getString(
                R.string.audio_keep_volume_summary,
                (audioVolume * 100f).roundToInt(),
            )
        }
    }

    private fun onUserChangedAudio() {
        scheduleEditorPreferencesSave()
        val info = activeMediaInfo ?: return
        cancelFreezePreview()
        cancelAdaptivePreview()
        if (!renderCoordinator.currentState.isActiveRender()) {
            val restorePreview = renderCoordinator.currentState !is RenderUiState.Idle
            renderCoordinator.reset(mediaHasAudio = renderNeedsAudioCapability(info))
            if (restorePreview) restoreSourcePreviewAfterStoppedRender()
        }
        requestSourceBlurPreviewUpdate(
            reason = "audio controls",
            immediate = false,
            force = true,
        )
        refreshAudioPreview()
        updateTrimSummary()
    }

    private fun bindOverlayControls() {
        editor.overlayEnabledSwitch.isChecked = overlayEnabled
        editor.sourceBlurEnabledSwitch.isChecked = sourceSubtitleBlurEnabled
        editor.sourceBlurStrengthSlider.value = sourceSubtitleBlurStrength
        editor.imageOverlayEnabledSwitch.isChecked = imageOverlayEnabled
        editor.imageOverlayXSlider.value = imageOverlayCenterX * 100f
        editor.imageOverlayYSlider.value = imageOverlayCenterY * 100f
        editor.imageOverlaySizeSlider.value = imageOverlayWidthFraction * 100f
        editor.imageOverlayOpacitySlider.value = imageOverlayOpacity * 100f
        renderOverlayControls()

        editor.overlayEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (overlayEnabled != isChecked) {
                overlayEnabled = isChecked
                ensureSourceBlurRange()
                ensureImageOverlayRange()
                renderOverlayControls()
                onUserChangedOverlay()
            }
        }
        editor.overlayVisibilityButton.setOnClickListener {
            overlayDetailsVisible = !overlayDetailsVisible
            renderOverlayControls()
            scheduleEditorPreferencesSave()
        }
        editor.sourceBlurEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (sourceSubtitleBlurEnabled != isChecked) {
                sourceSubtitleBlurEnabled = isChecked
                ensureSourceBlurRange()
                renderOverlayControls()
                onUserChangedOverlay()
            }
        }
        editor.sourceBlurTimeRangeSlider.addOnChangeListener { slider, _, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val values = slider.values.sorted()
            val info = activeMediaInfo ?: return@addOnChangeListener
            val startMs = ((values.firstOrNull() ?: 0f) * 1_000f)
                .roundToLong()
                .coerceIn(0L, info.durationMs)
            val endMs = ((values.lastOrNull() ?: info.durationMs / 1_000f) * 1_000f)
                .roundToLong()
                .coerceIn(startMs, info.durationMs)
            if (startMs != sourceSubtitleBlurStartMs || endMs != sourceSubtitleBlurEndMs) {
                sourceSubtitleBlurStartMs = startMs
                sourceSubtitleBlurEndMs = endMs
                sourceSubtitleBlurRangeInitialized = true
                sourceSubtitleBlurRangeFollowsTrim = false
                renderOverlayControls()
                onUserChangedOverlay(
                    throttleSourceBlurPreview = true,
                    reason = "source blur time range",
                )
            }
        }
        editor.sourceBlurStrengthSlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            if (sourceSubtitleBlurStrength != value) {
                sourceSubtitleBlurStrength = value
                renderOverlayControls()
                onUserChangedOverlay(
                    throttleSourceBlurPreview = true,
                    reason = "source blur strength",
                )
            }
        }
        listOf(
            editor.sourceBlurXSlider,
            editor.sourceBlurYSlider,
            editor.sourceBlurWidthSlider,
            editor.sourceBlurHeightSlider,
        ).forEach { slider ->
            slider.addOnChangeListener { _, _, fromUser ->
                if (!fromUser) return@addOnChangeListener
                val width = (editor.sourceBlurWidthSlider.value / 100f)
                    .coerceIn(BlurRectangle.MIN_BLUR_SPAN, 1f)
                val height = (editor.sourceBlurHeightSlider.value / 100f)
                    .coerceIn(BlurRectangle.MIN_BLUR_SPAN, 1f)
                val left = (editor.sourceBlurXSlider.value / 100f)
                    .coerceIn(0f, 1f - width)
                val top = (editor.sourceBlurYSlider.value / 100f)
                    .coerceIn(0f, 1f - height)
                updateSourceBlurRectangle(
                    BlurRectangle(left, top, left + width, top + height),
                )
            }
        }
        editor.sourceBlurResetButton.setOnClickListener {
            sourceSubtitleBlurRectangle = BlurRectangle()
            sourceSubtitleBlurStrength = OverlayCompiler.DEFAULT_BLUR_STRENGTH
            sourceSubtitleBlurRangeInitialized = false
            sourceSubtitleBlurRangeFollowsTrim = true
            ensureSourceBlurRange()
            renderOverlayControls()
            onUserChangedOverlay()
        }
        editor.imageOverlayEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (imageOverlayEnabled != isChecked) {
                imageOverlayEnabled = isChecked
                ensureImageOverlayRange()
                renderOverlayControls()
                onUserChangedOverlay(reason = "image overlay switch")
            }
        }
        editor.imageOverlayChooseButton.setOnClickListener { openImageOverlayPicker() }
        editor.imageOverlayRemoveButton.setOnClickListener {
            val selected = imageOverlayAsset
            imageOverlayEnabled = false
            editor.imageOverlayEnabledSwitch.isChecked = false
            imageOverlayImportCoordinator.clear(selected)
            renderOverlayControls()
            onUserChangedOverlay(reason = "image overlay removed")
        }
        editor.imageOverlayPositionGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val preset = when (checkedId) {
                R.id.imageOverlayTopLeftButton -> ImageOverlayPositionPreset.TOP_LEFT
                R.id.imageOverlayTopRightButton -> ImageOverlayPositionPreset.TOP_RIGHT
                R.id.imageOverlayCenterButton -> ImageOverlayPositionPreset.CENTER
                R.id.imageOverlayBottomLeftButton -> ImageOverlayPositionPreset.BOTTOM_LEFT
                R.id.imageOverlayBottomRightButton -> ImageOverlayPositionPreset.BOTTOM_RIGHT
                else -> return@addOnButtonCheckedListener
            }
            if (imageOverlayCenterX != preset.centerX || imageOverlayCenterY != preset.centerY) {
                imageOverlayCenterX = preset.centerX
                imageOverlayCenterY = preset.centerY
                renderImageOverlayControls()
                onUserChangedOverlay(reason = "image position preset")
            }
        }
        editor.imageOverlayXSlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val updated = (value / 100f).coerceIn(0f, 1f)
            if (imageOverlayCenterX != updated) {
                imageOverlayCenterX = updated
                editor.imageOverlayPositionGroup.clearChecked()
                renderImageOverlayControls()
                onUserChangedOverlay(
                    throttleSourceBlurPreview = true,
                    reason = "image horizontal position",
                )
            }
        }
        editor.imageOverlayYSlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val updated = (value / 100f).coerceIn(0f, 1f)
            if (imageOverlayCenterY != updated) {
                imageOverlayCenterY = updated
                editor.imageOverlayPositionGroup.clearChecked()
                renderImageOverlayControls()
                onUserChangedOverlay(
                    throttleSourceBlurPreview = true,
                    reason = "image vertical position",
                )
            }
        }
        editor.imageOverlaySizeSlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val updated = (value / 100f).coerceIn(
                OverlayCompiler.MIN_IMAGE_WIDTH_FRACTION,
                OverlayCompiler.MAX_IMAGE_WIDTH_FRACTION,
            )
            if (imageOverlayWidthFraction != updated) {
                imageOverlayWidthFraction = updated
                renderImageOverlayControls()
                onUserChangedOverlay(
                    throttleSourceBlurPreview = true,
                    reason = "image size",
                )
            }
        }
        editor.imageOverlayOpacitySlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val updated = (value / 100f).coerceIn(
                OverlayCompiler.MIN_IMAGE_OPACITY,
                OverlayCompiler.MAX_IMAGE_OPACITY,
            )
            if (imageOverlayOpacity != updated) {
                imageOverlayOpacity = updated
                renderImageOverlayControls()
                onUserChangedOverlay(
                    throttleSourceBlurPreview = true,
                    reason = "image opacity",
                )
            }
        }
        editor.imageOverlayTimeRangeSlider.addOnChangeListener { slider, _, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val info = activeMediaInfo ?: return@addOnChangeListener
            val values = slider.values.sorted()
            val startMs = ((values.firstOrNull() ?: 0f) * 1_000f)
                .roundToLong()
                .coerceIn(0L, info.durationMs)
            val endMs = ((values.lastOrNull() ?: info.durationMs / 1_000f) * 1_000f)
                .roundToLong()
                .coerceIn(startMs, info.durationMs)
            if (startMs != imageOverlayStartMs || endMs != imageOverlayEndMs) {
                imageOverlayStartMs = startMs
                imageOverlayEndMs = endMs
                imageOverlayRangeInitialized = true
                imageOverlayRangeFollowsTrim = false
                renderImageOverlayControls()
                onUserChangedOverlay(
                    throttleSourceBlurPreview = true,
                    reason = "image time range",
                )
            }
        }
        editor.imageOverlayResetButton.setOnClickListener {
            imageOverlayCenterX = OverlayCompiler.DEFAULT_IMAGE_CENTER_X
            imageOverlayCenterY = OverlayCompiler.DEFAULT_IMAGE_CENTER_Y
            imageOverlayWidthFraction = OverlayCompiler.DEFAULT_IMAGE_WIDTH_FRACTION
            imageOverlayOpacity = OverlayCompiler.DEFAULT_IMAGE_OPACITY
            imageOverlayRangeInitialized = false
            imageOverlayRangeFollowsTrim = true
            ensureImageOverlayRange()
            renderImageOverlayControls()
            onUserChangedOverlay(reason = "image controls reset")
        }
        bindSourceBlurGuideGestures()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindSourceBlurGuideGestures() {
        if (!SOURCE_BLUR_DIRECT_TOUCH_ENABLED) {
            // TEMPORARY_SAFETY_ROLLBACK: owner device testing still terminates the
            // Activity after ACTION_UP even though drag frames themselves are stable.
            // Keep the typed rectangle, realtime guide, sliders and export executor,
            // but do not install either touch target until the release-time platform /
            // Media3 interaction can be reproduced with a complete crash trace.
            cancelSourceBlurGestureCommit(resetGuide = true)
            editor.sourceBlurRegionGuide.apply {
                setOnTouchListener(null)
                isClickable = false
                isFocusable = false
                contentDescription = getString(R.string.source_blur_region_slider_description)
            }
            editor.sourceBlurRegionResizeHandle.apply {
                setOnTouchListener(null)
                isClickable = false
                isFocusable = false
                isVisible = false
            }
            Log.i(TAG_SOURCE_BLUR, "Direct-touch blur geometry is temporarily disabled")
            return
        }

        editor.sourceBlurRegionGuide.isClickable = true
        editor.sourceBlurRegionGuide.isFocusable = true
        editor.sourceBlurRegionResizeHandle.isVisible = true
        editor.sourceBlurRegionResizeHandle.isClickable = true
        editor.sourceBlurRegionResizeHandle.isFocusable = true
        var dragStartRawX = 0f
        var dragStartRawY = 0f
        var dragStartRectangle = sourceSubtitleBlurRectangle
        var dragPendingRectangle = sourceSubtitleBlurRectangle
        var dragGestureActive = false
        editor.sourceBlurRegionGuide.setOnTouchListener { guide, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN && !canEditSourceBlur()) {
                return@setOnTouchListener false
            }
            if (!dragGestureActive && event.actionMasked != MotionEvent.ACTION_DOWN) {
                return@setOnTouchListener false
            }
            try {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        cancelSourceBlurGestureCommit(resetGuide = false)
                        resetSourceBlurGesturePreview()
                        dragStartRawX = event.rawX
                        dragStartRawY = event.rawY
                        dragStartRectangle = sourceSubtitleBlurRectangle
                        dragPendingRectangle = dragStartRectangle
                        dragGestureActive = true
                        guide.parent?.requestDisallowInterceptTouchEvent(true)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val cardWidth = editor.previewCard.width.coerceAtLeast(1).toFloat()
                        val cardHeight = editor.previewCard.height.coerceAtLeast(1).toFloat()
                        val deltaX = (event.rawX - dragStartRawX) / cardWidth
                        val deltaY = (event.rawY - dragStartRawY) / cardHeight
                        val left = (dragStartRectangle.left + deltaX)
                            .coerceIn(0f, 1f - dragStartRectangle.width)
                        val top = (dragStartRectangle.top + deltaY)
                            .coerceIn(0f, 1f - dragStartRectangle.height)
                        dragPendingRectangle = BlurRectangle(
                            left = left,
                            top = top,
                            right = left + dragStartRectangle.width,
                            bottom = top + dragStartRectangle.height,
                        )

                        // TOUCH_GESTURE_VISUAL_ONLY: never mutate layout params, sliders,
                        // typed settings, or the Media3 effect graph while this view owns
                        // the active pointer stream. Commit one rectangle on release.
                        guide.translationX =
                            (dragPendingRectangle.left - dragStartRectangle.left) * cardWidth
                        guide.translationY =
                            (dragPendingRectangle.top - dragStartRectangle.top) * cardHeight
                        true
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        dragGestureActive = false
                        guide.parent?.requestDisallowInterceptTouchEvent(false)
                        scheduleSourceBlurGestureCommit(
                            rectangle = dragPendingRectangle,
                            reason = "source blur drag release",
                        )
                        true
                    }
                    else -> false
                }
            } catch (error: RuntimeException) {
                dragGestureActive = false
                guide.parent?.requestDisallowInterceptTouchEvent(false)
                cancelSourceBlurGestureCommit(resetGuide = true)
                Log.e(
                    TAG_SOURCE_BLUR,
                    "Blur guide drag failed; action=${event.actionMasked}",
                    error,
                )
                true
            }
        }

        var resizeStartRawX = 0f
        var resizeStartRawY = 0f
        var resizeStartRectangle = sourceSubtitleBlurRectangle
        var resizePendingRectangle = sourceSubtitleBlurRectangle
        var resizeGestureActive = false
        editor.sourceBlurRegionResizeHandle.setOnTouchListener { handle, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN && !canEditSourceBlur()) {
                return@setOnTouchListener false
            }
            if (!resizeGestureActive && event.actionMasked != MotionEvent.ACTION_DOWN) {
                return@setOnTouchListener false
            }
            try {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        cancelSourceBlurGestureCommit(resetGuide = false)
                        resetSourceBlurGesturePreview()
                        resizeStartRawX = event.rawX
                        resizeStartRawY = event.rawY
                        resizeStartRectangle = sourceSubtitleBlurRectangle
                        resizePendingRectangle = resizeStartRectangle
                        resizeGestureActive = true
                        handle.parent?.requestDisallowInterceptTouchEvent(true)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val cardWidth = editor.previewCard.width.coerceAtLeast(1).toFloat()
                        val cardHeight = editor.previewCard.height.coerceAtLeast(1).toFloat()
                        val right = (resizeStartRectangle.right +
                            (event.rawX - resizeStartRawX) / cardWidth).coerceIn(
                            resizeStartRectangle.left + BlurRectangle.MIN_BLUR_SPAN,
                            1f,
                        )
                        val bottom = (resizeStartRectangle.bottom +
                            (event.rawY - resizeStartRawY) / cardHeight).coerceIn(
                            resizeStartRectangle.top + BlurRectangle.MIN_BLUR_SPAN,
                            1f,
                        )
                        resizePendingRectangle = resizeStartRectangle.copy(
                            right = right,
                            bottom = bottom,
                        )

                        // TOUCH_GESTURE_VISUAL_ONLY: scale the guide compositor only.
                        // A layout pass, slider writes, and the GL graph wait for release.
                        editor.sourceBlurRegionGuide.apply {
                            pivotX = 0f
                            pivotY = 0f
                            scaleX = resizePendingRectangle.width / resizeStartRectangle.width
                            scaleY = resizePendingRectangle.height / resizeStartRectangle.height
                        }
                        true
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        resizeGestureActive = false
                        handle.parent?.requestDisallowInterceptTouchEvent(false)
                        scheduleSourceBlurGestureCommit(
                            rectangle = resizePendingRectangle,
                            reason = "source blur resize release",
                        )
                        true
                    }
                    else -> false
                }
            } catch (error: RuntimeException) {
                resizeGestureActive = false
                handle.parent?.requestDisallowInterceptTouchEvent(false)
                cancelSourceBlurGestureCommit(resetGuide = true)
                Log.e(
                    TAG_SOURCE_BLUR,
                    "Blur guide resize failed; action=${event.actionMasked}",
                    error,
                )
                true
            }
        }
    }

    private fun resetSourceBlurGesturePreview() {
        editor.sourceBlurRegionGuide.apply {
            translationX = 0f
            translationY = 0f
            pivotX = 0f
            pivotY = 0f
            scaleX = 1f
            scaleY = 1f
        }
    }

    private fun scheduleSourceBlurGestureCommit(
        rectangle: BlurRectangle,
        reason: String,
    ) {
        cancelSourceBlurGestureCommit(resetGuide = false)
        val expectedSourcePath = activeMediaInfo?.workingFilePath
        if (expectedSourcePath == null || !rectangle.isValid()) {
            resetSourceBlurGesturePreview()
            return
        }
        val runnable = Runnable {
            sourceBlurGestureCommitRunnable = null
            if (_binding == null) return@Runnable
            try {
                // RELEASE_COMMIT_AFTER_TOUCH_DISPATCH: restore compositor state and
                // mutate layout/sliders only from the next animation frame, never from
                // the ACTION_UP/ACTION_CANCEL input callback that owns the touch target.
                resetSourceBlurGesturePreview()
                val sourceIsUnchanged = activeMediaInfo?.workingFilePath == expectedSourcePath
                if (!sourceIsUnchanged || !canEditSourceBlur()) {
                    renderSourceBlurGuide()
                    Log.d(TAG_SOURCE_BLUR, "Deferred blur gesture commit discarded")
                    return@Runnable
                }
                updateSourceBlurRectangle(
                    rectangle = rectangle,
                    throttlePreview = true,
                    reason = reason,
                )
                Log.d(TAG_SOURCE_BLUR, "Deferred blur gesture commit applied: $reason")
            } catch (error: RuntimeException) {
                Log.e(TAG_SOURCE_BLUR, "Deferred blur gesture commit failed: $reason", error)
                runCatching {
                    resetSourceBlurGesturePreview()
                    renderSourceBlurGuide()
                }.onFailure { cleanupError ->
                    Log.e(TAG_SOURCE_BLUR, "Blur guide cleanup failed", cleanupError)
                }
            }
        }
        sourceBlurGestureCommitRunnable = runnable
        editor.previewCard.postOnAnimation(runnable)
        Log.d(TAG_SOURCE_BLUR, "Blur gesture commit scheduled after touch dispatch: $reason")
    }

    private fun cancelSourceBlurGestureCommit(resetGuide: Boolean) {
        val pending = sourceBlurGestureCommitRunnable
        if (pending != null && _binding != null) {
            editor.previewCard.removeCallbacks(pending)
        }
        sourceBlurGestureCommitRunnable = null
        if (resetGuide && _binding != null) resetSourceBlurGesturePreview()
    }

    private fun updateSourceBlurRectangle(
        rectangle: BlurRectangle,
        throttlePreview: Boolean = true,
        reason: String = "source blur geometry",
    ) {
        if (!rectangle.isValid() || rectangle == sourceSubtitleBlurRectangle) return
        sourceSubtitleBlurRectangle = rectangle
        renderSourceBlurGeometryControls()
        onUserChangedOverlay(
            throttleSourceBlurPreview = throttlePreview,
            reason = reason,
        )
    }

    private fun canEditSourceBlur(): Boolean =
        overlayEnabled &&
            sourceSubtitleBlurEnabled &&
            selectedReviewEditorTab == ReviewEditorTab.OVERLAY &&
            !renderCoordinator.currentState.isActiveRender()

    private fun ensureSourceBlurRange(reset: Boolean = false) {
        val info = activeMediaInfo ?: return
        val trim = currentTrimRange(info)
        val currentRangeIsValid = sourceSubtitleBlurRangeInitialized &&
            sourceSubtitleBlurStartMs >= 0L &&
            sourceSubtitleBlurEndMs <= info.durationMs &&
            sourceSubtitleBlurEndMs - sourceSubtitleBlurStartMs >=
            OverlayCompiler.MIN_BLUR_DURATION_MS
        if (reset || !currentRangeIsValid || sourceSubtitleBlurRangeFollowsTrim) {
            sourceSubtitleBlurStartMs = trim.startMs
            sourceSubtitleBlurEndMs = trim.endMs
            sourceSubtitleBlurRangeInitialized = true
            if (reset || !currentRangeIsValid) sourceSubtitleBlurRangeFollowsTrim = true
        }
        editor.sourceBlurTimeRangeSlider.valueFrom = 0f
        editor.sourceBlurTimeRangeSlider.valueTo = (info.durationMs / 1_000f).coerceAtLeast(1f)
        editor.sourceBlurTimeRangeSlider.values = listOf(
            sourceSubtitleBlurStartMs / 1_000f,
            sourceSubtitleBlurEndMs / 1_000f,
        )
    }

    private fun ensureImageOverlayRange(reset: Boolean = false) {
        val info = activeMediaInfo ?: return
        val trim = currentTrimRange(info)
        val currentRangeIsValid = imageOverlayRangeInitialized &&
            imageOverlayStartMs >= 0L &&
            imageOverlayEndMs <= info.durationMs &&
            imageOverlayEndMs - imageOverlayStartMs >= OverlayCompiler.MIN_IMAGE_DURATION_MS
        if (reset || !currentRangeIsValid || imageOverlayRangeFollowsTrim) {
            imageOverlayStartMs = trim.startMs
            imageOverlayEndMs = trim.endMs
            imageOverlayRangeInitialized = true
            if (reset || !currentRangeIsValid) imageOverlayRangeFollowsTrim = true
        }
        editor.imageOverlayTimeRangeSlider.valueFrom = 0f
        editor.imageOverlayTimeRangeSlider.valueTo =
            (info.durationMs / 1_000f).coerceAtLeast(1f)
        editor.imageOverlayTimeRangeSlider.values = listOf(
            imageOverlayStartMs / 1_000f,
            imageOverlayEndMs / 1_000f,
        )
    }

    private fun renderOverlayControls() {
        val renderActive = if (::renderCoordinator.isInitialized) {
            renderCoordinator.currentState.isActiveRender()
        } else {
            false
        }
        editor.overlayEnabledSwitch.isEnabled = !renderActive
        editor.overlayControlsGroup.isVisible = overlayDetailsVisible
        editor.overlayControlsGroup.alpha = if (overlayEnabled) 1f else 0.46f
        editor.overlayVisibilityButton.setText(
            if (overlayDetailsVisible) {
                R.string.overlay_hide_controls
            } else {
                R.string.overlay_show_controls
            },
        )
        editor.sourceBlurEnabledSwitch.isEnabled = overlayEnabled && !renderActive
        val blurControlsVisible = overlayEnabled && sourceSubtitleBlurEnabled
        editor.sourceBlurControlsGroup.isVisible = blurControlsVisible
        editor.sourceBlurControlsGroup.alpha = if (blurControlsVisible) 1f else 0.46f
        editor.sourceBlurTimeRangeSlider.isEnabled = blurControlsVisible && !renderActive
        editor.sourceBlurXSlider.isEnabled = blurControlsVisible && !renderActive
        editor.sourceBlurYSlider.isEnabled = blurControlsVisible && !renderActive
        editor.sourceBlurWidthSlider.isEnabled = blurControlsVisible && !renderActive
        editor.sourceBlurHeightSlider.isEnabled = blurControlsVisible && !renderActive
        editor.sourceBlurStrengthSlider.isEnabled = blurControlsVisible && !renderActive
        editor.sourceBlurResetButton.isEnabled = blurControlsVisible && !renderActive
        editor.imageOverlayEnabledSwitch.isEnabled = overlayEnabled && !renderActive
        val imageControlsVisible = overlayEnabled && imageOverlayEnabled
        editor.imageOverlayControlsGroup.isVisible = imageControlsVisible
        editor.imageOverlayControlsGroup.alpha = if (imageControlsVisible) 1f else 0.46f
        val imageControlsEnabled = imageControlsVisible && !renderActive && !imageOverlayImporting
        editor.imageOverlayChooseButton.isEnabled = imageControlsEnabled
        editor.imageOverlayRemoveButton.isEnabled = imageControlsEnabled && imageOverlayAsset != null
        editor.imageOverlayPositionGroup.setChildrenEnabled(imageControlsEnabled)
        editor.imageOverlayXSlider.isEnabled = imageControlsEnabled
        editor.imageOverlayYSlider.isEnabled = imageControlsEnabled
        editor.imageOverlaySizeSlider.isEnabled = imageControlsEnabled
        editor.imageOverlayOpacitySlider.isEnabled = imageControlsEnabled
        editor.imageOverlayTimeRangeSlider.isEnabled = imageControlsEnabled
        editor.imageOverlayResetButton.isEnabled = imageControlsEnabled
        editor.overlayBadge.setText(
            if (overlayEnabled) R.string.overlay_badge_on else R.string.overlay_badge_off,
        )
        editor.overlaySummary.text = when {
            !overlayEnabled -> getString(R.string.overlay_off_summary)
            sourceSubtitleBlurEnabled && imageOverlayEnabled && imageOverlayAsset != null ->
                getString(
                    R.string.overlay_blur_and_image_summary,
                    checkNotNull(imageOverlayAsset).displayName,
                )
            imageOverlayEnabled && imageOverlayAsset != null -> getString(
                R.string.overlay_image_summary,
                checkNotNull(imageOverlayAsset).displayName,
                (imageOverlayWidthFraction * 100f).roundToInt(),
                (imageOverlayOpacity * 100f).roundToInt(),
            )
            !sourceSubtitleBlurEnabled -> getString(R.string.overlay_on_no_items_summary)
            else -> getString(
                R.string.overlay_source_blur_summary,
                sourceSubtitleBlurStrength.roundToInt(),
                MediaFormatters.duration(sourceSubtitleBlurStartMs),
                MediaFormatters.duration(sourceSubtitleBlurEndMs),
            )
        }
        editor.sourceBlurSummary.setText(
            if (sourceSubtitleBlurEnabled) {
                R.string.source_blur_on_summary
            } else {
                R.string.source_blur_off_summary
            },
        )
        editor.sourceBlurTimeValue.text = getString(
            R.string.source_blur_time_value,
            MediaFormatters.duration(sourceSubtitleBlurStartMs),
            MediaFormatters.duration(sourceSubtitleBlurEndMs),
        )
        editor.sourceBlurStrengthValue.text = getString(
            R.string.source_blur_strength_value,
            sourceSubtitleBlurStrength.roundToInt(),
        )
        editor.sourceBlurStrengthSlider.value = sourceSubtitleBlurStrength
        ensureSourceBlurRange()
        renderSourceBlurGeometryControls()
        ensureImageOverlayRange()
        renderImageOverlayControls()
    }

    private fun renderImageOverlayControls() {
        val asset = imageOverlayAsset
        editor.imageOverlaySummary.text = when {
            !imageOverlayEnabled -> getString(R.string.image_overlay_off_summary)
            asset == null -> getString(R.string.image_overlay_missing_summary)
            else -> getString(R.string.image_overlay_on_summary, asset.displayName)
        }
        editor.imageOverlayChooseButton.setText(
            if (asset == null) R.string.image_overlay_choose else R.string.image_overlay_replace,
        )
        editor.imageOverlayRemoveButton.isVisible = asset != null
        editor.imageOverlayImportProgress.isVisible = imageOverlayImporting
        editor.imageOverlayAssetSummary.text = when {
            imageOverlayImporting -> getString(
                R.string.image_overlay_importing,
                imageOverlayImportName ?: asset?.displayName ?: "",
            )
            asset == null -> getString(R.string.image_overlay_asset_required)
            else -> getString(
                R.string.image_overlay_asset_summary,
                asset.displayName,
                asset.pixelWidth,
                asset.pixelHeight,
                MediaFormatters.fileSize(this, asset.fileSizeBytes),
            )
        }
        editor.imageOverlayXValue.text = getString(
            R.string.image_overlay_x_value,
            (imageOverlayCenterX * 100f).roundToInt(),
        )
        editor.imageOverlayYValue.text = getString(
            R.string.image_overlay_y_value,
            (imageOverlayCenterY * 100f).roundToInt(),
        )
        editor.imageOverlaySizeValue.text = getString(
            R.string.image_overlay_size_value,
            (imageOverlayWidthFraction * 100f).roundToInt(),
        )
        editor.imageOverlayOpacityValue.text = getString(
            R.string.image_overlay_opacity_value,
            (imageOverlayOpacity * 100f).roundToInt(),
        )
        editor.imageOverlayTimeValue.text = getString(
            R.string.image_overlay_time_value,
            MediaFormatters.duration(imageOverlayStartMs),
            MediaFormatters.duration(imageOverlayEndMs),
        )
        editor.imageOverlayXSlider.value = imageOverlayCenterX * 100f
        editor.imageOverlayYSlider.value = imageOverlayCenterY * 100f
        editor.imageOverlaySizeSlider.value = imageOverlayWidthFraction * 100f
        editor.imageOverlayOpacitySlider.value = imageOverlayOpacity * 100f
        val preset = ImageOverlayPositionPreset.entries.firstOrNull {
            abs(it.centerX - imageOverlayCenterX) < 0.001f &&
                abs(it.centerY - imageOverlayCenterY) < 0.001f
        }
        if (preset == null) {
            editor.imageOverlayPositionGroup.clearChecked()
        } else {
            editor.imageOverlayPositionGroup.check(imageOverlayPositionButtonId(preset))
        }
    }

    private fun renderSourceBlurGeometryControls() {
        editor.sourceBlurXValue.text = getString(
            R.string.source_blur_x_value,
            (sourceSubtitleBlurRectangle.left * 100f).roundToInt(),
        )
        editor.sourceBlurYValue.text = getString(
            R.string.source_blur_y_value,
            (sourceSubtitleBlurRectangle.top * 100f).roundToInt(),
        )
        editor.sourceBlurWidthValue.text = getString(
            R.string.source_blur_width_value,
            (sourceSubtitleBlurRectangle.width * 100f).roundToInt(),
        )
        editor.sourceBlurHeightValue.text = getString(
            R.string.source_blur_height_value,
            (sourceSubtitleBlurRectangle.height * 100f).roundToInt(),
        )
        editor.sourceBlurXSlider.value = sourceSubtitleBlurRectangle.left * 100f
        editor.sourceBlurYSlider.value = sourceSubtitleBlurRectangle.top * 100f
        editor.sourceBlurWidthSlider.value = sourceSubtitleBlurRectangle.width * 100f
        editor.sourceBlurHeightSlider.value = sourceSubtitleBlurRectangle.height * 100f
        renderSourceBlurGuide()
    }

    private fun renderSourceBlurGuide() {
        val info = activeMediaInfo
        val sourcePreviewVisible = info != null &&
            previewPath == info.workingFilePath &&
            !previewFallbackActive &&
            editor.previewCard.isVisible
        editor.sourceBlurRegionGuide.isVisible = sourcePreviewVisible && canEditSourceBlur()
        if (editor.sourceBlurRegionGuide.isVisible) applySourceBlurGuideLayout()
    }

    private fun applySourceBlurGuideLayout() {
        val previewWidth = editor.previewCard.width
        val previewHeight = editor.previewCard.height
        if (previewWidth <= 0 || previewHeight <= 0) {
            editor.previewCard.post { applySourceBlurGuideLayout() }
            return
        }
        val rectangle = sourceSubtitleBlurRectangle
        val params = editor.sourceBlurRegionGuide.layoutParams as? FrameLayout.LayoutParams
        if (params == null) {
            Log.e(
                TAG_SOURCE_BLUR,
                "Blur guide has incompatible layout params: " +
                    (editor.sourceBlurRegionGuide.layoutParams?.javaClass?.name ?: "null"),
            )
            return
        }
        val guideWidth = (previewWidth * rectangle.width)
            .roundToInt()
            .coerceIn(1, previewWidth)
        val guideHeight = (previewHeight * rectangle.height)
            .roundToInt()
            .coerceIn(1, previewHeight)
        params.width = guideWidth
        params.height = guideHeight
        params.leftMargin = (previewWidth * rectangle.left)
            .roundToInt()
            .coerceIn(0, previewWidth - guideWidth)
        params.topMargin = (previewHeight * rectangle.top)
            .roundToInt()
            .coerceIn(0, previewHeight - guideHeight)
        params.gravity = Gravity.TOP or Gravity.START
        editor.sourceBlurRegionGuide.layoutParams = params
    }

    private fun onUserChangedOverlay(
        throttleSourceBlurPreview: Boolean = false,
        reason: String = SOURCE_BLUR_PREVIEW_REASON_DEFAULT,
    ) {
        scheduleEditorPreferencesSave()
        val info = activeMediaInfo ?: return
        // Update retained preview shaders immediately. The debounced graph path below is needed
        // only when an effect is added/removed, an image asset changes, or Transform topology moves.
        updateRealtimeOverlayStates()
        cancelFreezePreview()
        cancelAdaptivePreview()
        if (!renderCoordinator.currentState.isActiveRender() &&
            renderCoordinator.currentState !is RenderUiState.Idle
        ) {
            renderCoordinator.reset(mediaHasAudio = renderNeedsAudioCapability(info))
            restoreSourcePreviewAfterStoppedRender()
        }
        if (throttleSourceBlurPreview) {
            scheduleSourceBlurPreviewUpdate(reason)
        } else {
            requestSourceBlurPreviewUpdate(reason, immediate = true)
        }
        updateTrimSummary()
    }

    private fun scheduleSourceBlurPreviewUpdate(
        reason: String,
        force: Boolean = compositionPreviewActive,
    ) {
        val info = activeMediaInfo ?: return
        val graphChanged = realtimePreviewSession.request(
            currentPreviewGraphKey(info),
            reason,
            force = force,
        )
        sourceBlurPreviewDirty = graphChanged
        sourceBlurPreviewReason = reason
        if (!graphChanged) {
            redrawPausedPreviewFrame()
            return
        }
        if (previewFallbackActive || sourceBlurPreviewUpdatePosted) return
        sourceBlurPreviewUpdatePosted = true
        sourceBlurPreviewHandler.postDelayed(
            sourceBlurPreviewUpdate,
            SOURCE_BLUR_PREVIEW_UPDATE_MS,
        )
    }

    private fun requestSourceBlurPreviewUpdate(
        reason: String,
        immediate: Boolean,
        force: Boolean = compositionPreviewActive,
    ) {
        val info = activeMediaInfo ?: return
        val graphChanged = realtimePreviewSession.request(
            currentPreviewGraphKey(info),
            reason,
            force = force,
        )
        sourceBlurPreviewDirty = graphChanged
        sourceBlurPreviewReason = reason
        if (!graphChanged) {
            redrawPausedPreviewFrame()
            return
        }
        if (immediate) {
            commitSourceBlurPreviewUpdate(reason)
        } else if (!previewFallbackActive && !sourceBlurPreviewUpdatePosted) {
            sourceBlurPreviewUpdatePosted = true
            sourceBlurPreviewHandler.postDelayed(
                sourceBlurPreviewUpdate,
                SOURCE_BLUR_PREVIEW_UPDATE_MS,
            )
        }
    }

    private fun commitSourceBlurPreviewUpdate(reason: String) {
        sourceBlurPreviewHandler.removeCallbacks(sourceBlurPreviewUpdate)
        sourceBlurPreviewUpdatePosted = false
        sourceBlurPreviewReason = reason
        val info = activeMediaInfo ?: return
        if (!sourceBlurPreviewDirty || previewFallbackActive) return
        if (renderCoordinator.currentState.isActiveRender()) return
        val pendingUpdate = realtimePreviewSession.takePending()
        if (pendingUpdate == null ||
            !realtimePreviewSession.isCurrent(info.workingFilePath, pendingUpdate.generation)
        ) {
            sourceBlurPreviewDirty = false
            redrawPausedPreviewFrame()
            return
        }
        val applied = applyLiveTransformPreview(
            info = info,
            reason = pendingUpdate.reason,
            requestedKey = pendingUpdate.key,
        )
        sourceBlurPreviewDirty = !applied && !previewFallbackActive
    }

    private fun cancelSourceBlurPreviewUpdate(clearDirty: Boolean) {
        sourceBlurPreviewHandler.removeCallbacks(sourceBlurPreviewUpdate)
        sourceBlurPreviewUpdatePosted = false
        realtimePreviewSession.clearPending()
        cancelSourceBlurGestureCommit(resetGuide = true)
        if (clearDirty) sourceBlurPreviewDirty = false
    }

    private fun openReplacementAudioPicker() {
        if (renderCoordinator.currentState.isActiveRender() || replacementAudioImporting) return
        replacementAudioPicker.launch(arrayOf("audio/*"))
    }

    private fun openImageOverlayPicker() {
        if (renderCoordinator.currentState.isActiveRender() || imageOverlayImporting) return
        imageOverlayPicker.launch(arrayOf("image/png", "image/jpeg", "image/webp"))
    }

    private fun renderImageOverlayImportState(state: ImageOverlayImportState) {
        when (state) {
            ImageOverlayImportState.Idle -> {
                imageOverlayImporting = false
                imageOverlayImportName = null
                imageOverlayAsset = null
            }
            is ImageOverlayImportState.Importing -> {
                imageOverlayImporting = true
                imageOverlayImportName = state.displayName
            }
            is ImageOverlayImportState.Ready -> {
                imageOverlayImporting = false
                imageOverlayImportName = null
                imageOverlayAsset = state.asset
                overlayEnabled = true
                imageOverlayEnabled = true
                editor.overlayEnabledSwitch.isChecked = true
                editor.imageOverlayEnabledSwitch.isChecked = true
                ensureImageOverlayRange()
                onUserChangedOverlay(reason = "image overlay imported")
            }
            is ImageOverlayImportState.Error -> {
                imageOverlayImporting = false
                imageOverlayImportName = null
                Snackbar.make(binding.mainRoot, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
        renderOverlayControls()
        updateTrimSummary()
    }

    private fun renderReplacementAudioImportState(state: ReplacementAudioImportState) {
        when (state) {
            ReplacementAudioImportState.Idle -> {
                replacementAudioImporting = false
                replacementAudioImportName = null
                replacementAudioAsset = null
            }
            is ReplacementAudioImportState.Importing -> {
                replacementAudioImporting = true
                replacementAudioImportName = state.displayName
                editor.replacementAudioSummary.text = getString(
                    R.string.audio_replace_importing,
                    state.displayName,
                )
            }
            is ReplacementAudioImportState.Ready -> {
                replacementAudioImporting = false
                replacementAudioImportName = null
                replacementAudioAsset = state.asset
                replacementPreviewErrorShown = false
                audioEnabled = true
                editor.audioEnabledSwitch.isChecked = true
                if (audioPolicy != AudioPolicy.MIX) {
                    audioPolicy = AudioPolicy.REPLACE
                }
                editor.audioPolicyGroup.check(audioPolicyButtonId(audioPolicy))
                onUserChangedAudio()
            }
            is ReplacementAudioImportState.Error -> {
                replacementAudioImporting = false
                replacementAudioImportName = null
                Snackbar.make(binding.mainRoot, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
        renderAudioControls()
        updateTrimSummary()
    }

    private fun bindTransformControls() {
        editor.transformEnabledSwitch.isChecked = transformEnabled
        editor.aspectRatioGroup.check(aspectRatioButtonId(transformAspectRatio))
        editor.scaleModeGroup.check(scaleModeButtonId(transformScaleMode))
        editor.cropEnabledSwitch.isChecked = cropEnabled
        editor.mirrorEnabledSwitch.isChecked = mirrorEnabled
        editor.colorEnabledSwitch.isChecked = colorEnabled
        editor.colorBrightnessSlider.value = colorBrightness
        editor.colorContrastSlider.value = colorContrast
        editor.colorSaturationSlider.value = colorSaturation
        editor.colorTemperatureSlider.value = colorTemperature
        editor.zoomEnabledSwitch.isChecked = zoomEnabled
        editor.zoomModeGroup.check(zoomModeButtonId(zoomMode))
        editor.speedEnabledSwitch.isChecked = speedEnabled
        editor.speedModeGroup.check(speedButtonId(speedMultiplier))
        editor.freezeEnabledSwitch.isChecked = freezeEnabled
        editor.freezeDurationGroup.check(freezeDurationButtonId(freezeDurationMs))
        editor.transitionEnabledSwitch.isChecked = transitionEnabled
        editor.transitionModeGroup.check(transitionModeButtonId(transitionMode))
        editor.transitionDurationGroup.check(transitionDurationButtonId(transitionDurationMs))
        editor.cropLeftSlider.value = cropRectangle.left * 100f
        editor.cropTopSlider.value = cropRectangle.top * 100f
        editor.cropRightSlider.value = (1f - cropRectangle.right) * 100f
        editor.cropBottomSlider.value = (1f - cropRectangle.bottom) * 100f
        renderTransformControls()

        editor.transformVisibilityButton.setOnClickListener {
            transformDetailsVisible = !transformDetailsVisible
            renderTransformControls()
            scheduleEditorPreferencesSave()
        }

        editor.transformEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (transformEnabled != isChecked) {
                transformEnabled = isChecked
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.aspectRatioGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val selected = when (checkedId) {
                R.id.aspectPortraitButton -> AspectRatioPreset.PORTRAIT_9_16
                R.id.aspectLandscapeButton -> AspectRatioPreset.LANDSCAPE_16_9
                R.id.aspectSquareButton -> AspectRatioPreset.SQUARE_1_1
                else -> AspectRatioPreset.ORIGINAL
            }
            if (transformAspectRatio != selected) {
                transformAspectRatio = selected
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.scaleModeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val selected = if (checkedId == R.id.scaleFillButton) {
                ScaleMode.FILL
            } else {
                ScaleMode.FIT
            }
            if (transformScaleMode != selected) {
                transformScaleMode = selected
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.cropEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (cropEnabled != isChecked) {
                cropEnabled = isChecked
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.mirrorEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (mirrorEnabled != isChecked) {
                mirrorEnabled = isChecked
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.colorEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (colorEnabled != isChecked) {
                colorEnabled = isChecked
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.zoomEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (zoomEnabled != isChecked) {
                zoomEnabled = isChecked
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.zoomModeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val selected = when (checkedId) {
                R.id.zoomOutButton -> ZoomMode.OUT
                R.id.zoomAlternateButton -> ZoomMode.ALTERNATE
                else -> ZoomMode.IN
            }
            if (zoomMode != selected) {
                zoomMode = selected
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.speedEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (speedEnabled != isChecked) {
                speedEnabled = isChecked
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.speedModeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val selected = when (checkedId) {
                R.id.speed05Button -> 0.5f
                R.id.speed075Button -> 0.75f
                R.id.speed10Button -> 1f
                R.id.speed15Button -> 1.5f
                R.id.speed20Button -> 2f
                else -> 1.25f
            }
            if (speedMultiplier != selected) {
                speedMultiplier = selected
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.freezeEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (freezeEnabled != isChecked) {
                freezeEnabled = isChecked
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.freezeDurationGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val selected = when (checkedId) {
                R.id.freeze1sButton -> 1_000L
                R.id.freeze3sButton -> 3_000L
                else -> 2_000L
            }
            if (freezeDurationMs != selected) {
                freezeDurationMs = selected
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.freezePreviewButton.setOnClickListener { previewIntroFreeze() }
        editor.transitionEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (transitionEnabled != isChecked) {
                transitionEnabled = isChecked
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.transitionModeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val selected = when (checkedId) {
                R.id.transitionFadeInButton -> TransitionMode.FADE_IN
                R.id.transitionFadeOutButton -> TransitionMode.FADE_OUT
                else -> TransitionMode.FADE_IN_OUT
            }
            if (transitionMode != selected) {
                transitionMode = selected
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.transitionDurationGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val selected = when (checkedId) {
                R.id.transition05sButton -> 500L
                R.id.transition15sButton -> 1_500L
                else -> 1_000L
            }
            if (transitionDurationMs != selected) {
                transitionDurationMs = selected
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        listOf(
            editor.colorBrightnessSlider,
            editor.colorContrastSlider,
            editor.colorSaturationSlider,
            editor.colorTemperatureSlider,
        ).forEach { slider ->
            slider.addOnChangeListener { _, _, fromUser ->
                if (!fromUser) return@addOnChangeListener
                colorBrightness = editor.colorBrightnessSlider.value
                colorContrast = editor.colorContrastSlider.value
                colorSaturation = editor.colorSaturationSlider.value
                colorTemperature = editor.colorTemperatureSlider.value
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.colorResetButton.setOnClickListener { resetColorAdjustments() }
        listOf(
            editor.cropLeftSlider,
            editor.cropTopSlider,
            editor.cropRightSlider,
            editor.cropBottomSlider,
        ).forEach { slider ->
            slider.addOnChangeListener { _, _, fromUser ->
                if (!fromUser) return@addOnChangeListener
                cropRectangle = CropRectangle(
                    left = editor.cropLeftSlider.value / 100f,
                    top = editor.cropTopSlider.value / 100f,
                    right = 1f - (editor.cropRightSlider.value / 100f),
                    bottom = 1f - (editor.cropBottomSlider.value / 100f),
                )
                renderTransformControls()
                onUserChangedTransform()
            }
        }
    }

    private fun bindNavigation(savedInstanceState: Bundle?) {
        binding.mainNavigation.setOnItemSelectedListener { item ->
            val destination = MainDestination.entries.firstOrNull {
                it.menuItemId == item.itemId
            } ?: return@setOnItemSelectedListener false
            showDestination(destination)
            true
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (selectedDestination != MainDestination.HOME) {
                    navigateTo(MainDestination.HOME)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        val restored = savedInstanceState?.getString(KEY_MAIN_DESTINATION)
            ?.let { name ->
                MainDestination.entries.firstOrNull { it.name == name }
            }
            ?: MainDestination.HOME
        binding.mainNavigation.selectedItemId = restored.menuItemId
        showDestination(restored)
    }

    private fun resetColorAdjustments() {
        val alreadyNeutral = colorBrightness == 0f &&
            colorContrast == 0f &&
            colorSaturation == 0f &&
            colorTemperature == 0f
        colorBrightness = 0f
        colorContrast = 0f
        colorSaturation = 0f
        colorTemperature = 0f
        editor.colorBrightnessSlider.value = 0f
        editor.colorContrastSlider.value = 0f
        editor.colorSaturationSlider.value = 0f
        editor.colorTemperatureSlider.value = 0f
        renderTransformControls()
        if (!alreadyNeutral) onUserChangedTransform()
    }

    private fun navigateTo(destination: MainDestination) {
        if (binding.mainNavigation.selectedItemId == destination.menuItemId) {
            showDestination(destination)
        } else {
            binding.mainNavigation.selectedItemId = destination.menuItemId
        }
    }

    private fun showDestination(destination: MainDestination) {
        selectedDestination = destination
        binding.homeDestination.isVisible = destination == MainDestination.HOME
        binding.editorDestination.isVisible = destination == MainDestination.EDITOR
        binding.settingsDestination.isVisible = destination == MainDestination.SETTINGS
        if (destination != MainDestination.EDITOR) {
            cancelFreezePreview()
            cancelAdaptivePreview()
            activePreviewPause()
        }

        binding.topAppBar.setTitle(destination.titleRes)
        binding.topAppBar.subtitle = when (destination) {
            MainDestination.HOME -> getString(R.string.home_toolbar_subtitle)
            MainDestination.EDITOR -> activeMediaInfo?.displayName
                ?: getString(R.string.editor_toolbar_subtitle)
            MainDestination.SETTINGS -> getString(R.string.settings_toolbar_subtitle)
        }
    }

    private fun requestVideoAccessThenPick() {
        if (renderCoordinator.currentState.isActiveRender()) {
            Snackbar.make(binding.mainRoot, R.string.finish_or_cancel_render, Snackbar.LENGTH_SHORT)
                .show()
            return
        }
        if (!needsLegacyMediaPermission()) {
            openVideoPicker()
            return
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.legacy_media_permission_title)
                .setMessage(R.string.legacy_media_permission_message)
                .setPositiveButton(R.string.allow_access) { _, _ ->
                    legacyMediaPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                .setNegativeButton(R.string.use_system_picker) { _, _ ->
                    openVideoPicker()
                }
                .show()
        } else {
            legacyMediaPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun needsLegacyMediaPermission(): Boolean =
        // minSdk 28 leaves Android 9 as the only legacy shared-media permission path.
        Build.VERSION.SDK_INT == Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED

    private fun openVideoPicker() {
        navigateTo(MainDestination.EDITOR)
        activePreviewPause()
        importCoordinator.beginPicking()
        videoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
        )
    }

    private fun renderState(state: ImportUiState) {
        renderEngineStatus(state)
        val ready = state is ImportUiState.Ready
        editor.emptyState.isVisible = state is ImportUiState.Empty
        editor.progressState.isVisible = state is ImportUiState.EngineChecking ||
            state is ImportUiState.Picking ||
            state is ImportUiState.Preparing ||
            state is ImportUiState.Probing
        editor.errorState.isVisible = state is ImportUiState.Error
        editor.readyState.isVisible = ready
        editor.previewCard.isVisible = ready
        editor.previewSpacer.isVisible = ready
        editor.previewBackdrop.isVisible = ready
        editor.editorSheetScroll.scrollTo(0, 0)
        updatePreviewUnderlayEffect(0)

        when (state) {
            ImportUiState.EngineChecking -> renderChecking()
            is ImportUiState.Empty -> Unit
            is ImportUiState.Picking -> renderPicking()
            is ImportUiState.Preparing -> renderPreparing(state)
            is ImportUiState.Probing -> renderProbing(state)
            is ImportUiState.Ready -> renderReady(state.mediaInfo)
            is ImportUiState.Error -> renderError(state)
        }
    }

    private fun renderEngineStatus(state: ImportUiState) {
        val version = when (state) {
            is ImportUiState.Empty -> state.engineVersion
            is ImportUiState.Picking -> state.engineVersion
            is ImportUiState.Preparing -> state.engineVersion
            is ImportUiState.Probing -> state.engineVersion
            is ImportUiState.Ready -> state.engineVersion
            is ImportUiState.Error -> state.engineVersion
            ImportUiState.EngineChecking -> null
        }

        val (titleRes, detail) = when {
            state is ImportUiState.EngineChecking -> {
                R.string.engine_checking to getString(R.string.native_status_checking)
            }
            version.isNullOrBlank() -> {
                R.string.engine_unavailable to if (state is ImportUiState.Error) {
                    state.message
                } else {
                    getString(R.string.unknown_value)
                }
            }
            else -> {
                R.string.engine_ready to version
            }
        }
        listOf(
            editor.engineStatusTitle,
            binding.homeContent.homeEngineStatusTitle,
            binding.settingsContent.settingsEngineStatusTitle,
        ).forEach { it.setText(titleRes) }
        listOf(
            editor.engineStatusDetail,
            binding.homeContent.homeEngineStatusDetail,
            binding.settingsContent.settingsEngineStatusDetail,
        ).forEach { it.text = detail }
    }

    private fun renderChecking() {
        editor.progressTitle.setText(R.string.engine_checking)
        editor.progressFileName.setText(R.string.native_status_checking)
        showIndeterminateProgress()
        editor.progressDetail.setText(R.string.local_processing_note)
    }

    private fun renderPicking() {
        editor.progressTitle.setText(R.string.import_video)
        editor.progressFileName.setText(R.string.preparing_unknown_file)
        showIndeterminateProgress()
        editor.progressDetail.setText(R.string.local_processing_note)
    }

    private fun renderPreparing(state: ImportUiState.Preparing) {
        editor.progressTitle.setText(R.string.preparing_title)
        editor.progressFileName.text = state.displayName
        val total = state.totalBytes
        if (total != null && total > 0L) {
            showDeterminateProgress(
                ((state.copiedBytes * 100L) / total).coerceIn(0L, 100L).toInt(),
            )
        } else {
            showIndeterminateProgress()
        }
        editor.progressDetail.text = MediaFormatters.copyProgress(
            this,
            state.copiedBytes,
            total,
        )
    }

    private fun renderProbing(state: ImportUiState.Probing) {
        editor.progressTitle.setText(R.string.probing_title)
        editor.progressFileName.text = state.preparedMedia.displayName
        showIndeterminateProgress()
        editor.progressDetail.setText(R.string.probe_progress_detail)
    }

    private fun showIndeterminateProgress() {
        if (editor.progressIndicator.isIndeterminate) {
            return
        }
        editor.progressIndicator.visibility = View.INVISIBLE
        editor.progressIndicator.isIndeterminate = true
        editor.progressIndicator.visibility = View.VISIBLE
    }

    private fun showDeterminateProgress(progress: Int) {
        if (editor.progressIndicator.isIndeterminate) {
            editor.progressIndicator.visibility = View.INVISIBLE
            editor.progressIndicator.isIndeterminate = false
            editor.progressIndicator.progress = progress
            editor.progressIndicator.visibility = View.VISIBLE
        } else {
            editor.progressIndicator.setProgressCompat(progress, true)
        }
    }

    private fun renderReady(info: MediaInfo) {
        val replacingSource = activeMediaInfo != null &&
            activeMediaInfo?.workingFilePath != info.workingFilePath
        if (activeMediaInfo?.workingFilePath != info.workingFilePath) {
            cancelSourceBlurPreviewUpdate(clearDirty = true)
            if (activeMediaInfo != null) {
                clearAdaptiveDraft()
                val previousReplacement = replacementAudioAsset
                replacementAudioAsset = null
                stopReplacementAudioPreview(clearMedia = true)
                replacementAudioImportCoordinator.clear(previousReplacement)
                val previousImage = imageOverlayAsset
                imageOverlayAsset = null
                imageOverlayEnabled = false
                realtimeSourceBlurState.update(null)
                realtimeImageOverlayState.update(null)
                editor.imageOverlayEnabledSwitch.isChecked = false
                imageOverlayImportCoordinator.clear(previousImage)
            }
            activeMediaInfo = info
            if (::clipTransitionEditorController.isInitialized && replacingSource) {
                clipTransitionEditorController.reset()
            }
            publicExportCoordinator.reset()
            configureTrim(info)
            if (replacingSource) {
                sourceSubtitleBlurRectangle = BlurRectangle()
                sourceSubtitleBlurStrength = OverlayCompiler.DEFAULT_BLUR_STRENGTH
                sourceSubtitleBlurRangeInitialized = false
                sourceSubtitleBlurRangeFollowsTrim = true
                imageOverlayCenterX = OverlayCompiler.DEFAULT_IMAGE_CENTER_X
                imageOverlayCenterY = OverlayCompiler.DEFAULT_IMAGE_CENTER_Y
                imageOverlayWidthFraction = OverlayCompiler.DEFAULT_IMAGE_WIDTH_FRACTION
                imageOverlayOpacity = OverlayCompiler.DEFAULT_IMAGE_OPACITY
                imageOverlayRangeInitialized = false
                imageOverlayRangeFollowsTrim = true
            }
            ensureSourceBlurRange(reset = replacingSource)
            ensureImageOverlayRange(reset = replacingSource)
            if (
                adaptiveDraftRanges.isNotEmpty() &&
                !AdaptiveCutCompiler.areRangesValid(adaptiveDraftRanges, currentTrimRange(info))
            ) {
                clearAdaptiveDraft()
            }
            renderCoordinator.reset(mediaHasAudio = renderNeedsAudioCapability(info))
        }
        editor.fileName.text = info.displayName
        val duration = MediaFormatters.duration(info.durationMs)
        val resolution = MediaFormatters.resolution(info)
        val videoCodec = MediaFormatters.codec(this, info.videoCodec)
        editor.fileSummary.text = "$duration • $resolution • $videoCodec"
        binding.homeContent.homeActiveProjectCard.isVisible = true
        binding.homeContent.homeImportCard.isVisible = false
        binding.homeContent.homeActiveFileName.text = info.displayName
        binding.homeContent.homeActiveFileSummary.text = "$duration • $resolution • $videoCodec"
        if (selectedDestination == MainDestination.EDITOR) {
            binding.topAppBar.subtitle = info.displayName
        }
        editor.durationValue.text = duration
        editor.resolutionValue.text = resolution
        editor.orientationValue.text = MediaFormatters.orientation(this, info)
        editor.frameRateValue.text = MediaFormatters.frameRate(this, info.frameRate)
        editor.containerValue.text = MediaFormatters.container(this, info.containerFormat)
        editor.videoCodecValue.text = videoCodec
        editor.audioValue.text = MediaFormatters.audio(this, info)
        editor.bitrateValue.text = MediaFormatters.bitrate(this, info.bitrate)
        editor.fileSizeValue.text = MediaFormatters.fileSize(this, info.fileSizeBytes)
        configureSourcePreviewLayout(info)
        renderOverlayControls()
        if (::clipTransitionEditorController.isInitialized) {
            clipTransitionEditorController.reconcile()
        }

        if (previewPath != info.workingFilePath) {
            technicalDetailsExpanded = false
            renderTechnicalDetailsVisibility()
            if (replacingSource || !previewCapabilityRestored) {
                setPreviewUiState(PreviewUiState.LiveEffects)
            } else {
                renderPreviewUiState()
            }
            previewCapabilityRestored = false
            preparePreview(info.workingFilePath, autoPlay = false)
        }
    }

    private fun preparePreview(
        workingFilePath: String,
        autoPlay: Boolean,
        startPositionMs: Long = if (autoPlay) 0L else PREVIEW_FRAME_MS,
    ) {
        val previousPath = previewPath
        previewPath = workingFilePath
        if (previousPath != workingFilePath) {
            compositionPreviewBlockedPath = null
            releaseCompositionPreview(attachExoPlayer = true, reason = "source changed")
        }
        val sessionGeneration = realtimePreviewSession.begin(
            path = workingFilePath,
            restart = previousPath != workingFilePath,
        )
        editor.previewHint.isVisible = false

        val sourceInfo = activeMediaInfo
        val sourceSelected = sourceInfo?.workingFilePath == workingFilePath
        if (sourceSelected && sourceInfo != null && compositionPreviewEligible(sourceInfo)) {
            configureSourcePreviewLayout(sourceInfo)
            val prepared = runCatching {
                prepareCompositionPreview(
                    info = sourceInfo,
                    autoPlay = autoPlay,
                    sourcePositionMs = startPositionMs,
                    reason = "prepare source preview",
                )
            }.onFailure { error ->
                compositionPreviewBlockedPath = sourceInfo.workingFilePath
                Log.w(TAG_PREVIEW, "CompositionPlayer setup rejected; using ExoPlayer", error)
                releaseCompositionPreview(attachExoPlayer = true, reason = "setup failure")
            }.getOrDefault(false)
            if (prepared) {
                renderSourceBlurGuide()
                return
            }
        }

        prepareExoPreview(
            workingFilePath = workingFilePath,
            autoPlay = autoPlay,
            startPositionMs = startPositionMs,
            sessionGeneration = sessionGeneration,
        )
    }

    private fun prepareExoPreview(
        workingFilePath: String,
        autoPlay: Boolean,
        startPositionMs: Long,
        sessionGeneration: Long = realtimePreviewSession.currentGeneration(),
    ) {
        releaseCompositionPreview(attachExoPlayer = true, reason = "ExoPlayer preview selected")
        previewPath = workingFilePath
        editor.previewHint.isVisible = false
        val sourceInfo = activeMediaInfo
        val isSourcePreview = sourceInfo
            ?.let { it.workingFilePath == workingFilePath && !previewFallbackActive }
            ?: false
        if (sourceInfo != null && sourceInfo.workingFilePath == workingFilePath) {
            configureSourcePreviewLayout(sourceInfo)
        } else {
            editor.videoPreview.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        val graphKey = sourceInfo
            ?.takeIf { isSourcePreview }
            ?.let(::currentPreviewGraphKey)
        val effects = graphKey
            ?.let { key ->
                val previewInfo = checkNotNull(sourceInfo)
                updateRealtimeOverlayStates()
                TransformVideoEffects.forPreview(
                    settings = key.transform,
                    sourceWidth = previewInfo.width,
                    sourceHeight = previewInfo.height,
                    overlays = currentOverlaySettings(),
                    timelineOffsetUs = key.timelineOffsetUs,
                    sourceDurationMs = key.sourceDurationMs,
                    sourceTimeOffsetUs = key.sourceTimeOffsetUs,
                    realtimeSourceBlurState = realtimeSourceBlurState,
                    realtimeImageOverlayState = realtimeImageOverlayState,
                )
            }
            .orEmpty()
        editor.videoPreview.player = previewPlayer
        previewPlayer.setVideoEffects(effects)
        previewEffectSignature = effects.previewEffectSignature()
        if (graphKey != null) {
            realtimePreviewSession.markApplying(graphKey)
        } else {
            realtimePreviewSession.clearAppliedGraph()
        }
        previewPlayer.setPlaybackSpeed(if (isSourcePreview) currentPreviewSpeed() else 1f)
        previewPlayer.volume = if (isSourcePreview) currentPreviewVolume() else 1f
        previewPlayer.setMediaItem(MediaItem.fromUri(File(workingFilePath).toURI().toString()))
        previewPlayer.playWhenReady = autoPlay
        previewPlayer.seekTo(startPositionMs.coerceAtLeast(0L))
        previewPlayer.prepare()
        schedulePreviewReadyTimeout(workingFilePath, sessionGeneration)
        refreshAudioPreview()
        renderSourceBlurGuide()
    }

    private fun configureSourcePreviewLayout(
        info: MediaInfo,
        onSurfaceGeometrySettled: (() -> Unit)? = null,
    ) {
        val settings = currentTransformSettings()
        val previewAspectOwner = PreviewAspectPolicy.resolve(
            settings = settings,
            preset = RenderPreset.HD_720P,
            liveEffectsAvailable = !previewFallbackActive,
        )
        // PREVIEW_SINGLE_ASPECT_OWNER: Presentation/Crop already maps source pixels into
        // the output frame. Let that complete frame fill its matching card so PlayerView
        // does not apply source-aspect correction a second time. Geometry-changing updates
        // are rebound only after the card/TextureView consumes these target bounds.
        editor.videoPreview.resizeMode = when (previewAspectOwner) {
            PreviewAspectOwner.PLAYER_VIEW -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            PreviewAspectOwner.VIDEO_EFFECTS -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        }
        val geometryEffectsAvailable = previewAspectOwner == PreviewAspectOwner.VIDEO_EFFECTS
        val compiledTransform = PreviewGeometryPolicy
            .compile(settings, info.width, info.height)
            .takeIf { geometryEffectsAvailable }
        val compiledCrop = CropCompiler.compile(settings).takeIf { geometryEffectsAvailable }
        when {
            compiledTransform != null -> {
                configurePreviewLayout(
                    compiledTransform.targetWidth,
                    compiledTransform.targetHeight,
                    onSurfaceGeometrySettled,
                )
            }
            compiledCrop != null -> {
                val swapsDimensions = info.rotationDegrees == 90 || info.rotationDegrees == 270
                val displayWidth = if (swapsDimensions) info.height else info.width
                val displayHeight = if (swapsDimensions) info.width else info.height
                configurePreviewLayout(
                    (displayWidth * cropRectangle.width).roundToInt().coerceAtLeast(1),
                    (displayHeight * cropRectangle.height).roundToInt().coerceAtLeast(1),
                    onSurfaceGeometrySettled,
                )
            }
            else -> configurePreviewLayout(info, onSurfaceGeometrySettled)
        }
    }

    private fun configurePreviewLayout(
        info: MediaInfo,
        onSurfaceGeometrySettled: (() -> Unit)? = null,
    ) {
        val swapsDimensions = info.rotationDegrees.mod(180) != 0
        val displayWidth = (if (swapsDimensions) info.height else info.width).coerceAtLeast(1)
        val displayHeight = (if (swapsDimensions) info.width else info.height).coerceAtLeast(1)
        configurePreviewLayout(displayWidth, displayHeight, onSurfaceGeometrySettled)
    }

    private fun configurePreviewLayout(
        displayWidth: Int,
        displayHeight: Int,
        onSurfaceGeometrySettled: (() -> Unit)? = null,
    ) {
        editor.root.post {
            val rootWidth = editor.root.width
            val rootHeight = editor.root.height
            if (rootWidth <= 0 || rootHeight <= 0) return@post

            val horizontalMargin = resources.getDimensionPixelSize(
                R.dimen.rf_editor_preview_horizontal_margin,
            )
            val topMargin = resources.getDimensionPixelSize(
                R.dimen.rf_editor_preview_top_margin,
            )
            val bottomGap = resources.getDimensionPixelSize(
                R.dimen.rf_editor_preview_bottom_gap,
            )
            val maxWidth = (rootWidth - horizontalMargin * 2).coerceAtLeast(1)
            val maxHeight = (rootHeight / 3f).roundToInt().coerceAtLeast(1)

            val aspectRatio = displayWidth.toFloat() / displayHeight.toFloat()

            var previewWidth = maxWidth
            var previewHeight = (previewWidth / aspectRatio).roundToInt()
            if (previewHeight > maxHeight) {
                previewHeight = maxHeight
                previewWidth = (previewHeight * aspectRatio).roundToInt().coerceAtMost(maxWidth)
            }

            previewBaseWidth = previewWidth
            previewBaseHeight = previewHeight
            applyPreviewOverlayLayout(refreshVideoSurface = true)

            val clearance = topMargin + previewHeight + bottomGap
            editor.previewSpacer.layoutParams = editor.previewSpacer.layoutParams.apply {
                height = clearance
            }

            if (onSurfaceGeometrySettled != null) {
                // The first animation frame applies the card bounds; the second lets PlayerView's
                // TextureView consume them before a Presentation graph with a new aspect is bound.
                editor.previewCard.postOnAnimation {
                    editor.previewCard.postOnAnimation {
                        if (_binding != null) onSurfaceGeometrySettled()
                    }
                }
            }
        }
    }

    private fun applyPreviewOverlayLayout(refreshVideoSurface: Boolean = false) {
        val rootWidth = editor.root.width
        val rootHeight = editor.root.height
        if (rootWidth <= 0 || rootHeight <= 0 || previewBaseWidth <= 0 || previewBaseHeight <= 0) {
            return
        }

        val edgeMargin = resources.getDimensionPixelSize(R.dimen.rf_editor_preview_edge_margin)
        val topMargin = resources.getDimensionPixelSize(R.dimen.rf_editor_preview_top_margin)
        val availableWidth = (rootWidth - edgeMargin * 2).coerceAtLeast(1)
        val availableHeight = (rootHeight - edgeMargin * 2).coerceAtLeast(1)
        val maxScale = maxPreviewOverlayScale()
        previewOverlayScale = previewOverlayScale.coerceIn(
            MIN_PREVIEW_OVERLAY_SCALE,
            maxScale,
        )

        val previewWidth = (previewBaseWidth * previewOverlayScale).roundToInt()
            .coerceIn(1, availableWidth)
        val previewHeight = (previewBaseHeight * previewOverlayScale).roundToInt()
            .coerceIn(1, availableHeight)
        val centerX = previewOverlayCenterXFraction * rootWidth
        val centerY = if (previewOverlayCenterYFraction == PREVIEW_POSITION_UNSET) {
            topMargin + previewHeight / 2f
        } else {
            previewOverlayCenterYFraction * rootHeight
        }
        applyPreviewOverlayFrame(
            left = (centerX - previewWidth / 2f).roundToInt(),
            top = (centerY - previewHeight / 2f).roundToInt(),
            width = previewWidth,
            height = previewHeight,
            persistPosition = true,
            refreshVideoSurface = refreshVideoSurface,
        )
    }

    private fun movePreviewOverlay(left: Int, top: Int) {
        val params = editor.previewCard.layoutParams as FrameLayout.LayoutParams
        applyPreviewOverlayFrame(
            left = left,
            top = top,
            width = params.width,
            height = params.height,
            persistPosition = true,
        )
    }

    private fun applyPreviewOverlayFrame(
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        persistPosition: Boolean,
        refreshVideoSurface: Boolean = false,
    ) {
        val rootWidth = editor.root.width
        val rootHeight = editor.root.height
        if (rootWidth <= 0 || rootHeight <= 0 || width <= 0 || height <= 0) return

        val edgeMargin = resources.getDimensionPixelSize(R.dimen.rf_editor_preview_edge_margin)
        val maxLeft = (rootWidth - edgeMargin - width).coerceAtLeast(edgeMargin)
        val maxTop = (rootHeight - edgeMargin - height).coerceAtLeast(edgeMargin)
        val boundedLeft = left.coerceIn(edgeMargin, maxLeft)
        val boundedTop = top.coerceIn(edgeMargin, maxTop)

        editor.previewCard.layoutParams =
            (editor.previewCard.layoutParams as FrameLayout.LayoutParams).apply {
                this.width = width
                this.height = height
                gravity = Gravity.TOP or Gravity.START
                leftMargin = boundedLeft
                topMargin = boundedTop
            }
        editor.previewBackdrop.layoutParams =
            (editor.previewBackdrop.layoutParams as FrameLayout.LayoutParams).apply {
                this.width = width
                this.height = height
                gravity = Gravity.TOP or Gravity.START
                leftMargin = boundedLeft
                topMargin = boundedTop
            }

        if (persistPosition) {
            previewOverlayCenterXFraction = (boundedLeft + width / 2f) / rootWidth.toFloat()
            previewOverlayCenterYFraction = (boundedTop + height / 2f) / rootHeight.toFloat()
        }
        if (refreshVideoSurface) refreshVideoPreviewSurfaceGeometry()
        updatePreviewUnderlayEffect(editor.editorSheetScroll.scrollY)
        if (editor.sourceBlurRegionGuide.isVisible) applySourceBlurGuideLayout()
    }

    private fun refreshVideoPreviewSurfaceGeometry() {
        // PREVIEW_TEXTURE_BOUNDS_RECOVERY: the Editor card is both movable and resizable.
        // Force PlayerView and its embedded TextureView to consume the bounded card dimensions.
        // Geometry-changing Presentation graphs are bound only after this layout settles.
        editor.videoPreview.requestLayout()
        editor.videoPreview.invalidate()
        editor.videoPreview.videoSurfaceView?.requestLayout()
        editor.previewCard.postOnAnimation {
            if (_binding == null) return@postOnAnimation
            editor.videoPreview.requestLayout()
            editor.videoPreview.videoSurfaceView?.apply {
                requestLayout()
                invalidate()
            }
            editor.videoPreview.invalidate()
        }
    }

    private fun maxPreviewOverlayScale(): Float {
        val edgeMargin = resources.getDimensionPixelSize(R.dimen.rf_editor_preview_edge_margin)
        val availableWidth = (editor.root.width - edgeMargin * 2).coerceAtLeast(1)
        val availableHeight = (editor.root.height - edgeMargin * 2).coerceAtLeast(1)
        if (previewBaseWidth <= 0 || previewBaseHeight <= 0) {
            return DEFAULT_PREVIEW_OVERLAY_SCALE
        }
        return min(
            availableWidth.toFloat() / previewBaseWidth.toFloat(),
            availableHeight.toFloat() / previewBaseHeight.toFloat(),
        ).coerceAtLeast(MIN_PREVIEW_OVERLAY_SCALE)
    }

    private fun configureRenderedPreviewLayout(preset: RenderPreset) {
        val info = activeMediaInfo ?: return
        val settings = currentEditPlan(preset).transform
        val compiledTransform = TransformCompiler.compile(settings, preset)
        // The encoded output already has its final display aspect. PlayerView may safely
        // preserve that file aspect while the card is sized to the same dimensions below.
        editor.videoPreview.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        when {
            compiledTransform != null -> {
                configurePreviewLayout(compiledTransform.targetWidth, compiledTransform.targetHeight)
            }
            CropCompiler.compile(settings) != null -> {
                val swapsDimensions = info.rotationDegrees == 90 || info.rotationDegrees == 270
                val displayWidth = if (swapsDimensions) info.height else info.width
                val displayHeight = if (swapsDimensions) info.width else info.height
                configurePreviewLayout(
                    (displayWidth * cropRectangle.width).roundToInt().coerceAtLeast(1),
                    (displayHeight * cropRectangle.height).roundToInt().coerceAtLeast(1),
                )
            }
            else -> configurePreviewLayout(info)
        }
    }

    private fun updatePreviewUnderlayEffect(scrollY: Int) {
        val previewHeight = editor.previewCard.height.coerceAtLeast(1)
        val overlap = (scrollY.toFloat() / (previewHeight * 0.72f)).coerceIn(0f, 1f)
        editor.previewBackdrop.alpha = 0.78f * overlap
    }

    private fun refreshDeviceProfile() {
        val profile = DeviceProfileReader.read(this)
        with(binding.settingsContent) {
            settingsDeviceName.text = profile.deviceName
            settingsDeviceType.text = profile.deviceType
            settingsCapabilityValue.text = getString(
                R.string.device_profile_tier,
                profile.capabilityTier.name,
            )
            settingsScreenValue.text = getString(R.string.device_screen_value, profile.screenSummary)
            settingsCpuValue.text = getString(R.string.device_cpu_value, profile.cpuSummary)
            settingsMemoryValue.text = getString(R.string.device_memory_value, profile.memorySummary)
            settingsStorageValue.text = getString(R.string.device_storage_value, profile.storageSummary)
            settingsNetworkValue.text = getString(R.string.device_network_value, profile.networkSummary)
            settingsRecommendation.text = profile.recommendation
        }
        val completed = renderCoordinator.currentState as? RenderUiState.Completed
        if (completed != null && previewPath == completed.outputPath) {
            configureRenderedPreviewLayout(completed.preset)
        } else {
            activeMediaInfo?.let(::configureSourcePreviewLayout)
        }
    }

    private fun bindEditorPreferenceControls() {
        with(binding.settingsContent) {
            settingsAutoRestoreSwitch.isChecked = editorPreferencesStore.autoRestoreEnabled
            settingsAutoRestoreSwitch.setOnCheckedChangeListener { _, enabled ->
                editorPreferencesStore.autoRestoreEnabled = enabled
                scheduleEditorPreferencesSave(immediate = true)
            }
            settingsSavePresetButton.setOnClickListener {
                if (!canChangeSavedEditorPreferences()) return@setOnClickListener
                editorPreferencesStore.savePreset(currentEditorPreferencesSnapshot())
                renderEditorPreferenceControls()
                Snackbar.make(binding.mainRoot, R.string.settings_editor_preferences_status_saved, Snackbar.LENGTH_SHORT)
                    .show()
            }
            settingsRestorePresetButton.setOnClickListener {
                if (!canChangeSavedEditorPreferences()) return@setOnClickListener
                restoreEditorPreferences(
                    editorPreferencesStore.loadPreset(),
                    R.string.settings_no_saved_preset,
                )
            }
            settingsRestoreLastSessionButton.setOnClickListener {
                if (!canChangeSavedEditorPreferences()) return@setOnClickListener
                restoreEditorPreferences(
                    editorPreferencesStore.loadLastSession(),
                    R.string.settings_no_last_session,
                )
            }
            settingsResetCurrentSectionButton.setOnClickListener {
                if (!canChangeSavedEditorPreferences()) return@setOnClickListener
                resetCurrentEditorSection()
            }
            settingsResetAllButton.setOnClickListener {
                if (!canChangeSavedEditorPreferences()) return@setOnClickListener
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.settings_reset_all_title)
                    .setMessage(R.string.settings_reset_all_message)
                    .setNegativeButton(R.string.settings_reset_cancel, null)
                    .setPositiveButton(R.string.settings_reset_confirm) { _, _ ->
                        editorPreferencesStore.clearSavedState()
                        clearAdaptiveDraft()
                        sourceSubtitleBlurRangeInitialized = false
                        sourceSubtitleBlurRangeFollowsTrim = true
                        imageOverlayRangeInitialized = false
                        imageOverlayRangeFollowsTrim = true
                        activeMediaInfo?.let { info ->
                            editor.trimRangeSlider.values = listOf(0f, info.durationMs / 1_000f)
                        }
                        applyEditorPreferences(EditorPreferencesSnapshot())
                        Snackbar.make(
                            binding.mainRoot,
                            R.string.settings_editor_preferences_status_reset,
                            Snackbar.LENGTH_SHORT,
                        ).show()
                    }
                    .show()
            }
        }
        renderEditorPreferenceControls()
    }

    private fun canChangeSavedEditorPreferences(): Boolean {
        if (!renderCoordinator.currentState.isActiveRender()) return true
        Snackbar.make(
            binding.mainRoot,
            R.string.settings_preferences_busy,
            Snackbar.LENGTH_SHORT,
        ).show()
        return false
    }

    private fun renderEditorPreferenceControls() {
        if (!::editorPreferencesStore.isInitialized || _binding == null) return
        val active = ::renderCoordinator.isInitialized &&
            renderCoordinator.currentState.isActiveRender()
        with(binding.settingsContent) {
            if (settingsAutoRestoreSwitch.isChecked != editorPreferencesStore.autoRestoreEnabled) {
                settingsAutoRestoreSwitch.isChecked = editorPreferencesStore.autoRestoreEnabled
            }
            settingsSavePresetButton.isEnabled = !active
            settingsRestorePresetButton.isEnabled = !active && editorPreferencesStore.hasPreset
            settingsRestoreLastSessionButton.isEnabled = !active &&
                editorPreferencesStore.loadLastSession() != null
            settingsResetCurrentSectionButton.isEnabled = !active
            settingsResetAllButton.isEnabled = !active
            settingsEditorPreferencesStatus.setText(
                if (editorPreferencesStore.hasPreset) {
                    R.string.settings_editor_preferences_status_available
                } else {
                    R.string.settings_editor_preferences_status_default
                },
            )
        }
    }

    private fun restoreEditorPreferences(
        snapshot: EditorPreferencesSnapshot?,
        missingMessage: Int,
    ) {
        if (snapshot == null) {
            Snackbar.make(binding.mainRoot, missingMessage, Snackbar.LENGTH_SHORT).show()
            return
        }
        applyEditorPreferences(snapshot)
        Snackbar.make(
            binding.mainRoot,
            R.string.settings_editor_preferences_status_restored,
            Snackbar.LENGTH_SHORT,
        ).show()
    }

    private fun resetCurrentEditorSection() {
        val current = currentEditorPreferencesSnapshot()
        val defaults = EditorPreferencesSnapshot()
        when (selectedReviewEditorTab) {
            ReviewEditorTab.CLIPS -> {
                adaptivePreset = AdaptiveCutPreset.BALANCED
                clearAdaptiveDraft()
                activeMediaInfo?.let { info ->
                    editor.trimRangeSlider.values = listOf(0f, info.durationMs / 1_000f)
                    onUserChangedTrim()
                }
                editor.adaptivePresetGroup.check(adaptivePresetButtonId(adaptivePreset))
            }
            ReviewEditorTab.TRANSFORM -> applyEditorPreferences(
                current.copy(
                    transform = defaults.transform,
                    transformDetailsVisible = defaults.transformDetailsVisible,
                ),
            )
            ReviewEditorTab.AUDIO -> applyEditorPreferences(current.copy(audio = defaults.audio))
            ReviewEditorTab.OVERLAY -> {
                sourceSubtitleBlurRangeInitialized = false
                sourceSubtitleBlurRangeFollowsTrim = true
                imageOverlayRangeInitialized = false
                imageOverlayRangeFollowsTrim = true
                applyEditorPreferences(
                    current.copy(
                        overlay = defaults.overlay,
                        overlayDetailsVisible = defaults.overlayDetailsVisible,
                    ),
                )
            }
            ReviewEditorTab.EXPORT -> {
                selectedRenderPreset = RenderPreset.DEFAULT
                editor.exportQualityGroup.check(renderPresetButtonId(selectedRenderPreset))
                onUserChangedRenderPreset()
            }
        }
        scheduleEditorPreferencesSave(immediate = true)
        Snackbar.make(binding.mainRoot, R.string.settings_section_reset, Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun currentEditorPreferencesSnapshot(): EditorPreferencesSnapshot =
        EditorPreferencesPolicy.sanitize(
            EditorPreferencesSnapshot(
                transform = currentTransformSettings(),
                audio = AudioPreference(
                    enabled = audioEnabled,
                    policy = audioPolicy,
                    volume = audioVolume,
                    mixSourceVolume = mixSourceVolume,
                    mixAddedVolume = mixAddedVolume,
                ),
                overlay = OverlayPreference(
                    enabled = overlayEnabled,
                    blurEnabled = sourceSubtitleBlurEnabled,
                    blurRectangle = sourceSubtitleBlurRectangle,
                    blurStrength = sourceSubtitleBlurStrength,
                    imageEnabled = imageOverlayEnabled,
                    imageCenterX = imageOverlayCenterX,
                    imageCenterY = imageOverlayCenterY,
                    imageWidthFraction = imageOverlayWidthFraction,
                    imageOpacity = imageOverlayOpacity,
                ),
                adaptivePreset = adaptivePreset,
                renderPreset = selectedRenderPreset,
                selectedSection = EditorSection.valueOf(selectedReviewEditorTab.name),
                transformDetailsVisible = transformDetailsVisible,
                overlayDetailsVisible = overlayDetailsVisible,
                previewScale = previewOverlayScale,
                previewCenterX = previewOverlayCenterXFraction,
                previewCenterY = previewOverlayCenterYFraction
                    .takeUnless { it == PREVIEW_POSITION_UNSET },
            ),
        )

    private fun applyEditorPreferencesToState(
        rawSnapshot: EditorPreferencesSnapshot,
        assetDependentSettings: Boolean,
    ) {
        val snapshot = EditorPreferencesPolicy.sanitize(rawSnapshot)
        val transform = snapshot.transform
        transformEnabled = transform.enabled
        transformAspectRatio = transform.aspectRatio
        transformScaleMode = transform.scaleMode
        cropEnabled = transform.crop.enabled
        cropRectangle = transform.crop.rectangle
        mirrorEnabled = transform.mirrorEnabled
        colorEnabled = transform.color.enabled
        colorBrightness = transform.color.brightness
        colorContrast = transform.color.contrast
        colorSaturation = transform.color.saturation
        colorTemperature = transform.color.temperature
        zoomEnabled = transform.zoom.enabled
        zoomMode = transform.zoom.mode
        speedEnabled = transform.speedEnabled
        speedMultiplier = transform.speed
        freezeEnabled = transform.freeze.enabled
        freezeDurationMs = transform.freeze.durationMs
        transitionEnabled = transform.transition.enabled
        transitionMode = transform.transition.mode
        transitionDurationMs = transform.transition.durationMs
        transformDetailsVisible = snapshot.transformDetailsVisible

        audioPolicy = snapshot.audio.policy
        audioVolume = snapshot.audio.volume
        mixSourceVolume = snapshot.audio.mixSourceVolume
        mixAddedVolume = snapshot.audio.mixAddedVolume
        audioEnabled = snapshot.audio.enabled && (
            audioPolicy !in setOf(AudioPolicy.REPLACE, AudioPolicy.MIX) ||
                (assetDependentSettings && replacementAudioAsset != null)
            )

        overlayEnabled = snapshot.overlay.enabled
        overlayDetailsVisible = snapshot.overlayDetailsVisible
        sourceSubtitleBlurEnabled = snapshot.overlay.blurEnabled
        sourceSubtitleBlurRectangle = snapshot.overlay.blurRectangle
        sourceSubtitleBlurStrength = snapshot.overlay.blurStrength
        imageOverlayCenterX = snapshot.overlay.imageCenterX
        imageOverlayCenterY = snapshot.overlay.imageCenterY
        imageOverlayWidthFraction = snapshot.overlay.imageWidthFraction
        imageOverlayOpacity = snapshot.overlay.imageOpacity
        imageOverlayEnabled = snapshot.overlay.imageEnabled &&
            assetDependentSettings && imageOverlayAsset != null

        adaptivePreset = snapshot.adaptivePreset
        selectedRenderPreset = snapshot.renderPreset
        selectedReviewEditorTab = ReviewEditorTab.valueOf(snapshot.selectedSection.name)
        previewOverlayScale = snapshot.previewScale
        previewOverlayCenterXFraction = snapshot.previewCenterX
        previewOverlayCenterYFraction = snapshot.previewCenterY ?: PREVIEW_POSITION_UNSET
    }

    private fun applyEditorPreferences(snapshot: EditorPreferencesSnapshot) {
        applyEditorPreferencesToState(snapshot, assetDependentSettings = true)
        syncEditorPreferenceViewsFromState()
        activeMediaInfo?.let {
            ensureSourceBlurRange()
            ensureImageOverlayRange()
            onUserChangedTransform()
            onUserChangedAudio()
            onUserChangedOverlay(reason = "restored editor preferences")
        }
        scheduleEditorPreferencesSave(immediate = true)
    }

    private fun syncEditorPreferenceViewsFromState() {
        editor.transformEnabledSwitch.isChecked = transformEnabled
        editor.aspectRatioGroup.check(aspectRatioButtonId(transformAspectRatio))
        editor.scaleModeGroup.check(scaleModeButtonId(transformScaleMode))
        editor.cropEnabledSwitch.isChecked = cropEnabled
        editor.cropLeftSlider.value = cropRectangle.left * 100f
        editor.cropTopSlider.value = cropRectangle.top * 100f
        editor.cropRightSlider.value = (1f - cropRectangle.right) * 100f
        editor.cropBottomSlider.value = (1f - cropRectangle.bottom) * 100f
        editor.mirrorEnabledSwitch.isChecked = mirrorEnabled
        editor.colorEnabledSwitch.isChecked = colorEnabled
        editor.colorBrightnessSlider.value = colorBrightness
        editor.colorContrastSlider.value = colorContrast
        editor.colorSaturationSlider.value = colorSaturation
        editor.colorTemperatureSlider.value = colorTemperature
        editor.zoomEnabledSwitch.isChecked = zoomEnabled
        editor.zoomModeGroup.check(zoomModeButtonId(zoomMode))
        editor.speedEnabledSwitch.isChecked = speedEnabled
        editor.speedModeGroup.check(speedButtonId(speedMultiplier))
        editor.freezeEnabledSwitch.isChecked = freezeEnabled
        editor.freezeDurationGroup.check(freezeDurationButtonId(freezeDurationMs))
        editor.transitionEnabledSwitch.isChecked = transitionEnabled
        editor.transitionModeGroup.check(transitionModeButtonId(transitionMode))
        editor.transitionDurationGroup.check(transitionDurationButtonId(transitionDurationMs))

        editor.audioEnabledSwitch.isChecked = audioEnabled
        editor.audioPolicyGroup.check(audioPolicyButtonId(audioPolicy))
        editor.audioVolumeSlider.value = audioVolume * 100f
        editor.mixSourceVolumeSlider.value = mixSourceVolume * 100f
        editor.mixAddedVolumeSlider.value = mixAddedVolume * 100f

        editor.overlayEnabledSwitch.isChecked = overlayEnabled
        editor.sourceBlurEnabledSwitch.isChecked = sourceSubtitleBlurEnabled
        editor.sourceBlurStrengthSlider.value = sourceSubtitleBlurStrength
        editor.imageOverlayEnabledSwitch.isChecked = imageOverlayEnabled
        renderSourceBlurGeometryControls()
        renderImageOverlayControls()

        editor.adaptivePresetGroup.check(adaptivePresetButtonId(adaptivePreset))
        editor.exportQualityGroup.check(renderPresetButtonId(selectedRenderPreset))
        editor.reviewEditorTabGroup.check(
            when (selectedReviewEditorTab) {
                ReviewEditorTab.CLIPS -> R.id.reviewClipsTabButton
                ReviewEditorTab.TRANSFORM -> R.id.reviewTransformTabButton
                ReviewEditorTab.AUDIO -> R.id.reviewAudioTabButton
                ReviewEditorTab.OVERLAY -> R.id.reviewOverlayTabButton
                ReviewEditorTab.EXPORT -> R.id.reviewExportTabButton
            },
        )
        renderTransformControls()
        renderAudioControls()
        renderOverlayControls()
        renderAdaptiveCutControls()
        renderExportQualityControls()
        renderReviewEditorTab()
        applyPreviewOverlayLayout()
    }

    private fun scheduleEditorPreferencesSave(immediate: Boolean = false) {
        if (!::editorPreferencesStore.isInitialized || _binding == null) return
        editorPreferencesHandler.removeCallbacks(persistEditorPreferences)
        if (immediate) {
            persistEditorPreferences.run()
        } else {
            editorPreferencesHandler.postDelayed(
                persistEditorPreferences,
                EDITOR_PREFERENCES_SAVE_DELAY_MS,
            )
        }
    }

    private fun startNextRender() {
        val mediaInfo = activeMediaInfo ?: return
        if (renderCoordinator.currentState.isActiveRender()) return
        if (publicExportCoordinator.currentState is PublicExportUiState.Publishing) return
        val preset = selectedRenderPreset
        val editPlan = currentEditPlan(preset)
        val firstIssue = EditPlanValidator.validate(editPlan).firstOrNull()
        if (firstIssue != null) {
            Snackbar.make(
                binding.mainRoot,
                validationMessage(firstIssue),
                Snackbar.LENGTH_SHORT,
            ).show()
            return
        }
        // Release the preview decoder before Transformer requests the device
        // decoder/encoder pair; lower-end devices expose very limited codecs.
        cancelFreezePreview()
        cancelAdaptivePreview(restoreSource = false)
        releaseCompositionPreview(attachExoPlayer = true, reason = "final render codec handoff")
        previewPath = null
        stopReplacementAudioPreview(clearMedia = true)
        previewPlayer.stop()
        previewPlayer.clearMediaItems()
        publicExportCoordinator.reset()
        renderCoordinator.start(mediaInfo, editPlan)
    }

    private fun playRenderedOutput() {
        val state = renderCoordinator.currentState as? RenderUiState.Completed ?: return
        configureRenderedPreviewLayout(state.preset)
        preparePreview(state.outputPath, autoPlay = true)
    }

    private fun requestPublicExport() {
        val completed = renderCoordinator.currentState as? RenderUiState.Completed ?: return
        if (!publicExportCoordinator.needsLegacyWritePermission()) {
            publicExportCoordinator.publish(completed.outputPath, force = true)
            return
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.export_legacy_permission_title)
                .setMessage(R.string.export_legacy_permission_message)
                .setPositiveButton(R.string.export_allow_and_save) { _, _ ->
                    legacyPublicExportPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            legacyPublicExportPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun openPublishedExport() {
        val published = publicExportCoordinator.currentState as? PublicExportUiState.Published
            ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(published.contentUri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(intent) }.onFailure {
            Snackbar.make(binding.mainRoot, R.string.export_open_failed, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun sharePublishedExport() {
        val published = publicExportCoordinator.currentState as? PublicExportUiState.Published
            ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, published.contentUri)
            clipData = ClipData.newUri(contentResolver, published.displayName, published.contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.export_share_chooser)))
        }.onFailure {
            Snackbar.make(binding.mainRoot, R.string.export_share_failed, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun renderPublicExportState(state: PublicExportUiState) {
        val completed = renderCoordinator.currentState as? RenderUiState.Completed
        editor.exportProgressIndicator.isVisible = state is PublicExportUiState.Publishing
        editor.exportSaveButton.isVisible = state !is PublicExportUiState.Published
        editor.exportSaveButton.isEnabled = completed != null && state !is PublicExportUiState.Publishing
        editor.exportOpenButton.isVisible = state is PublicExportUiState.Published
        editor.exportShareButton.isVisible = state is PublicExportUiState.Published
        when (state) {
            PublicExportUiState.Idle -> {
                editor.exportStatusTitle.setText(
                    if (completed == null) R.string.export_waiting_title else R.string.export_private_ready,
                )
                editor.exportStatusDetail.setText(
                    if (completed == null) R.string.export_waiting_detail else R.string.export_private_detail,
                )
                editor.exportSaveButton.setText(R.string.export_save_to_gallery)
            }
            is PublicExportUiState.PermissionRequired -> {
                editor.exportStatusTitle.setText(R.string.export_permission_required)
                editor.exportStatusDetail.setText(R.string.export_permission_detail)
                editor.exportSaveButton.setText(R.string.export_allow_and_save)
            }
            is PublicExportUiState.Publishing -> {
                editor.exportStatusTitle.setText(R.string.export_publishing_title)
                editor.exportStatusDetail.text = getString(
                    R.string.export_publishing_detail,
                    state.displayName,
                )
                editor.exportSaveButton.setText(R.string.export_publishing_button)
            }
            is PublicExportUiState.Published -> {
                editor.exportStatusTitle.setText(R.string.export_published_title)
                editor.exportStatusDetail.text = getString(
                    R.string.export_published_detail,
                    state.publicLocation,
                )
                editor.exportOpenButton.isEnabled = true
                editor.exportShareButton.isEnabled = true
                Snackbar.make(binding.mainRoot, R.string.export_published_snackbar, Snackbar.LENGTH_SHORT)
                    .show()
            }
            is PublicExportUiState.Failed -> {
                editor.exportStatusTitle.setText(R.string.export_failed_title)
                editor.exportStatusDetail.text = state.message
                editor.exportSaveButton.setText(R.string.export_retry)
            }
        }
        renderExportQualityControls()
        if (completed != null) {
            editor.nextGateButton.isEnabled = state !is PublicExportUiState.Publishing
        }
    }

    private fun confirmRenderCancellation() {
        if (!renderCoordinator.currentState.isActiveRender()) {
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cancel_render_title)
            .setMessage(R.string.cancel_render_message)
            .setNegativeButton(R.string.keep_rendering, null)
            .setPositiveButton(R.string.cancel_and_delete) { _, _ ->
                renderCoordinator.cancel()
            }
            .show()
    }

    private fun renderRenderState(state: RenderUiState) {
        val active = state.isActiveRender()
        editor.importButton.isEnabled = !active
        editor.chooseAnotherButton.isEnabled = !active
        editor.chooseAnotherErrorButton.isEnabled = !active
        binding.homeContent.homeImportButton.isEnabled = !active
        binding.homeContent.homeChooseAnotherButton.isEnabled = !active
        editor.trimRangeSlider.isEnabled = !active
        editor.resetTrimButton.isEnabled = !active
        if (::clipTransitionEditorController.isInitialized) {
            clipTransitionEditorController.setBusy(active)
        }
        renderTransformControls()
        renderAdaptiveCutControls()
        renderAudioControls()
        renderOverlayControls()
        renderExportQualityControls()
        renderEditorPreferenceControls()
        editor.cancelRenderButton.isVisible = active
        editor.playOutputButton.isVisible = state is RenderUiState.Completed
        editor.renderProgressIndicator.isVisible = active || state is RenderUiState.Completed
        editor.renderStage.isVisible = state !is RenderUiState.Idle
        editor.renderElapsed.isVisible = state !is RenderUiState.Idle
        editor.renderDestination.isVisible = when (state) {
            is RenderUiState.Preparing,
            is RenderUiState.Rendering,
            is RenderUiState.Finalizing,
            is RenderUiState.Completed -> true
            else -> false
        }

        when (state) {
            is RenderUiState.Idle -> renderIdleRender(state)
            is RenderUiState.Preparing -> renderPreparingRender(state)
            is RenderUiState.Rendering -> renderActiveRender(state)
            is RenderUiState.Finalizing -> renderFinalizing(state)
            is RenderUiState.Completed -> renderCompletedRender(state)
            is RenderUiState.Failed -> renderFailedRender(state)
            is RenderUiState.Cancelled -> renderCancelled(state)
        }
        if (state is RenderUiState.Completed) {
            publicExportCoordinator.publish(state.outputPath)
        }
    }

    private fun renderIdleRender(state: RenderUiState.Idle) {
        editor.phase6Badge.setText(R.string.phase_6_ready)
        editor.renderTitle.text = getString(
            R.string.render_selected_title,
            selectedRenderPreset.displayName,
        )
        val capabilitySummary = if (state.capability.available) {
            state.capability.videoEncoderName?.let { getString(R.string.render_encoder, it) }
                ?: getString(R.string.next_gate_subtitle)
        } else {
            state.capability.reason ?: getString(R.string.render_encoder_unavailable)
        }
        editor.renderSubtitle.text = buildString {
            append(capabilitySummary)
            if (state.capability.available) {
                activeMediaInfo?.let { info ->
                    val quality = RenderQualityPolicy.forSource(
                        info,
                        selectedRenderPreset,
                    )
                    append('\n')
                    append(
                        getString(
                            R.string.render_quality_target,
                            MediaFormatters.bitrate(
                                this@MainActivity,
                                quality.requestedVideoBitrate.toLong(),
                            ),
                        ),
                    )
                    if (quality.isUpscaling) {
                        append('\n')
                        append(
                            getString(
                                R.string.render_quality_upscale_warning,
                                quality.sourceShortSidePixels,
                                selectedRenderPreset.displayName,
                            ),
                        )
                    }
                    if (quality.isPreviousRecapFlowExport) {
                        append('\n')
                        append(getString(R.string.render_quality_generation_warning))
                    }
                }
            }
        }
        editor.nextGateButton.isVisible = true
        editor.nextGateButton.isEnabled = state.capability.available && isCurrentTrimValid()
        editor.nextGateButton.text = getString(
            R.string.render_selected_title,
            selectedRenderPreset.displayName,
        )
        editor.renderProgressIndicator.isVisible = false
    }

    private fun renderPreparingRender(state: RenderUiState.Preparing) {
        renderRunningShell(state.preset, state.outputPath, state.elapsedMs)
        editor.renderTitle.setText(R.string.render_preparing)
        editor.renderStage.setText(R.string.render_progress_waiting)
        showRenderProgress(null)
    }

    private fun renderActiveRender(state: RenderUiState.Rendering) {
        renderRunningShell(state.preset, state.outputPath, state.elapsedMs)
        editor.renderTitle.text = getString(R.string.render_rendering, state.preset.displayName)
        editor.renderStage.text = state.progressPercent?.let {
            getString(R.string.render_progress_percent, it)
        } ?: getString(R.string.render_progress_waiting)
        showRenderProgress(state.progressPercent)
    }

    private fun renderFinalizing(state: RenderUiState.Finalizing) {
        renderRunningShell(state.preset, state.outputPath, state.elapsedMs)
        editor.renderTitle.setText(R.string.render_finalizing)
        editor.renderStage.setText(R.string.render_finalizing)
        showRenderProgress(99)
    }

    private fun renderRunningShell(preset: RenderPreset, outputPath: String, elapsedMs: Long) {
        editor.phase6Badge.setText(R.string.phase_6_rendering)
        val compiledAudio = AudioCompiler.compile(currentAudioSettings())
        val renderSummary = getString(
            when {
                compiledAudio?.mixesSourceAudio == true -> R.string.rendering_local_mixed_summary
                compiledAudio?.replacement != null -> R.string.rendering_local_replaced_summary
                compiledAudio?.removeAudio == true -> R.string.rendering_local_muted_summary
                else -> R.string.rendering_local_summary
            },
            preset.displayName,
        )
        editor.renderSubtitle.text = buildString {
            append(renderSummary)
            activeMediaInfo?.let { info ->
                val quality = RenderQualityPolicy.forSource(info, preset)
                append('\n')
                append(
                    getString(
                        R.string.render_quality_target,
                        MediaFormatters.bitrate(
                            this@MainActivity,
                            quality.requestedVideoBitrate.toLong(),
                        ),
                    ),
                )
            }
        }
        editor.renderElapsed.text = getString(R.string.render_elapsed, elapsedText(elapsedMs))
        editor.renderDestination.text = getString(R.string.render_destination, outputPath)
        editor.nextGateButton.isVisible = true
        editor.nextGateButton.isEnabled = false
        editor.nextGateButton.text = getString(R.string.render_rendering, preset.displayName)
    }

    private fun renderCompletedRender(state: RenderUiState.Completed) {
        editor.phase6Badge.setText(R.string.phase_6_complete)
        editor.renderTitle.text = getString(R.string.render_complete_title, state.preset.displayName)
        editor.renderSubtitle.text = buildString {
            append(
                getString(
                    R.string.render_complete_summary,
                    MediaFormatters.fileSize(this@MainActivity, state.outputSizeBytes),
                    state.videoEncoderName ?: getString(R.string.unknown_value),
                ),
            )
            append('\n')
            append(
                getString(
                    R.string.render_quality_result,
                    MediaFormatters.bitrate(
                        this@MainActivity,
                        state.requestedVideoBitrate.toLong(),
                    ),
                    state.averageVideoBitrate?.let {
                        MediaFormatters.bitrate(this@MainActivity, it.toLong())
                    } ?: getString(R.string.render_quality_not_reported),
                ),
            )
        }
        editor.renderStage.text = buildString {
            append(
                getString(
                    R.string.render_quality_verified,
                    state.outputWidth,
                    state.outputHeight,
                    state.preset.accessibilityName,
                    getString(
                        if (state.outputHasAudio) {
                            R.string.render_quality_audio_aac
                        } else {
                            R.string.render_quality_audio_none
                        },
                    ),
                ),
            )
            append('\n')
            append(getString(R.string.render_output_validated))
            if (state.sourceWasUpscaled && state.sourceShortSidePixels > 0) {
                append('\n')
                append(
                    getString(
                        R.string.render_quality_upscale_warning,
                        state.sourceShortSidePixels,
                        state.preset.displayName,
                    ),
                )
            }
            if (state.sourceWasPreviousRecapFlowExport) {
                append('\n')
                append(getString(R.string.render_quality_generation_warning))
            }
            state.validationWarnings.forEach { warning ->
                append('\n')
                append(getString(R.string.render_quality_warning, warning))
            }
        }
        editor.renderElapsed.text = getString(
            R.string.render_metrics,
            elapsedText(state.elapsedMs),
            MediaFormatters.duration(state.plannedDurationMs),
            state.realtimeFactor ?: 0.0,
        )
        editor.renderDestination.text = getString(R.string.render_destination, state.outputPath)
        showRenderProgress(100)
        editor.nextGateButton.isVisible = true
        editor.nextGateButton.isEnabled = true
        editor.nextGateButton.text = getString(R.string.render_again, state.preset.displayName)
    }

    private fun renderFailedRender(state: RenderUiState.Failed) {
        restoreSourcePreviewAfterStoppedRender()
        editor.phase6Badge.setText(R.string.phase_6_attention)
        editor.renderTitle.setText(R.string.render_failed_title)
        editor.renderSubtitle.text = state.message
        editor.renderStage.text = state.diagnostics
        editor.renderElapsed.text = getString(R.string.render_elapsed, elapsedText(state.elapsedMs))
        editor.nextGateButton.isVisible = true
        editor.nextGateButton.isEnabled = true
        editor.nextGateButton.text = getString(
            R.string.render_try_again,
            selectedRenderPreset.displayName,
        )
        editor.renderProgressIndicator.isVisible = false
    }

    private fun renderCancelled(state: RenderUiState.Cancelled) {
        restoreSourcePreviewAfterStoppedRender()
        editor.phase6Badge.setText(R.string.phase_6_attention)
        editor.renderTitle.setText(R.string.render_cancelled_title)
        editor.renderSubtitle.setText(R.string.render_cancelled_summary)
        editor.renderStage.setText(R.string.render_cancelled_summary)
        editor.renderElapsed.text = getString(R.string.render_elapsed, elapsedText(state.elapsedMs))
        editor.nextGateButton.isVisible = true
        editor.nextGateButton.isEnabled = true
        editor.nextGateButton.text = getString(
            R.string.render_try_again,
            selectedRenderPreset.displayName,
        )
        editor.renderProgressIndicator.isVisible = false
    }

    private fun restoreSourcePreviewAfterStoppedRender() {
        val info = activeMediaInfo ?: return
        val sourcePath = info.workingFilePath
        configureSourcePreviewLayout(info)
        if (previewPath != sourcePath) {
            preparePreview(sourcePath, autoPlay = false)
        }
    }

    private fun configureTrim(info: MediaInfo) {
        val maxSeconds = (info.durationMs / 1_000f).coerceAtLeast(1f)
        editor.trimRangeSlider.valueFrom = 0f
        editor.trimRangeSlider.valueTo = maxSeconds

        val startMs = restoredTrimStartMs
            ?.coerceIn(0L, info.durationMs)
            ?: 0L
        val endMs = restoredTrimEndMs
            ?.coerceIn(startMs, info.durationMs)
            ?: info.durationMs
        editor.trimRangeSlider.values = listOf(startMs / 1_000f, endMs / 1_000f)
        restoredTrimStartMs = null
        restoredTrimEndMs = null
        updateTrimSummary()
    }

    private fun resetTrimToFullSource() {
        val info = activeMediaInfo ?: return
        editor.trimRangeSlider.values = listOf(0f, info.durationMs / 1_000f)
        updateTrimSummary()
        onUserChangedTrim()
    }

    private fun onUserChangedTrim() {
        scheduleEditorPreferencesSave()
        val info = activeMediaInfo ?: return
        cancelFreezePreview()
        clearAdaptiveDraft()
        if (::clipTransitionEditorController.isInitialized) {
            clipTransitionEditorController.reconcile()
        }
        // Untouched overlay time windows are linked to the current Trim. This prevents a blur/logo
        // initialized on an earlier shorter Trim from silently ending halfway after Clips is
        // expanded again. Explicitly edited overlay time windows remain absolute and unchanged.
        ensureSourceBlurRange()
        ensureImageOverlayRange()
        renderOverlayControls()
        if (!renderCoordinator.currentState.isActiveRender() &&
            renderCoordinator.currentState !is RenderUiState.Idle
        ) {
            renderCoordinator.reset(mediaHasAudio = renderNeedsAudioCapability(info))
            restoreSourcePreviewAfterStoppedRender()
        }
        if (previewPath == info.workingFilePath) {
            val trim = currentTrimRange(info)
            if (!previewFallbackActive) {
                updateRealtimeOverlayStates()
                requestSourceBlurPreviewUpdate("trim range", immediate = false)
            }
            activePreviewSeekToSourcePosition(info, trim.startMs)
        }
        renderTransformControls()
        updateTrimSummary()
    }

    private fun onUserChangedTransform() {
        scheduleEditorPreferencesSave()
        val info = activeMediaInfo ?: return
        cancelFreezePreview()
        cancelAdaptivePreview()
        if (!renderCoordinator.currentState.isActiveRender() &&
            renderCoordinator.currentState !is RenderUiState.Idle
        ) {
            renderCoordinator.reset(mediaHasAudio = renderNeedsAudioCapability(info))
            restoreSourcePreviewAfterStoppedRender()
        }
        requestSourceBlurPreviewUpdate("transform controls", immediate = false)
        refreshAudioPreview()
        renderAdaptiveCutControls()
        updateTrimSummary()
    }

    private fun previewIntroFreeze() {
        val info = activeMediaInfo ?: return
        if (!transformEnabled || !freezeEnabled || renderCoordinator.currentState.isActiveRender()) {
            return
        }
        cancelAdaptivePreview()
        cancelFreezePreview()
        if (compositionPreviewActive) {
            val sourcePositionMs = activePreviewSourcePositionMs(info)
            releaseCompositionPreview(attachExoPlayer = true, reason = "intro freeze simulation")
            prepareExoPreview(
                info.workingFilePath,
                autoPlay = false,
                startPositionMs = sourcePositionMs,
            )
        } else if (previewPath != info.workingFilePath) {
            prepareExoPreview(info.workingFilePath, autoPlay = false, startPositionMs = currentTrimRange(info).startMs)
        }
        previewPlayer.pause()
        previewPlayer.seekTo(currentTrimRange(info).startMs)
        freezePreviewActive = true
        freezePreviewStartedAtElapsedMs = SystemClock.elapsedRealtime()
        syncReplacementAudioPreview(forceSeek = true)
        renderTransformControls()
        freezePreviewHandler.postDelayed(freezePreviewCompletion, freezeDurationMs)
    }

    private val freezePreviewCompletion = Runnable {
        if (!freezePreviewActive) return@Runnable
        freezePreviewActive = false
        syncReplacementAudioPreview(forceSeek = true)
        renderTransformControls()
        val info = activeMediaInfo
        if (
            info != null &&
            selectedDestination == MainDestination.EDITOR &&
            previewPath == info.workingFilePath &&
            transformEnabled &&
            freezeEnabled &&
            !renderCoordinator.currentState.isActiveRender()
        ) {
            previewPlayer.play()
        }
    }

    private fun cancelFreezePreview() {
        freezePreviewHandler.removeCallbacks(freezePreviewCompletion)
        if (freezePreviewActive) {
            freezePreviewActive = false
            syncReplacementAudioPreview(forceSeek = true)
            if (_binding != null) renderTransformControls()
        }
    }

    private fun applyLiveTransformPreview(
        info: MediaInfo,
        reason: String = "editor settings",
        requestedKey: PreviewGraphKey = currentPreviewGraphKey(info),
    ): Boolean {
        if (previewPath != info.workingFilePath) return false
        if (!realtimePreviewSession.isCurrent(
                info.workingFilePath,
                realtimePreviewSession.currentGeneration(),
            )
        ) {
            return false
        }
        val positionMs = activePreviewSourcePositionMs(info)
        val resumePlayback = activePreviewPlayWhenReady()
        val geometryRebindRequired = PreviewGeometryChangePolicy.requiresSurfaceRebind(
            previous = realtimePreviewSession.currentTransform(),
            requested = requestedKey.transform,
        )

        if (compositionPreviewEligible(info)) {
            val rebuildComposition = {
                runCatching {
                    prepareCompositionPreview(
                        info = info,
                        autoPlay = resumePlayback,
                        sourcePositionMs = positionMs,
                        reason = reason,
                    )
                }.onFailure { error ->
                    Log.e(TAG_PREVIEW, "CompositionPlayer preview rebuild failed: $reason", error)
                    if (compositionPreviewActive) {
                        fallbackFromCompositionPreview("graph rebuild: $reason", error)
                    } else {
                        compositionPreviewBlockedPath = info.workingFilePath
                        prepareExoPreview(
                            workingFilePath = info.workingFilePath,
                            autoPlay = resumePlayback,
                            startPositionMs = positionMs,
                        )
                    }
                }.getOrDefault(false)
            }
            if (geometryRebindRequired) {
                activePreviewPause()
                configureSourcePreviewLayout(info) {
                    if (_binding == null || previewFallbackActive || previewPath != info.workingFilePath) {
                        return@configureSourcePreviewLayout
                    }
                    if (currentPreviewGraphKey(info) != requestedKey) {
                        requestSourceBlurPreviewUpdate(
                            reason = "superseded composition geometry",
                            immediate = false,
                            force = true,
                        )
                        return@configureSourcePreviewLayout
                    }
                    rebuildComposition()
                }
                return true
            }
            configureSourcePreviewLayout(info)
            return rebuildComposition()
        }

        if (compositionPreviewActive) {
            releaseCompositionPreview(attachExoPlayer = true, reason = "capability fallback: $reason")
            prepareExoPreview(
                workingFilePath = info.workingFilePath,
                autoPlay = resumePlayback,
                startPositionMs = positionMs,
            )
            return true
        }

        if (geometryRebindRequired) {
            // PREVIEW_GEOMETRY_REBIND: changing 9:16/16:9/1:1 or FIT/FILL changes Media3
            // Presentation geometry. Rebinding that graph before the movable TextureView has its
            // new bounds can leave the decoded image clipped to one side and can poison the player
            // until source-only recovery. Apply layout first, wait two UI frames, then rebuild only
            // the preview decoder/effect graph. No intermediate media render is created.
            previewPlayer.pause()
            configureSourcePreviewLayout(info) {
                if (
                    _binding == null ||
                    previewFallbackActive ||
                    previewPath != info.workingFilePath
                ) {
                    return@configureSourcePreviewLayout
                }
                if (currentPreviewGraphKey(info) != requestedKey) {
                    requestSourceBlurPreviewUpdate(
                        reason = "superseded preview geometry",
                        immediate = false,
                    )
                    return@configureSourcePreviewLayout
                }
                runCatching {
                    updateRealtimeOverlayStates()
                    val effects = TransformVideoEffects.forPreview(
                        settings = requestedKey.transform,
                        sourceWidth = info.width,
                        sourceHeight = info.height,
                        overlays = currentOverlaySettings(),
                        timelineOffsetUs = requestedKey.timelineOffsetUs,
                        sourceDurationMs = requestedKey.sourceDurationMs,
                        sourceTimeOffsetUs = requestedKey.sourceTimeOffsetUs,
                        realtimeSourceBlurState = realtimeSourceBlurState,
                        realtimeImageOverlayState = realtimeImageOverlayState,
                    )
                    rebuildSourcePreviewGraph(
                        info = info,
                        effects = effects,
                        requestedKey = requestedKey,
                        positionMs = positionMs,
                        resumePlayback = resumePlayback,
                        reason = "surface geometry change: $reason",
                    )
                }.onFailure { geometryError ->
                    Log.e(TAG_PREVIEW, "Preview geometry rebind failed: $reason", geometryError)
                    recoverPreviewSession(
                        failedPath = info.workingFilePath,
                        expectedGeneration = realtimePreviewSession.currentGeneration(),
                        reason = "geometry rebind: $reason",
                        preferredPositionMs = positionMs,
                    )
                }
            }
            return true
        }

        configureSourcePreviewLayout(info)
        return try {
            updateRealtimeOverlayStates()
            val effects = TransformVideoEffects.forPreview(
                settings = requestedKey.transform,
                sourceWidth = info.width,
                sourceHeight = info.height,
                overlays = currentOverlaySettings(),
                timelineOffsetUs = requestedKey.timelineOffsetUs,
                sourceDurationMs = requestedKey.sourceDurationMs,
                sourceTimeOffsetUs = requestedKey.sourceTimeOffsetUs,
                realtimeSourceBlurState = realtimeSourceBlurState,
                realtimeImageOverlayState = realtimeImageOverlayState,
            )
            val requestedSignature = effects.previewEffectSignature()
            if (requestedSignature != previewEffectSignature) {
                rebuildSourcePreviewGraph(
                    info = info,
                    effects = effects,
                    requestedKey = requestedKey,
                    positionMs = positionMs,
                    resumePlayback = resumePlayback,
                    reason = "topology change: $reason",
                )
                return true
            }

            previewPlayer.setVideoEffects(effects)
            previewEffectSignature = requestedSignature
            realtimePreviewSession.markApplying(requestedKey)
            previewPlayer.setPlaybackSpeed(currentPreviewSpeed())
            // Playing previews receive parameter updates on the next frame. A same-position seek
            // asks ExoPlayer to redraw immediately when the user adjusted a paused frame.
            if (!previewPlayer.isPlaying && previewPlayer.playbackState != Player.STATE_IDLE) {
                previewPlayer.seekTo(positionMs)
            }
            Log.d(
                TAG_PREVIEW,
                "Applied live effects reason=$reason count=${effects.size} " +
                    "blur=${overlayEnabled && sourceSubtitleBlurEnabled}",
            )
            true
        } catch (error: RuntimeException) {
            Log.w(
                TAG_PREVIEW,
                "Retained live-effect update rejected; rebuilding preview graph: $reason",
                error,
            )
            val rebuild = runCatching {
                updateRealtimeOverlayStates()
                val effects = TransformVideoEffects.forPreview(
                    settings = requestedKey.transform,
                    sourceWidth = info.width,
                    sourceHeight = info.height,
                    overlays = currentOverlaySettings(),
                    timelineOffsetUs = requestedKey.timelineOffsetUs,
                    sourceDurationMs = requestedKey.sourceDurationMs,
                    sourceTimeOffsetUs = requestedKey.sourceTimeOffsetUs,
                    realtimeSourceBlurState = realtimeSourceBlurState,
                    realtimeImageOverlayState = realtimeImageOverlayState,
                )
                rebuildSourcePreviewGraph(
                    info = info,
                    effects = effects,
                    requestedKey = requestedKey,
                    positionMs = positionMs,
                    resumePlayback = resumePlayback,
                    reason = "rejected retained graph: $reason",
                )
            }
            if (rebuild.isSuccess) {
                true
            } else {
                val rebuildError = rebuild.exceptionOrNull()
                Log.e(TAG_PREVIEW, "Clean live-effect graph rebuild failed: $reason", rebuildError)
                sourceBlurPreviewDirty = false
                recoverPreviewSession(
                    failedPath = info.workingFilePath,
                    expectedGeneration = realtimePreviewSession.currentGeneration(),
                    reason = "graph rebuild: $reason",
                    preferredPositionMs = positionMs,
                )
                false
            }
        }
    }

    /**
     * Recreates only the realtime decoder/effect graph. No output file is generated here. This
     * keeps Clips/Transform/Audio/Overlay independently editable while the immutable EditPlan keeps
     * stacking every enabled operation for the one final Transformer export.
     */
    private fun rebuildSourcePreviewGraph(
        info: MediaInfo,
        effects: List<androidx.media3.common.Effect>,
        requestedKey: PreviewGraphKey,
        positionMs: Long,
        resumePlayback: Boolean,
        reason: String,
    ) {
        cancelPreviewReadyTimeout()
        replacePreviewPlayer()
        previewPath = info.workingFilePath
        previewPlayer.setVideoEffects(effects)
        previewEffectSignature = effects.previewEffectSignature()
        realtimePreviewSession.markApplying(requestedKey)
        previewPlayer.setPlaybackSpeed(currentPreviewSpeed())
        previewPlayer.volume = currentPreviewVolume()
        previewPlayer.setMediaItem(
            MediaItem.fromUri(File(info.workingFilePath).toURI().toString()),
        )
        previewPlayer.playWhenReady = resumePlayback
        previewPlayer.seekTo(positionMs.coerceIn(0L, info.durationMs.coerceAtLeast(0L)))
        previewPlayer.prepare()
        schedulePreviewReadyTimeout(
            path = info.workingFilePath,
            generation = realtimePreviewSession.currentGeneration(),
        )
        refreshAudioPreview()
        Log.i(
            TAG_PREVIEW,
            "Rebuilt live preview graph without render reason=$reason effects=${effects.size}",
        )
    }

    private fun List<androidx.media3.common.Effect>.previewEffectSignature(): List<String> =
        map { effect -> effect.javaClass.name }

    private fun currentPreviewGraphKey(info: MediaInfo): PreviewGraphKey {
        val trim = currentTrimRange(info)
        val settings = currentOverlaySettings()
        return PreviewGraphKey(
            sourcePath = info.workingFilePath,
            transform = currentTransformSettings(),
            sourceBlurPresent = OverlayCompiler.compile(settings) != null,
            imageAssetPath = OverlayCompiler.compileImage(settings)?.asset?.workingFilePath,
            timelineOffsetUs = trim.startMs * 1_000L,
            sourceDurationMs = trim.durationMs,
        )
    }

    private fun updateRealtimeOverlayStates() {
        val settings = currentOverlaySettings()
        realtimeSourceBlurState.update(OverlayCompiler.compile(settings))
        realtimeImageOverlayState.update(OverlayCompiler.compileImage(settings))
    }

    private fun redrawPausedPreviewFrame() {
        val info = activeMediaInfo ?: return
        if (
            previewFallbackActive ||
            compositionPreviewActive ||
            previewPath != info.workingFilePath ||
            previewPlayer.isPlaying ||
            previewPlayer.playbackState == Player.STATE_IDLE
        ) {
            sourceBlurPreviewHandler.removeCallbacks(settlePausedPreviewFrameRefresh)
            pausedPreviewRefreshAnchorMs = null
            return
        }

        // A seek to the exact current timestamp can be optimized away by ExoPlayer, leaving the
        // already-presented paused texture on screen. The realtime blur/logo shader has the new
        // state, but drawFrame() is not called until another frame arrives (for example after the
        // floating preview is resized). Pulse by roughly two source frames, then settle back to the
        // original position. This is preview-only invalidation: no graph rebuild and no render.
        val anchorMs = pausedPreviewRefreshAnchorMs
            ?: runCatching { previewPlayer.currentPosition.coerceAtLeast(0L) }
                .getOrDefault(previewLastValidPositionMs)
                .coerceIn(0L, info.durationMs.coerceAtLeast(0L))
        pausedPreviewRefreshAnchorMs = anchorMs
        val targetMs = PausedPreviewRefreshPolicy.refreshTargetMs(
            anchorMs = anchorMs,
            durationMs = info.durationMs,
            frameRate = info.frameRate,
            preferForward = pausedPreviewRefreshPreferForward,
        )
        pausedPreviewRefreshPreferForward = !pausedPreviewRefreshPreferForward
        sourceBlurPreviewHandler.removeCallbacks(settlePausedPreviewFrameRefresh)
        runCatching {
            previewPlayer.seekTo(targetMs)
            sourceBlurPreviewHandler.postDelayed(
                settlePausedPreviewFrameRefresh,
                PAUSED_PREVIEW_REFRESH_SETTLE_MS,
            )
        }.onFailure { error ->
            pausedPreviewRefreshAnchorMs = null
            Log.w(TAG_PREVIEW, "Paused preview redraw pulse was rejected", error)
        }
    }

    private fun schedulePreviewReadyTimeout(
        path: String? = previewPath,
        generation: Long = realtimePreviewSession.currentGeneration(),
    ) {
        val activePath = path ?: return
        previewRecoveryHandler.removeCallbacks(previewReadyTimeout)
        previewReadyTimeoutPath = activePath
        previewReadyTimeoutGeneration = generation
        previewRecoveryHandler.postDelayed(previewReadyTimeout, PREVIEW_READY_TIMEOUT_MS)
    }

    private fun cancelPreviewReadyTimeout() {
        previewRecoveryHandler.removeCallbacks(previewReadyTimeout)
        previewReadyTimeoutPath = null
    }

    private fun recoverPreviewSession(
        failedPath: String,
        expectedGeneration: Long,
        reason: String,
        preferredPositionMs: Long? = null,
    ) {
        cancelFreezePreview()
        cancelAdaptivePreview(restoreSource = false)
        val info = activeMediaInfo
        val isSourcePreview = info?.workingFilePath == failedPath
        if (
            !isSourcePreview ||
            !realtimePreviewSession.claimRecovery(failedPath, expectedGeneration)
        ) {
            Log.e(
                TAG_PREVIEW,
                "Preview recovery exhausted reason=$reason path=$failedPath " +
                    "generation=$expectedGeneration",
            )
            showPreviewUnavailable("recovery exhausted: $reason")
            return
        }

        val resumePositionMs = preferredPositionMs
            ?: runCatching { previewPlayer.currentPosition.takeIf { it >= 0L } }.getOrNull()
            ?: previewLastValidPositionMs
        val resumePlayback = runCatching { previewPlayer.playWhenReady }.getOrDefault(false) ||
            previewLastPlayWhenReady
        setPreviewUiState(PreviewUiState.SourceOnly(reason))
        cancelSourceBlurPreviewUpdate(clearDirty = true)
        realtimeSourceBlurState.update(null)
        realtimeImageOverlayState.update(null)
        previewEffectSignature = emptyList()
        Snackbar.make(
            binding.mainRoot,
            R.string.live_preview_fallback,
            Snackbar.LENGTH_LONG,
        ).show()
        Log.w(
            TAG_PREVIEW,
            "Recovering retained source preview without live effects reason=$reason " +
                "positionMs=$resumePositionMs",
        )
        runCatching {
            replacePreviewPlayer()
            preparePreview(
                workingFilePath = checkNotNull(info).workingFilePath,
                autoPlay = resumePlayback,
                startPositionMs = resumePositionMs.coerceAtLeast(0L),
            )
        }.onFailure { fallbackError ->
            Log.e(TAG_PREVIEW, "Unable to restore source preview", fallbackError)
            showPreviewUnavailable("source-only recovery failed: ${fallbackError.javaClass.simpleName}")
        }
    }

    private fun retryLivePreviewEffects() {
        val info = activeMediaInfo ?: return
        if (renderCoordinator.currentState.isActiveRender()) return
        val resumePositionMs = runCatching { activePreviewSourcePositionMs(info) }
            .getOrDefault(previewLastValidPositionMs)
            .coerceAtLeast(0L)
        val resumePlayback = runCatching { activePreviewPlayWhenReady() }
            .getOrDefault(previewLastPlayWhenReady)
        cancelPreviewReadyTimeout()
        cancelSourceBlurPreviewUpdate(clearDirty = true)
        val retryGeneration = realtimePreviewSession.begin(info.workingFilePath, restart = true)
        setPreviewUiState(PreviewUiState.LiveEffects)
        updateRealtimeOverlayStates()
        Log.i(
            TAG_PREVIEW,
            "User requested live-effect retry generation=$retryGeneration " +
                "source=${info.width}x${info.height} codec=${info.videoCodec}",
        )
        compositionPreviewBlockedPath = null
        releaseCompositionPreview(attachExoPlayer = true, reason = "explicit live preview retry")
        runCatching {
            preparePreview(
                workingFilePath = info.workingFilePath,
                autoPlay = resumePlayback,
                startPositionMs = resumePositionMs,
            )
        }.onFailure { retryError ->
            Log.e(TAG_PREVIEW, "Live-effect retry failed synchronously", retryError)
            recoverPreviewSession(
                failedPath = info.workingFilePath,
                expectedGeneration = retryGeneration,
                reason = "retry graph setup: ${retryError.javaClass.simpleName}",
                preferredPositionMs = resumePositionMs,
            )
        }
    }

    private fun setPreviewUiState(state: PreviewUiState) {
        previewUiState = state
        if (_binding != null) {
            renderPreviewUiState()
            activeMediaInfo?.let(::configureSourcePreviewLayout)
        }
    }

    private fun renderPreviewUiState() {
        val sourcePreviewSelected = activeMediaInfo?.workingFilePath == previewPath
        editor.previewHint.isVisible = sourcePreviewSelected &&
            previewUiState is PreviewUiState.Unavailable
        editor.retryLivePreviewButton.isVisible = sourcePreviewSelected &&
            previewUiState !is PreviewUiState.LiveEffects
        renderSourceBlurGuide()
    }

    private fun showPreviewUnavailable(reason: String = "preview recovery unavailable") {
        cancelPreviewReadyTimeout()
        realtimePreviewSession.clearPending()
        pauseReplacementAudioPreview()
        releaseCompositionPreview(attachExoPlayer = true, reason = "preview unavailable")
        runCatching {
            previewPlayer.playWhenReady = false
            previewPlayer.stop()
        }
        previewEffectSignature = emptyList()
        Log.e(TAG_PREVIEW, "Preview unavailable reason=$reason")
        setPreviewUiState(PreviewUiState.Unavailable(reason))
    }

    private fun showNonSourcePreviewUnavailable(reason: String) {
        cancelPreviewReadyTimeout()
        pauseReplacementAudioPreview()
        releaseCompositionPreview(attachExoPlayer = true, reason = "non-source preview unavailable")
        runCatching {
            previewPlayer.playWhenReady = false
            previewPlayer.stop()
        }
        Log.e(TAG_PREVIEW, "Rendered/output preview unavailable reason=$reason")
        editor.previewHint.setText(R.string.preview_unavailable)
        editor.previewHint.isVisible = true
        editor.retryLivePreviewButton.isVisible = false
    }

    private fun renderTransformControls() {
        val renderActive = if (::renderCoordinator.isInitialized) {
            renderCoordinator.currentState.isActiveRender()
        } else {
            false
        }
        editor.transformEnabledSwitch.isEnabled = !renderActive
        val controlsEnabled = transformEnabled && !renderActive
        val scaleControlsEnabled = controlsEnabled &&
            transformAspectRatio != AspectRatioPreset.ORIGINAL
        editor.transformControlsGroup.isVisible = transformDetailsVisible
        editor.transformControlsGroup.alpha = if (transformEnabled) 1f else 0.46f
        editor.transformVisibilityButton.setText(
            if (transformDetailsVisible) {
                R.string.transform_hide_controls
            } else {
                R.string.transform_show_controls
            },
        )
        editor.aspectRatioGroup.setChildrenEnabled(controlsEnabled)
        editor.scaleModeGroup.alpha = if (
            transformEnabled && transformAspectRatio == AspectRatioPreset.ORIGINAL
        ) {
            0.46f
        } else {
            1f
        }
        editor.scaleModeGroup.setChildrenEnabled(scaleControlsEnabled)
        editor.cropEnabledSwitch.isEnabled = controlsEnabled
        editor.mirrorEnabledSwitch.isEnabled = controlsEnabled
        editor.colorEnabledSwitch.isEnabled = controlsEnabled
        editor.zoomEnabledSwitch.isEnabled = controlsEnabled
        editor.speedEnabledSwitch.isEnabled = controlsEnabled
        editor.freezeEnabledSwitch.isEnabled = controlsEnabled
        editor.transitionEnabledSwitch.isEnabled = controlsEnabled
        val cropControlsEnabled = controlsEnabled && cropEnabled
        editor.cropControlsGroup.isVisible = transformEnabled && cropEnabled
        editor.cropControlsGroup.alpha = if (transformEnabled && cropEnabled) 1f else 0.46f
        editor.cropControlsGroup.setChildrenEnabled(cropControlsEnabled)
        val colorControlsEnabled = controlsEnabled && colorEnabled
        editor.colorControlsGroup.isVisible = transformEnabled && colorEnabled
        editor.colorControlsGroup.alpha = if (transformEnabled && colorEnabled) 1f else 0.46f
        editor.colorControlsGroup.setChildrenEnabled(colorControlsEnabled)
        val zoomControlsEnabled = controlsEnabled && zoomEnabled
        editor.zoomControlsGroup.isVisible = transformEnabled && zoomEnabled
        editor.zoomControlsGroup.alpha = if (transformEnabled && zoomEnabled) 1f else 0.46f
        editor.zoomModeGroup.setChildrenEnabled(zoomControlsEnabled)
        val speedControlsEnabled = controlsEnabled && speedEnabled
        editor.speedControlsGroup.isVisible = transformEnabled && speedEnabled
        editor.speedControlsGroup.alpha = if (transformEnabled && speedEnabled) 1f else 0.46f
        editor.speedModeGroup.setChildrenEnabled(speedControlsEnabled)
        val freezeControlsEnabled = controlsEnabled && freezeEnabled && !freezePreviewActive
        editor.freezeControlsGroup.isVisible = transformEnabled && freezeEnabled
        editor.freezeControlsGroup.alpha = if (transformEnabled && freezeEnabled) 1f else 0.46f
        editor.freezeDurationGroup.setChildrenEnabled(freezeControlsEnabled)
        editor.freezePreviewButton.isEnabled = freezeControlsEnabled && activeMediaInfo != null
        editor.freezePreviewButton.setText(
            if (freezePreviewActive) R.string.freeze_previewing else R.string.freeze_preview,
        )
        val transitionControlsEnabled = controlsEnabled && transitionEnabled
        editor.transitionControlsGroup.isVisible = transformEnabled && transitionEnabled
        editor.transitionControlsGroup.alpha = if (transformEnabled && transitionEnabled) 1f else 0.46f
        editor.transitionModeGroup.setChildrenEnabled(transitionControlsEnabled)
        editor.transitionDurationGroup.setChildrenEnabled(transitionControlsEnabled)
        editor.transformBadge.setText(
            if (transformEnabled) R.string.transform_badge_on else R.string.transform_badge_off,
        )
        val baseSummary = if (
            transformEnabled && transformAspectRatio == AspectRatioPreset.ORIGINAL
        ) {
            getString(R.string.transform_original_summary)
        } else if (transformEnabled) {
            getString(
                R.string.transform_on_summary,
                aspectRatioLabel(transformAspectRatio),
                scaleModeDescription(transformScaleMode),
            )
        } else {
            getString(R.string.transform_off_summary)
        }
        editor.transformSummary.text = buildString {
            append(baseSummary)
            if (transformEnabled && cropEnabled) append(getString(R.string.transform_crop_suffix))
            if (transformEnabled && mirrorEnabled) append(getString(R.string.transform_mirror_suffix))
            if (transformEnabled && colorEnabled) append(getString(R.string.transform_color_suffix))
            if (transformEnabled && zoomEnabled) append(getString(R.string.transform_zoom_suffix))
            if (transformEnabled && speedEnabled) {
                append(getString(R.string.transform_speed_suffix, speedLabel(speedMultiplier)))
            }
            if (transformEnabled && freezeEnabled) {
                append(getString(R.string.transform_freeze_suffix, freezeDurationLabel(freezeDurationMs)))
            }
            if (transformEnabled && transitionEnabled) {
                append(
                    getString(
                        R.string.transform_transition_suffix,
                        transitionModeLabel(transitionMode),
                        transitionDurationLabel(transitionDurationMs),
                    ),
                )
            }
        }
        editor.mirrorSummary.setText(
            when {
                !mirrorEnabled -> R.string.mirror_off_summary
                transformEnabled -> R.string.mirror_on_summary
                else -> R.string.mirror_remembered_summary
            },
        )
        editor.colorSummary.text = when {
            !colorEnabled -> getString(R.string.color_off_summary)
            !transformEnabled -> getString(R.string.color_remembered_summary)
            ColorSettings(
                enabled = true,
                brightness = colorBrightness,
                contrast = colorContrast,
                saturation = colorSaturation,
                temperature = colorTemperature,
            ).isNeutral() -> getString(R.string.color_neutral_summary)
            else -> getString(
                R.string.color_on_summary,
                colorBrightness.roundToInt(),
                colorContrast.roundToInt(),
                colorSaturation.roundToInt(),
                colorTemperature.roundToInt(),
            )
        }
        editor.colorBrightnessValue.text = getString(
            R.string.color_brightness_value,
            colorBrightness.roundToInt(),
        )
        editor.colorContrastValue.text = getString(
            R.string.color_contrast_value,
            colorContrast.roundToInt(),
        )
        editor.colorSaturationValue.text = getString(
            R.string.color_saturation_value,
            colorSaturation.roundToInt(),
        )
        editor.colorTemperatureValue.text = getString(
            R.string.color_temperature_value,
            colorTemperature.roundToInt(),
        )
        editor.zoomSummary.text = when {
            !zoomEnabled -> getString(R.string.zoom_off_summary)
            !transformEnabled -> getString(
                R.string.zoom_remembered_summary,
                zoomModeLabel(zoomMode),
            )
            else -> getString(R.string.zoom_on_summary, zoomModeLabel(zoomMode))
        }
        editor.speedSummary.text = when {
            !speedEnabled -> getString(R.string.speed_off_summary)
            !transformEnabled -> getString(
                R.string.speed_remembered_summary,
                speedLabel(speedMultiplier),
            )
            else -> {
                val estimatedDurationMs = activeMediaInfo?.let { info ->
                    val sourceDurationMs = currentTrimRange(info).durationMs
                    SpeedCompiler.compile(currentTransformSettings())
                        ?.outputDurationMs(sourceDurationMs)
                        ?: sourceDurationMs
                } ?: 0L
                getString(
                    R.string.speed_on_summary,
                    speedLabel(speedMultiplier),
                    MediaFormatters.duration(estimatedDurationMs),
                )
            }
        }
        editor.freezeSummary.text = when {
            !freezeEnabled -> getString(R.string.freeze_off_summary)
            !transformEnabled -> getString(
                R.string.freeze_remembered_summary,
                freezeDurationLabel(freezeDurationMs),
            )
            else -> {
                val estimatedDurationMs = activeMediaInfo?.let {
                    currentEditPlan(selectedRenderPreset).plannedDurationMs
                } ?: freezeDurationMs
                getString(
                    R.string.freeze_on_summary,
                    freezeDurationLabel(freezeDurationMs),
                    MediaFormatters.duration(estimatedDurationMs),
                )
            }
        }
        editor.transitionSummary.text = when {
            !transitionEnabled -> getString(R.string.transition_off_summary)
            !transformEnabled -> getString(
                R.string.transition_remembered_summary,
                transitionModeLabel(transitionMode),
                transitionDurationLabel(transitionDurationMs),
            )
            adaptiveApplied -> getString(
                R.string.transition_adaptive_summary,
                transitionModeLabel(transitionMode),
                transitionDurationLabel(transitionDurationMs),
            )
            else -> getString(
                R.string.transition_on_summary,
                transitionModeLabel(transitionMode),
                transitionDurationLabel(transitionDurationMs),
            )
        }
        editor.cropSummary.text = if (cropEnabled) {
            getString(
                R.string.crop_on_summary,
                cropRectangle.left * 100f,
                cropRectangle.top * 100f,
                (1f - cropRectangle.right) * 100f,
                (1f - cropRectangle.bottom) * 100f,
            )
        } else {
            getString(R.string.crop_off_summary)
        }
        editor.cropLeftValue.text = getString(R.string.crop_left_value, cropRectangle.left * 100f)
        editor.cropTopValue.text = getString(R.string.crop_top_value, cropRectangle.top * 100f)
        editor.cropRightValue.text = getString(
            R.string.crop_right_value,
            (1f - cropRectangle.right) * 100f,
        )
        editor.cropBottomValue.text = getString(
            R.string.crop_bottom_value,
            (1f - cropRectangle.bottom) * 100f,
        )
    }

    private fun aspectRatioButtonId(aspectRatio: AspectRatioPreset): Int = when (aspectRatio) {
        AspectRatioPreset.ORIGINAL -> R.id.aspectOriginalButton
        AspectRatioPreset.PORTRAIT_9_16 -> R.id.aspectPortraitButton
        AspectRatioPreset.LANDSCAPE_16_9 -> R.id.aspectLandscapeButton
        AspectRatioPreset.SQUARE_1_1 -> R.id.aspectSquareButton
    }

    private fun adaptivePresetButtonId(preset: AdaptiveCutPreset): Int = when (preset) {
        AdaptiveCutPreset.GENTLE -> R.id.adaptiveGentleButton
        AdaptiveCutPreset.BALANCED -> R.id.adaptiveBalancedButton
        AdaptiveCutPreset.COMPACT -> R.id.adaptiveCompactButton
    }

    private fun renderPresetButtonId(preset: RenderPreset): Int = when (preset) {
        RenderPreset.HD_720P -> R.id.exportQuality720Button
        RenderPreset.FULL_HD_1080P -> R.id.exportQuality1080Button
        RenderPreset.QHD_2K -> R.id.exportQuality2kButton
    }

    private fun scaleModeButtonId(scaleMode: ScaleMode): Int = when (scaleMode) {
        ScaleMode.FIT -> R.id.scaleFitButton
        ScaleMode.FILL -> R.id.scaleFillButton
    }

    private fun zoomModeButtonId(mode: ZoomMode): Int = when (mode) {
        ZoomMode.OFF,
        ZoomMode.IN -> R.id.zoomInButton
        ZoomMode.OUT -> R.id.zoomOutButton
        ZoomMode.ALTERNATE -> R.id.zoomAlternateButton
    }

    private fun speedButtonId(speed: Float): Int = when (speed) {
        0.5f -> R.id.speed05Button
        0.75f -> R.id.speed075Button
        1f -> R.id.speed10Button
        1.5f -> R.id.speed15Button
        2f -> R.id.speed20Button
        else -> R.id.speed125Button
    }

    private fun freezeDurationButtonId(durationMs: Long): Int = when (durationMs) {
        1_000L -> R.id.freeze1sButton
        3_000L -> R.id.freeze3sButton
        else -> R.id.freeze2sButton
    }

    private fun transitionModeButtonId(mode: TransitionMode): Int = when (mode) {
        TransitionMode.OFF,
        TransitionMode.FADE_IN_OUT -> R.id.transitionFadeBothButton
        TransitionMode.FADE_IN -> R.id.transitionFadeInButton
        TransitionMode.FADE_OUT -> R.id.transitionFadeOutButton
    }

    private fun transitionDurationButtonId(durationMs: Long): Int = when (durationMs) {
        500L -> R.id.transition05sButton
        1_500L -> R.id.transition15sButton
        else -> R.id.transition1sButton
    }

    private fun imageOverlayPositionButtonId(preset: ImageOverlayPositionPreset): Int =
        when (preset) {
            ImageOverlayPositionPreset.TOP_LEFT -> R.id.imageOverlayTopLeftButton
            ImageOverlayPositionPreset.TOP_RIGHT -> R.id.imageOverlayTopRightButton
            ImageOverlayPositionPreset.CENTER -> R.id.imageOverlayCenterButton
            ImageOverlayPositionPreset.BOTTOM_LEFT -> R.id.imageOverlayBottomLeftButton
            ImageOverlayPositionPreset.BOTTOM_RIGHT -> R.id.imageOverlayBottomRightButton
        }

    private fun aspectRatioLabel(aspectRatio: AspectRatioPreset): String = getString(
        when (aspectRatio) {
            AspectRatioPreset.ORIGINAL -> R.string.aspect_original
            AspectRatioPreset.PORTRAIT_9_16 -> R.string.aspect_portrait
            AspectRatioPreset.LANDSCAPE_16_9 -> R.string.aspect_landscape
            AspectRatioPreset.SQUARE_1_1 -> R.string.aspect_square
        },
    )

    private fun scaleModeDescription(scaleMode: ScaleMode): String = getString(
        when (scaleMode) {
            ScaleMode.FIT -> R.string.scale_fit_description
            ScaleMode.FILL -> R.string.scale_fill_description
        },
    )

    private fun zoomModeLabel(mode: ZoomMode): String = getString(
        when (mode) {
            ZoomMode.OFF,
            ZoomMode.IN -> R.string.zoom_mode_in
            ZoomMode.OUT -> R.string.zoom_mode_out
            ZoomMode.ALTERNATE -> R.string.zoom_mode_alternate
        },
    )

    private fun speedLabel(speed: Float): String = when (speed) {
        0.5f -> getString(R.string.speed_05)
        0.75f -> getString(R.string.speed_075)
        1f -> getString(R.string.speed_10)
        1.5f -> getString(R.string.speed_15)
        2f -> getString(R.string.speed_20)
        else -> getString(R.string.speed_125)
    }

    private fun freezeDurationLabel(durationMs: Long): String = getString(
        when (durationMs) {
            1_000L -> R.string.freeze_1s
            3_000L -> R.string.freeze_3s
            else -> R.string.freeze_2s
        },
    )

    private fun transitionModeLabel(mode: TransitionMode): String = getString(
        when (mode) {
            TransitionMode.OFF,
            TransitionMode.FADE_IN_OUT -> R.string.transition_mode_both
            TransitionMode.FADE_IN -> R.string.transition_mode_in
            TransitionMode.FADE_OUT -> R.string.transition_mode_out
        },
    )

    private fun transitionDurationLabel(durationMs: Long): String = getString(
        when (durationMs) {
            500L -> R.string.transition_05s
            1_500L -> R.string.transition_15s
            else -> R.string.transition_1s
        },
    )

    private fun currentPreviewSpeed(): Float =
        SpeedCompiler.compile(currentTransformSettings())?.multiplier ?: 1f

    private fun currentPreviewVolume(): Float {
        val compiledAudio = AudioCompiler.compile(currentAudioSettings())
        return when {
            compiledAudio == null -> AudioCompiler.UNITY_LINEAR_GAIN
            compiledAudio.removeAudio -> AudioCompiler.MIN_LINEAR_GAIN
            else -> compiledAudio.linearGain
        }
    }

    private fun audioPolicyButtonId(policy: AudioPolicy): Int = when (policy) {
        AudioPolicy.MUTE -> R.id.audioMuteButton
        AudioPolicy.REPLACE -> R.id.audioReplaceButton
        AudioPolicy.MIX -> R.id.audioMixButton
        else -> R.id.audioKeepOriginalButton
    }

    private fun refreshAudioPreview() {
        val info = activeMediaInfo
        val isSourcePreview = info != null && previewPath == info.workingFilePath
        if (isSourcePreview) {
            previewPlayer.volume = currentPreviewVolume()
        }
        if (!isReplacementPreviewActive()) {
            stopReplacementAudioPreview(clearMedia = !isSourcePreview)
            return
        }
        ensureReplacementAudioPreviewLoaded()
        syncReplacementAudioPreview(forceSeek = true)
    }

    private fun isReplacementPreviewActive(): Boolean {
        val info = activeMediaInfo ?: return false
        val asset = replacementAudioAsset ?: return false
        return !compositionPreviewActive &&
            audioEnabled &&
            audioPolicy in setOf(AudioPolicy.REPLACE, AudioPolicy.MIX) &&
            previewPath == info.workingFilePath &&
            File(asset.workingFilePath).isFile &&
            !renderCoordinator.currentState.isActiveRender()
    }

    private fun ensureReplacementAudioPreviewLoaded() {
        val asset = replacementAudioAsset ?: return
        if (replacementPreviewPath == asset.workingFilePath) {
            replacementAudioPlayer.volume = currentExternalPreviewVolume()
            return
        }
        replacementAudioSyncHandler.removeCallbacks(replacementAudioSyncRunnable)
        replacementAudioPlayer.stop()
        replacementAudioPlayer.clearMediaItems()
        replacementPreviewPath = asset.workingFilePath
        replacementAudioPlayer.volume = currentExternalPreviewVolume()
        replacementAudioPlayer.setMediaItem(
            MediaItem.fromUri(File(asset.workingFilePath).toURI().toString()),
        )
        replacementAudioPlayer.playWhenReady = false
        replacementAudioPlayer.prepare()
    }

    private fun syncReplacementAudioPreview(forceSeek: Boolean) {
        if (!::replacementAudioPlayer.isInitialized || !isReplacementPreviewActive()) {
            pauseReplacementAudioPreview()
            return
        }
        val asset = replacementAudioAsset ?: return
        ensureReplacementAudioPreviewLoaded()
        val durationMs = asset.durationMs.coerceAtLeast(1L)
        val expectedPositionMs = ReplacementAudioTimeline.loopPositionMs(
            replacementOutputPositionMs(),
            durationMs,
        )
        val actualPositionMs = replacementAudioPlayer.currentPosition.coerceAtLeast(0L)
        val directDriftMs = abs(actualPositionMs - expectedPositionMs)
        val loopDriftMs = min(
            abs((actualPositionMs + durationMs) - expectedPositionMs),
            abs(actualPositionMs - (expectedPositionMs + durationMs)),
        )
        if (forceSeek || min(directDriftMs, loopDriftMs) > REPLACEMENT_SYNC_TOLERANCE_MS) {
            replacementAudioPlayer.seekTo(expectedPositionMs)
        }
        replacementAudioPlayer.volume = currentExternalPreviewVolume()
        val shouldPlay = freezePreviewActive || previewPlayer.isPlaying
        replacementAudioPlayer.playWhenReady = shouldPlay
        replacementAudioSyncHandler.removeCallbacks(replacementAudioSyncRunnable)
        if (shouldPlay) {
            replacementAudioSyncHandler.postDelayed(
                replacementAudioSyncRunnable,
                REPLACEMENT_SYNC_POLL_MS,
            )
        }
    }

    private fun replacementOutputPositionMs(): Long {
        if (freezePreviewActive) {
            return (SystemClock.elapsedRealtime() - freezePreviewStartedAtElapsedMs)
                .coerceIn(0L, freezeDurationMs)
        }
        val speed = currentPreviewSpeed()
        val freezeOffsetMs = FreezeCompiler.compile(currentTransformSettings())?.durationMs ?: 0L
        if (adaptiveSequencePreviewActive) {
            return ReplacementAudioTimeline.sequencePositionMs(
                ranges = adaptiveDraftRanges,
                rangeIndex = previewPlayer.currentMediaItemIndex,
                itemPositionMs = previewPlayer.currentPosition,
                speed = speed,
                introFreezeMs = freezeOffsetMs,
            )
        }
        if (adaptivePreviewActive) {
            return ReplacementAudioTimeline.candidatePositionMs(
                ranges = adaptiveDraftRanges,
                rangeIndex = adaptiveCandidateIndex,
                sourcePositionMs = previewPlayer.currentPosition,
                speed = speed,
                introFreezeMs = freezeOffsetMs,
            )
        }
        val info = activeMediaInfo ?: return 0L
        return ReplacementAudioTimeline.sourcePositionMs(
            sourcePositionMs = previewPlayer.currentPosition,
            trimRange = currentTrimRange(info),
            speed = speed,
            introFreezeMs = freezeOffsetMs,
        )
    }

    private val replacementAudioSyncRunnable = object : Runnable {
        override fun run() {
            syncReplacementAudioPreview(forceSeek = false)
        }
    }

    private fun currentExternalPreviewVolume(): Float =
        AudioCompiler.compile(currentAudioSettings())?.replacementLinearGain
            ?: AudioCompiler.UNITY_LINEAR_GAIN

    private fun pauseReplacementAudioPreview() {
        if (!::replacementAudioPlayer.isInitialized) return
        replacementAudioSyncHandler.removeCallbacks(replacementAudioSyncRunnable)
        replacementAudioPlayer.pause()
    }

    private fun stopReplacementAudioPreview(clearMedia: Boolean) {
        if (!::replacementAudioPlayer.isInitialized) return
        replacementAudioSyncHandler.removeCallbacks(replacementAudioSyncRunnable)
        replacementAudioPlayer.pause()
        if (clearMedia) {
            replacementAudioPlayer.stop()
            replacementAudioPlayer.clearMediaItems()
            replacementPreviewPath = null
        }
    }

    private fun ViewGroup.setChildrenEnabled(enabled: Boolean) {
        for (index in 0 until childCount) {
            getChildAt(index).isEnabled = enabled
        }
    }

    private fun updateTrimSummary() {
        val info = activeMediaInfo ?: return
        val trim = currentTrimRange(info)
        editor.trimStartValue.text = MediaFormatters.duration(trim.startMs)
        editor.trimEndValue.text = MediaFormatters.duration(trim.endMs)
        editor.trimDurationValue.text = getString(
            R.string.trim_selected_duration,
            MediaFormatters.duration(trim.durationMs.coerceAtLeast(0L)),
        )
        val issue = EditPlanValidator.validate(currentEditPlan(selectedRenderPreset))
            .firstOrNull()
        val mixSourceMissing = audioEnabled && audioPolicy == AudioPolicy.MIX && !info.hasAudio
        editor.trimValidationMessage.isVisible = issue != null || mixSourceMissing
        editor.trimValidationMessage.text = when {
            mixSourceMissing -> getString(R.string.audio_mix_source_missing_error)
            issue != null -> validationMessage(issue)
            else -> ""
        }
        val idle = renderCoordinator.currentState as? RenderUiState.Idle
        if (idle != null) {
            editor.nextGateButton.isEnabled =
                idle.capability.available && issue == null && !mixSourceMissing
        }
        renderDurationFitAdvisor()
    }

    private fun currentTrimRange(info: MediaInfo): TrimRange {
        val values = editor.trimRangeSlider.values.sorted()
        val startMs = ((values.firstOrNull() ?: 0f) * 1_000f)
            .roundToLong()
            .coerceIn(0L, info.durationMs)
        val endMs = ((values.lastOrNull() ?: info.durationMs / 1_000f) * 1_000f)
            .roundToLong()
            .coerceIn(startMs, info.durationMs)
        return TrimRange(startMs, endMs)
    }

    private fun currentEditPlan(preset: RenderPreset): EditPlan {
        val info = checkNotNull(activeMediaInfo)
        val clipTransitions = if (::clipTransitionEditorController.isInitialized) {
            clipTransitionEditorController.currentSettings()
        } else {
            ClipTransitionSettings()
        }
        return EditPlan(
            sourcePath = info.workingFilePath,
            sourceDurationMs = info.durationMs,
            profile = when {
                adaptiveApplied -> EditProfile.ADAPTIVE
                transformEnabled || audioEnabled || overlayEnabled || clipTransitions.enabled ->
                    EditProfile.CUSTOM
                else -> EditProfile.NORMAL
            },
            trimRange = currentTrimRange(info),
            adaptiveCuts = AdaptiveCutSettings(
                enabled = adaptiveApplied,
                preset = adaptivePreset,
                reviewedRanges = adaptiveDraftRanges,
            ),
            transform = currentTransformSettings(),
            audio = currentAudioSettings(),
            overlays = currentOverlaySettings(),
            exportPreset = preset,
            clipTransitions = clipTransitions,
        )
    }

    private fun currentAudioSettings(): AudioSettings = AudioSettings(
        enabled = audioEnabled,
        policy = audioPolicy,
        volume = if (audioPolicy == AudioPolicy.MIX) mixSourceVolume else audioVolume,
        mixVolume = mixAddedVolume,
        replacement = replacementAudioAsset,
    )

    private fun currentOverlaySettings(): OverlaySettings = OverlaySettings(
        enabled = overlayEnabled,
        sourceSubtitleBlur = SourceSubtitleBlurSettings(
            enabled = sourceSubtitleBlurEnabled,
            rectangle = sourceSubtitleBlurRectangle,
            strength = sourceSubtitleBlurStrength,
            startMs = sourceSubtitleBlurStartMs,
            endMs = sourceSubtitleBlurEndMs,
        ),
        image = ImageOverlaySettings(
            enabled = imageOverlayEnabled,
            asset = imageOverlayAsset,
            centerX = imageOverlayCenterX,
            centerY = imageOverlayCenterY,
            widthFraction = imageOverlayWidthFraction,
            opacity = imageOverlayOpacity,
            startMs = imageOverlayStartMs,
            endMs = imageOverlayEndMs,
        ),
    )

    private fun renderNeedsAudioCapability(info: MediaInfo): Boolean =
        info.hasAudio ||
            (
                audioEnabled &&
                    audioPolicy in setOf(AudioPolicy.REPLACE, AudioPolicy.MIX) &&
                    replacementAudioAsset != null
                )

    private fun currentTransformSettings(): TransformSettings = TransformSettings(
        enabled = transformEnabled,
        aspectRatio = transformAspectRatio,
        scaleMode = transformScaleMode,
        crop = CropSettings(
            enabled = cropEnabled,
            rectangle = cropRectangle,
        ),
        mirrorEnabled = mirrorEnabled,
        color = ColorSettings(
            enabled = colorEnabled,
            brightness = colorBrightness,
            contrast = colorContrast,
            saturation = colorSaturation,
            temperature = colorTemperature,
        ),
        zoom = ZoomSettings(
            enabled = zoomEnabled,
            mode = zoomMode,
        ),
        speedEnabled = speedEnabled,
        speed = speedMultiplier,
        freeze = FreezeSettings(
            enabled = freezeEnabled,
            durationMs = freezeDurationMs,
        ),
        transition = TransitionSettings(
            enabled = transitionEnabled,
            mode = transitionMode,
            durationMs = transitionDurationMs,
        ),
    )

    private fun isCurrentTrimValid(): Boolean {
        val info = activeMediaInfo ?: return false
        if (audioEnabled && audioPolicy == AudioPolicy.MIX && !info.hasAudio) return false
        return EditPlanValidator.validate(currentEditPlan(selectedRenderPreset)).isEmpty()
    }

    private fun validationMessage(issue: EditPlanIssue): String = when (issue) {
        EditPlanIssue.TRIM_TOO_SHORT -> getString(R.string.trim_minimum_error)
        EditPlanIssue.TRIM_START_BEFORE_SOURCE,
        EditPlanIssue.TRIM_END_AFTER_SOURCE -> getString(R.string.trim_bounds_error)
        EditPlanIssue.CROP_RECTANGLE_INVALID -> getString(R.string.crop_rectangle_error)
        EditPlanIssue.COLOR_SETTINGS_INVALID -> getString(R.string.color_settings_error)
        EditPlanIssue.TRANSITION_DURATION_INVALID -> getString(R.string.transition_duration_error)
        EditPlanIssue.TRANSITION_TOO_LONG -> getString(R.string.transition_too_long_error)
        EditPlanIssue.ADAPTIVE_RANGES_MISSING -> getString(R.string.adaptive_ranges_missing_error)
        EditPlanIssue.ADAPTIVE_RANGES_INVALID -> getString(R.string.adaptive_ranges_invalid_error)
        EditPlanIssue.EXTERNAL_AUDIO_MISSING -> getString(
            if (audioPolicy == AudioPolicy.MIX) {
                R.string.audio_mix_missing_error
            } else {
                R.string.audio_replace_missing_error
            },
        )
        EditPlanIssue.REPLACEMENT_AUDIO_INVALID -> getString(R.string.audio_replace_invalid_error)
        EditPlanIssue.SOURCE_BLUR_RECTANGLE_INVALID -> getString(
            R.string.source_blur_rectangle_error,
        )
        EditPlanIssue.SOURCE_BLUR_STRENGTH_INVALID -> getString(
            R.string.source_blur_strength_error,
        )
        EditPlanIssue.SOURCE_BLUR_TIME_RANGE_INVALID -> getString(
            R.string.source_blur_time_error,
        )
        EditPlanIssue.IMAGE_OVERLAY_ASSET_INVALID -> getString(
            R.string.image_overlay_asset_error,
        )
        EditPlanIssue.IMAGE_OVERLAY_GEOMETRY_INVALID -> getString(
            R.string.image_overlay_geometry_error,
        )
        EditPlanIssue.IMAGE_OVERLAY_TIME_RANGE_INVALID -> getString(
            R.string.image_overlay_time_error,
        )
        else -> issue.description
    }

    private fun showRenderProgress(progress: Int?) {
        editor.renderProgressIndicator.isVisible = true
        editor.renderProgressIndicator.visibility = View.INVISIBLE
        editor.renderProgressIndicator.isIndeterminate = progress == null
        if (progress != null) {
            editor.renderProgressIndicator.progress = progress
        }
        editor.renderProgressIndicator.visibility = View.VISIBLE
    }

    private fun elapsedText(elapsedMs: Long): String {
        val totalSeconds = elapsedMs / 1_000L
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60L, totalSeconds % 60L)
    }

    private fun exactDurationText(durationMs: Long): String = String.format(
        Locale.US,
        "%s.%03d",
        MediaFormatters.duration(durationMs),
        durationMs.coerceAtLeast(0L) % 1_000L,
    )

    private fun RenderUiState.isActiveRender(): Boolean =
        this is RenderUiState.Preparing ||
            this is RenderUiState.Rendering ||
            this is RenderUiState.Finalizing

    private fun renderError(state: ImportUiState.Error) {
        editor.errorTitle.text = state.title
        editor.errorMessage.text = state.message
        editor.errorCode.text = getString(R.string.error_code, state.code.value)
        editor.retryButton.isVisible = state.recoverable &&
            (state.sourceUri != null || state.preparedMedia != null)
        editor.chooseAnotherErrorButton.isVisible = state.engineVersion
            ?.contains("/ FFmpeg ") == true
        editor.copyDiagnosticsButton.isVisible = true
    }

    private fun renderTechnicalDetailsVisibility() {
        editor.technicalDetailsGroup.isVisible = technicalDetailsExpanded
        editor.technicalDetailsButton.setText(
            if (technicalDetailsExpanded) {
                R.string.hide_technical_details
            } else {
                R.string.technical_details
            },
        )
    }

    private fun copyDiagnostics() {
        val state = importCoordinator.currentState as? ImportUiState.Error ?: return
        val text = buildString {
            appendLine("RecapFlowAI Phase 6F.2.6")
            appendLine("Code: ${state.code.value}")
            appendLine("Message: ${state.message}")
            state.diagnostics?.takeIf(String::isNotBlank)?.let {
                appendLine("FFmpeg: $it")
            }
            state.engineVersion?.takeIf(String::isNotBlank)?.let {
                appendLine("Engine: $it")
            }
        }.trim()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("RecapFlowAI diagnostics", text))
        Snackbar.make(binding.mainRoot, R.string.diagnostics_copied, Snackbar.LENGTH_SHORT)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_PREVIEW_SOURCE_ONLY, previewFallbackActive)
        outState.putString(KEY_MAIN_DESTINATION, selectedDestination.name)
        outState.putBoolean(KEY_TRANSFORM_ENABLED, transformEnabled)
        outState.putBoolean(KEY_TRANSFORM_DETAILS_VISIBLE, transformDetailsVisible)
        outState.putString(KEY_TRANSFORM_ASPECT_RATIO, transformAspectRatio.name)
        outState.putString(KEY_TRANSFORM_SCALE_MODE, transformScaleMode.name)
        outState.putBoolean(KEY_CROP_ENABLED, cropEnabled)
        outState.putFloat(KEY_CROP_LEFT, cropRectangle.left)
        outState.putFloat(KEY_CROP_TOP, cropRectangle.top)
        outState.putFloat(KEY_CROP_RIGHT, cropRectangle.right)
        outState.putFloat(KEY_CROP_BOTTOM, cropRectangle.bottom)
        outState.putBoolean(KEY_MIRROR_ENABLED, mirrorEnabled)
        outState.putBoolean(KEY_COLOR_ENABLED, colorEnabled)
        outState.putFloat(KEY_COLOR_BRIGHTNESS, colorBrightness)
        outState.putFloat(KEY_COLOR_CONTRAST, colorContrast)
        outState.putFloat(KEY_COLOR_SATURATION, colorSaturation)
        outState.putFloat(KEY_COLOR_TEMPERATURE, colorTemperature)
        outState.putBoolean(KEY_ZOOM_ENABLED, zoomEnabled)
        outState.putString(KEY_ZOOM_MODE, zoomMode.name)
        outState.putBoolean(KEY_SPEED_ENABLED, speedEnabled)
        outState.putFloat(KEY_SPEED_MULTIPLIER, speedMultiplier)
        outState.putBoolean(KEY_FREEZE_ENABLED, freezeEnabled)
        outState.putLong(KEY_FREEZE_DURATION_MS, freezeDurationMs)
        outState.putBoolean(KEY_TRANSITION_ENABLED, transitionEnabled)
        outState.putString(KEY_TRANSITION_MODE, transitionMode.name)
        outState.putLong(KEY_TRANSITION_DURATION_MS, transitionDurationMs)
        outState.putBoolean(KEY_AUDIO_ENABLED, audioEnabled)
        outState.putString(KEY_AUDIO_POLICY, audioPolicy.name)
        outState.putFloat(KEY_AUDIO_VOLUME, audioVolume)
        outState.putFloat(KEY_MIX_SOURCE_VOLUME, mixSourceVolume)
        outState.putFloat(KEY_MIX_ADDED_VOLUME, mixAddedVolume)
        outState.putBoolean(KEY_OVERLAY_ENABLED, overlayEnabled)
        outState.putBoolean(KEY_OVERLAY_DETAILS_VISIBLE, overlayDetailsVisible)
        outState.putBoolean(KEY_SOURCE_BLUR_ENABLED, sourceSubtitleBlurEnabled)
        outState.putFloat(KEY_SOURCE_BLUR_LEFT, sourceSubtitleBlurRectangle.left)
        outState.putFloat(KEY_SOURCE_BLUR_TOP, sourceSubtitleBlurRectangle.top)
        outState.putFloat(KEY_SOURCE_BLUR_RIGHT, sourceSubtitleBlurRectangle.right)
        outState.putFloat(KEY_SOURCE_BLUR_BOTTOM, sourceSubtitleBlurRectangle.bottom)
        outState.putFloat(KEY_SOURCE_BLUR_STRENGTH, sourceSubtitleBlurStrength)
        if (sourceSubtitleBlurRangeInitialized) {
            outState.putLong(KEY_SOURCE_BLUR_START_MS, sourceSubtitleBlurStartMs)
            outState.putLong(KEY_SOURCE_BLUR_END_MS, sourceSubtitleBlurEndMs)
            outState.putBoolean(
                KEY_SOURCE_BLUR_RANGE_FOLLOWS_TRIM,
                sourceSubtitleBlurRangeFollowsTrim,
            )
        }
        outState.putBoolean(KEY_IMAGE_OVERLAY_ENABLED, imageOverlayEnabled)
        outState.putFloat(KEY_IMAGE_OVERLAY_CENTER_X, imageOverlayCenterX)
        outState.putFloat(KEY_IMAGE_OVERLAY_CENTER_Y, imageOverlayCenterY)
        outState.putFloat(KEY_IMAGE_OVERLAY_WIDTH_FRACTION, imageOverlayWidthFraction)
        outState.putFloat(KEY_IMAGE_OVERLAY_OPACITY, imageOverlayOpacity)
        if (imageOverlayRangeInitialized) {
            outState.putLong(KEY_IMAGE_OVERLAY_START_MS, imageOverlayStartMs)
            outState.putLong(KEY_IMAGE_OVERLAY_END_MS, imageOverlayEndMs)
            outState.putBoolean(
                KEY_IMAGE_OVERLAY_RANGE_FOLLOWS_TRIM,
                imageOverlayRangeFollowsTrim,
            )
        }
        imageOverlayAsset?.let { asset ->
            outState.putString(KEY_IMAGE_OVERLAY_PATH, asset.workingFilePath)
            outState.putString(KEY_IMAGE_OVERLAY_NAME, asset.displayName)
            outState.putString(KEY_IMAGE_OVERLAY_MIME_TYPE, asset.mimeType)
            outState.putInt(KEY_IMAGE_OVERLAY_PIXEL_WIDTH, asset.pixelWidth)
            outState.putInt(KEY_IMAGE_OVERLAY_PIXEL_HEIGHT, asset.pixelHeight)
            outState.putLong(KEY_IMAGE_OVERLAY_SIZE_BYTES, asset.fileSizeBytes)
        }
        replacementAudioAsset?.let { asset ->
            outState.putString(KEY_REPLACEMENT_AUDIO_PATH, asset.workingFilePath)
            outState.putString(KEY_REPLACEMENT_AUDIO_NAME, asset.displayName)
            outState.putLong(KEY_REPLACEMENT_AUDIO_DURATION_MS, asset.durationMs)
            outState.putLong(KEY_REPLACEMENT_AUDIO_SIZE_BYTES, asset.fileSizeBytes)
        }
        outState.putString(KEY_ADAPTIVE_PRESET, adaptivePreset.name)
        outState.putLongArray(
            KEY_ADAPTIVE_RANGE_STARTS,
            adaptiveDraftRanges.map { it.startMs }.toLongArray(),
        )
        outState.putLongArray(
            KEY_ADAPTIVE_RANGE_ENDS,
            adaptiveDraftRanges.map { it.endMs }.toLongArray(),
        )
        outState.putBoolean(KEY_ADAPTIVE_APPLIED, adaptiveApplied)
        outState.putInt(KEY_ADAPTIVE_CANDIDATE_INDEX, adaptiveCandidateIndex)
        outState.putString(KEY_REVIEW_EDITOR_TAB, selectedReviewEditorTab.name)
        outState.putString(KEY_RENDER_PRESET, selectedRenderPreset.name)
        outState.putFloat(KEY_PREVIEW_OVERLAY_SCALE, previewOverlayScale)
        outState.putFloat(KEY_PREVIEW_OVERLAY_CENTER_X, previewOverlayCenterXFraction)
        if (previewOverlayCenterYFraction != PREVIEW_POSITION_UNSET) {
            outState.putFloat(KEY_PREVIEW_OVERLAY_CENTER_Y, previewOverlayCenterYFraction)
        }
        activeMediaInfo?.let { info ->
            val trim = currentTrimRange(info)
            outState.putLong(KEY_TRIM_START_MS, trim.startMs)
            outState.putLong(KEY_TRIM_END_MS, trim.endMs)
        }
        when (val state = importCoordinator.currentState) {
            is ImportUiState.Ready -> {
                outState.putString(KEY_SOURCE_URI, state.mediaInfo.sourceUri)
                outState.putString(KEY_WORKING_PATH, state.mediaInfo.workingFilePath)
                outState.putString(KEY_DISPLAY_NAME, state.mediaInfo.displayName)
            }
            is ImportUiState.Probing -> {
                outState.putString(KEY_SOURCE_URI, state.preparedMedia.sourceUri)
                outState.putString(KEY_WORKING_PATH, state.preparedMedia.workingFilePath)
                outState.putString(KEY_DISPLAY_NAME, state.preparedMedia.displayName)
            }
            is ImportUiState.Preparing -> {
                outState.putString(KEY_SOURCE_URI, state.sourceUri)
                outState.putString(KEY_DISPLAY_NAME, state.displayName)
            }
            is ImportUiState.Error -> {
                val prepared = state.preparedMedia
                outState.putString(KEY_SOURCE_URI, prepared?.sourceUri ?: state.sourceUri)
                outState.putString(KEY_WORKING_PATH, prepared?.workingFilePath)
                outState.putString(KEY_DISPLAY_NAME, prepared?.displayName)
            }
            else -> Unit
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDeviceProfile()
        if (sourceBlurPreviewDirty && !previewFallbackActive) {
            scheduleSourceBlurPreviewUpdate("resume pending source blur")
        }
        if (activePreviewPlaybackState() == Player.STATE_BUFFERING) {
            schedulePreviewReadyTimeout()
        }
    }

    override fun onStop() {
        editorPreferencesHandler.removeCallbacks(persistEditorPreferences)
        if (::editorPreferencesStore.isInitialized && _binding != null) {
            editorPreferencesStore.saveLastSession(currentEditorPreferencesSnapshot())
        }
        cancelSourceBlurPreviewUpdate(clearDirty = false)
        sourceBlurPreviewHandler.removeCallbacks(settlePausedPreviewFrameRefresh)
        pausedPreviewRefreshAnchorMs = null
        cancelPreviewReadyTimeout()
        cancelFreezePreview()
        cancelAdaptivePreview()
        clipTransitionPreviewHandler.removeCallbacks(clipTransitionPreviewCompletion)
        activePreviewPause()
        pauseReplacementAudioPreview()
        super.onStop()
    }

    override fun onDestroy() {
        realtimeSourceBlurState.update(null)
        realtimeImageOverlayState.update(null)
        freezePreviewHandler.removeCallbacksAndMessages(null)
        adaptivePreviewHandler.removeCallbacksAndMessages(null)
        replacementAudioSyncHandler.removeCallbacksAndMessages(null)
        sourceBlurPreviewHandler.removeCallbacksAndMessages(null)
        pausedPreviewRefreshAnchorMs = null
        previewRecoveryHandler.removeCallbacksAndMessages(null)
        editorPreferencesHandler.removeCallbacksAndMessages(null)
        clipTransitionPreviewHandler.removeCallbacksAndMessages(null)
        sourceBlurPreviewUpdatePosted = false
        cancelSourceBlurGestureCommit(resetGuide = true)
        releaseCompositionPreview(attachExoPlayer = false, reason = "activity destroy")
        editor.videoPreview.player = null
        previewPlayer.removeListener(previewListener)
        previewPlayer.release()
        replacementAudioPlayer.removeListener(replacementAudioPreviewListener)
        replacementAudioPlayer.release()
        renderCoordinator.close()
        publicExportCoordinator.close()
        replacementAudioImportCoordinator.close()
        imageOverlayImportCoordinator.close()
        importCoordinator.close()
        _binding = null
        super.onDestroy()
    }

    private fun Bundle?.toResumeRequest(): ImportResumeRequest? {
        this ?: return null
        val sourceUri = getString(KEY_SOURCE_URI)
        if (sourceUri.isNullOrBlank()) {
            return null
        }
        return ImportResumeRequest(
            sourceUri = sourceUri,
            workingFilePath = getString(KEY_WORKING_PATH)
                ?.takeIf { File(it).isFile },
            displayName = getString(KEY_DISPLAY_NAME),
        )
    }

    private var View.isVisible: Boolean
        get() = visibility == View.VISIBLE
        set(value) {
            visibility = if (value) View.VISIBLE else View.GONE
        }

    companion object {
        private const val KEY_SOURCE_URI = "recapflow.sourceUri"
        private const val KEY_PREVIEW_SOURCE_ONLY = "recapflow.previewSourceOnly"
        private const val KEY_WORKING_PATH = "recapflow.workingPath"
        private const val KEY_DISPLAY_NAME = "recapflow.displayName"
        private const val KEY_TRIM_START_MS = "recapflow.trimStartMs"
        private const val KEY_TRIM_END_MS = "recapflow.trimEndMs"
        private const val KEY_TRANSFORM_ENABLED = "recapflow.transform.enabled"
        private const val KEY_TRANSFORM_DETAILS_VISIBLE = "recapflow.transform.detailsVisible"
        private const val KEY_TRANSFORM_ASPECT_RATIO = "recapflow.transform.aspectRatio"
        private const val KEY_TRANSFORM_SCALE_MODE = "recapflow.transform.scaleMode"
        private const val KEY_CROP_ENABLED = "recapflow.transform.crop.enabled"
        private const val KEY_CROP_LEFT = "recapflow.transform.crop.left"
        private const val KEY_CROP_TOP = "recapflow.transform.crop.top"
        private const val KEY_CROP_RIGHT = "recapflow.transform.crop.right"
        private const val KEY_CROP_BOTTOM = "recapflow.transform.crop.bottom"
        private const val KEY_MIRROR_ENABLED = "recapflow.transform.mirror.enabled"
        private const val KEY_COLOR_ENABLED = "recapflow.transform.color.enabled"
        private const val KEY_COLOR_BRIGHTNESS = "recapflow.transform.color.brightness"
        private const val KEY_COLOR_CONTRAST = "recapflow.transform.color.contrast"
        private const val KEY_COLOR_SATURATION = "recapflow.transform.color.saturation"
        private const val KEY_COLOR_TEMPERATURE = "recapflow.transform.color.temperature"
        private const val KEY_ZOOM_ENABLED = "recapflow.transform.zoom.enabled"
        private const val KEY_ZOOM_MODE = "recapflow.transform.zoom.mode"
        private const val KEY_SPEED_ENABLED = "recapflow.transform.speed.enabled"
        private const val KEY_SPEED_MULTIPLIER = "recapflow.transform.speed.multiplier"
        private const val KEY_FREEZE_ENABLED = "recapflow.transform.freeze.enabled"
        private const val KEY_FREEZE_DURATION_MS = "recapflow.transform.freeze.durationMs"
        private const val KEY_TRANSITION_ENABLED = "recapflow.transform.transition.enabled"
        private const val KEY_TRANSITION_MODE = "recapflow.transform.transition.mode"
        private const val KEY_TRANSITION_DURATION_MS = "recapflow.transform.transition.durationMs"
        private const val KEY_AUDIO_ENABLED = "recapflow.audio.enabled"
        private const val KEY_AUDIO_POLICY = "recapflow.audio.policy"
        private const val KEY_AUDIO_VOLUME = "recapflow.audio.volume"
        private const val KEY_MIX_SOURCE_VOLUME = "recapflow.audio.mix.sourceVolume"
        private const val KEY_MIX_ADDED_VOLUME = "recapflow.audio.mix.addedVolume"
        private const val KEY_OVERLAY_ENABLED = "recapflow.overlay.enabled"
        private const val KEY_OVERLAY_DETAILS_VISIBLE = "recapflow.overlay.detailsVisible"
        private const val KEY_SOURCE_BLUR_ENABLED = "recapflow.overlay.sourceBlur.enabled"
        private const val KEY_SOURCE_BLUR_LEFT = "recapflow.overlay.sourceBlur.left"
        private const val KEY_SOURCE_BLUR_TOP = "recapflow.overlay.sourceBlur.top"
        private const val KEY_SOURCE_BLUR_RIGHT = "recapflow.overlay.sourceBlur.right"
        private const val KEY_SOURCE_BLUR_BOTTOM = "recapflow.overlay.sourceBlur.bottom"
        private const val KEY_SOURCE_BLUR_STRENGTH = "recapflow.overlay.sourceBlur.strength"
        private const val KEY_SOURCE_BLUR_START_MS = "recapflow.overlay.sourceBlur.startMs"
        private const val KEY_SOURCE_BLUR_END_MS = "recapflow.overlay.sourceBlur.endMs"
        private const val KEY_SOURCE_BLUR_RANGE_FOLLOWS_TRIM = "recapflow.overlay.blur.rangeFollowsTrim"
        private const val KEY_IMAGE_OVERLAY_ENABLED = "recapflow.overlay.image.enabled"
        private const val KEY_IMAGE_OVERLAY_CENTER_X = "recapflow.overlay.image.centerX"
        private const val KEY_IMAGE_OVERLAY_CENTER_Y = "recapflow.overlay.image.centerY"
        private const val KEY_IMAGE_OVERLAY_WIDTH_FRACTION = "recapflow.overlay.image.width"
        private const val KEY_IMAGE_OVERLAY_OPACITY = "recapflow.overlay.image.opacity"
        private const val KEY_IMAGE_OVERLAY_START_MS = "recapflow.overlay.image.startMs"
        private const val KEY_IMAGE_OVERLAY_END_MS = "recapflow.overlay.image.endMs"
        private const val KEY_IMAGE_OVERLAY_RANGE_FOLLOWS_TRIM = "recapflow.overlay.image.rangeFollowsTrim"
        private const val KEY_IMAGE_OVERLAY_PATH = "recapflow.overlay.image.path"
        private const val KEY_IMAGE_OVERLAY_NAME = "recapflow.overlay.image.name"
        private const val KEY_IMAGE_OVERLAY_MIME_TYPE = "recapflow.overlay.image.mimeType"
        private const val KEY_IMAGE_OVERLAY_PIXEL_WIDTH = "recapflow.overlay.image.pixelWidth"
        private const val KEY_IMAGE_OVERLAY_PIXEL_HEIGHT = "recapflow.overlay.image.pixelHeight"
        private const val KEY_IMAGE_OVERLAY_SIZE_BYTES = "recapflow.overlay.image.sizeBytes"
        private const val KEY_REPLACEMENT_AUDIO_PATH = "recapflow.audio.replacement.path"
        private const val KEY_REPLACEMENT_AUDIO_NAME = "recapflow.audio.replacement.name"
        private const val KEY_REPLACEMENT_AUDIO_DURATION_MS =
            "recapflow.audio.replacement.durationMs"
        private const val KEY_REPLACEMENT_AUDIO_SIZE_BYTES =
            "recapflow.audio.replacement.sizeBytes"
        private const val KEY_ADAPTIVE_PRESET = "recapflow.adaptive.preset"
        private const val KEY_ADAPTIVE_RANGE_STARTS = "recapflow.adaptive.rangeStarts"
        private const val KEY_ADAPTIVE_RANGE_ENDS = "recapflow.adaptive.rangeEnds"
        private const val KEY_ADAPTIVE_APPLIED = "recapflow.adaptive.applied"
        private const val KEY_ADAPTIVE_CANDIDATE_INDEX = "recapflow.adaptive.candidateIndex"
        private const val KEY_REVIEW_EDITOR_TAB = "recapflow.reviewEditor.tab"
        private const val KEY_RENDER_PRESET = "recapflow.export.renderPreset"
        private const val KEY_MAIN_DESTINATION = "recapflow.mainDestination"
        private const val KEY_PREVIEW_OVERLAY_SCALE = "recapflow.previewOverlay.scale"
        private const val KEY_PREVIEW_OVERLAY_CENTER_X = "recapflow.previewOverlay.centerX"
        private const val KEY_PREVIEW_OVERLAY_CENTER_Y = "recapflow.previewOverlay.centerY"
        private const val PREVIEW_FRAME_MS = 500L
        private const val DEFAULT_SPEED_MULTIPLIER = 1.25f
        private const val DEFAULT_PREVIEW_OVERLAY_SCALE = 1f
        private const val DEFAULT_PREVIEW_CENTER_X_FRACTION = 0.5f
        private const val MIN_PREVIEW_OVERLAY_SCALE = 0.55f
        private const val PREVIEW_POSITION_UNSET = -1f
        private const val ADAPTIVE_PREVIEW_POLL_MS = 100L
        private const val CLIP_TRANSITION_PREVIEW_LEAD_MS = 800L
        private const val CLIP_TRANSITION_PREVIEW_WINDOW_MS = 2_500L
        private const val REPLACEMENT_SYNC_POLL_MS = 250L
        private const val REPLACEMENT_SYNC_TOLERANCE_MS = 120L
        private const val SOURCE_BLUR_PREVIEW_UPDATE_MS = 140L
        private const val PAUSED_PREVIEW_REFRESH_SETTLE_MS = 360L
        private const val PREVIEW_READY_TIMEOUT_MS = 10_000L
        private const val EDITOR_PREFERENCES_SAVE_DELAY_MS = 350L
        private const val SOURCE_BLUR_DIRECT_TOUCH_ENABLED = false
        private const val SOURCE_BLUR_PREVIEW_REASON_DEFAULT = "overlay controls"
        private const val TAG_PREVIEW = "RecapFlowPreview"
        private const val TAG_SOURCE_BLUR = "RecapFlowBlur"
        private const val ADAPTIVE_SEQUENCE_MEDIA_ID_PREFIX = "recapflow-adaptive-range-"
    }

    private enum class MainDestination(
        val menuItemId: Int,
        val titleRes: Int,
    ) {
        HOME(R.id.navigationHome, R.string.app_name),
        EDITOR(R.id.navigationEditor, R.string.nav_editor),
        SETTINGS(R.id.navigationSettings, R.string.nav_settings),
    }

    private enum class ReviewEditorTab {
        CLIPS,
        TRANSFORM,
        AUDIO,
        OVERLAY,
        EXPORT,
    }
}
