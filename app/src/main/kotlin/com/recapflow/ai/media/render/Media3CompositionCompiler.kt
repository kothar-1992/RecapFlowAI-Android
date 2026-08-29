package com.recapflow.ai.media.render

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.recapflow.ai.BuildConfig
import com.recapflow.ai.media.MediaInfo
import com.recapflow.ai.media.edit.AudioCompiler
import com.recapflow.ai.media.edit.EditPlan
import com.recapflow.ai.media.edit.OverlayCompiler
import com.recapflow.ai.media.edit.OverlaySettings
import com.recapflow.ai.media.edit.ReplacementAudioAsset
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TrimRange
import com.recapflow.ai.media.edit.ZoomMode
import java.io.File

@UnstableApi
data class CompiledMedia3Composition(
    val composition: Composition,
    val plan: Media3CompositionPlan,
    val requiresMultipleInputVideoGraph: Boolean = false,
)

/** Builds the single authoritative Media3 Composition used by preview and final export. */
@UnstableApi
object Media3CompositionCompiler {
    const val PREVIEW_FRAME_RATE = 30

    fun compile(
        mediaInfo: MediaInfo,
        editPlan: EditPlan,
        input: File,
        freezeFrame: File?,
        plan: Media3CompositionPlan = Media3CompositionPlanCompiler.compile(mediaInfo, editPlan),
    ): CompiledMedia3Composition = compileInternal(
        mediaInfo = mediaInfo,
        editPlan = editPlan,
        input = input,
        freezeFrame = freezeFrame,
        plan = plan,
        forCompositionPreview = false,
    )

    /**
     * Builds the same shared composition topology for CompositionPlayer preview. Unlike Transformer,
     * CompositionPlayer requires every encoded [EditedMediaItem] to expose the original source
     * duration before clipping. Freeze stays on the proven ExoPlayer simulation in Phase 6F.2.7.
     */
    fun compileForPreview(
        mediaInfo: MediaInfo,
        editPlan: EditPlan,
        input: File,
        plan: Media3CompositionPlan = Media3CompositionPlanCompiler.compile(mediaInfo, editPlan),
    ): CompiledMedia3Composition {
        require(plan.freeze == null) {
            "Phase 6F.2.7 CompositionPlayer preview delegates Intro Freeze to ExoPlayer"
        }
        return compileInternal(
            mediaInfo = mediaInfo,
            editPlan = editPlan,
            input = input,
            freezeFrame = null,
            plan = plan,
            forCompositionPreview = true,
        )
    }

    private fun compileInternal(
        mediaInfo: MediaInfo,
        editPlan: EditPlan,
        input: File,
        freezeFrame: File?,
        plan: Media3CompositionPlan,
        forCompositionPreview: Boolean,
    ): CompiledMedia3Composition {
        require(input.isFile) { "Composition source is unavailable: ${input.absolutePath}" }
        require((plan.freeze == null) == (freezeFrame == null)) {
            "Freeze-frame asset must match the compiled composition plan"
        }

        val crossfadeActive = plan.clipTransitions.isNotEmpty()
        Media3ClipTransitionRuntimePolicy.requireSupported(
            plan = plan,
            runtimeSpikeEnabled = BuildConfig.ENABLE_CROSSFADE_RUNTIME_SPIKE,
        )

        val targetFrameRate = if (forCompositionPreview) {
            PREVIEW_FRAME_RATE
        } else {
            ExportFrameRatePolicy.forSource(mediaInfo.frameRate)
        }

        return if (crossfadeActive) {
            compileCrossfadeComposition(
                mediaInfo = mediaInfo,
                editPlan = editPlan,
                input = input,
                freezeFrame = freezeFrame,
                plan = plan,
                forCompositionPreview = forCompositionPreview,
                targetFrameRate = targetFrameRate,
            )
        } else {
            compileSequentialComposition(
                mediaInfo = mediaInfo,
                editPlan = editPlan,
                input = input,
                freezeFrame = freezeFrame,
                plan = plan,
                forCompositionPreview = forCompositionPreview,
                targetFrameRate = targetFrameRate,
            )
        }
    }

    private fun compileSequentialComposition(
        mediaInfo: MediaInfo,
        editPlan: EditPlan,
        input: File,
        freezeFrame: File?,
        plan: Media3CompositionPlan,
        forCompositionPreview: Boolean,
        targetFrameRate: Int,
    ): CompiledMedia3Composition {
        val videoSequence = EditedMediaItemSequence.Builder().apply {
            var compositionOffsetUs = 0L
            if (plan.freeze != null) {
                addItem(
                    buildFreezeItem(
                        editPlan = editPlan,
                        freezeFrame = checkNotNull(freezeFrame),
                        freeze = plan.freeze,
                        targetFrameRate = targetFrameRate,
                    ),
                )
                compositionOffsetUs += plan.freeze.durationMs * 1_000L
            }
            plan.selectedRanges.forEach { range ->
                addItem(
                    buildEditedVideoItem(
                        mediaInfo = mediaInfo,
                        editPlan = editPlan,
                        input = input,
                        range = range,
                        plan = plan,
                        compositionOffsetUs = compositionOffsetUs,
                        forCompositionPreview = forCompositionPreview,
                        targetFrameRate = targetFrameRate,
                        crossfadeSlot = null,
                    ),
                )
                compositionOffsetUs += CompositionOverlayTimelinePolicy.presentationDurationUs(
                    editPlan.transform,
                    range,
                )
            }
            if (plan.forceSourceAudioTrack) {
                experimentalSetForceAudioTrack(true)
            }
        }.build()

        val sequences = buildList {
            add(videoSequence)
            plan.replacementAudio?.let { replacement ->
                add(
                    buildReplacementAudioSequence(
                        replacement = replacement,
                        linearGain = plan.replacementLinearGain,
                        normalizeForMix = plan.mixesSourceAudio,
                    ),
                )
            }
        }

        return CompiledMedia3Composition(
            composition = Composition.Builder(sequences).build(),
            plan = plan,
            requiresMultipleInputVideoGraph = false,
        )
    }

    private fun compileCrossfadeComposition(
        mediaInfo: MediaInfo,
        editPlan: EditPlan,
        input: File,
        freezeFrame: File?,
        plan: Media3CompositionPlan,
        forCompositionPreview: Boolean,
        targetFrameRate: Int,
    ): CompiledMedia3Composition {
        check(BuildConfig.ENABLE_CROSSFADE_RUNTIME_SPIKE) {
            "Crossfade runtime spike must be explicitly enabled"
        }
        val topology = Media3CrossfadeTopologyCompiler.compile(plan, editPlan)
        require(topology.laneCount == 2) {
            "Reviewed Crossfade topology must expose exactly two video lanes"
        }

        val sequences = buildList {
            add(
                buildCrossfadeLaneSequence(
                    lane = 0,
                    topology = topology,
                    mediaInfo = mediaInfo,
                    editPlan = editPlan,
                    input = input,
                    freezeFrame = freezeFrame,
                    plan = plan,
                    forCompositionPreview = forCompositionPreview,
                    targetFrameRate = targetFrameRate,
                ),
            )
            add(
                buildCrossfadeLaneSequence(
                    lane = 1,
                    topology = topology,
                    mediaInfo = mediaInfo,
                    editPlan = editPlan,
                    input = input,
                    freezeFrame = null,
                    plan = plan,
                    forCompositionPreview = forCompositionPreview,
                    targetFrameRate = targetFrameRate,
                ),
            )
            plan.replacementAudio?.let { replacement ->
                add(
                    buildReplacementAudioSequence(
                        replacement = replacement,
                        linearGain = plan.replacementLinearGain,
                        normalizeForMix = plan.mixesSourceAudio,
                    ),
                )
            }
        }

        val composition = Composition.Builder(sequences)
            .setVideoCompositorSettings(Media3CrossfadeVideoCompositorSettings(topology))
            .build()

        return CompiledMedia3Composition(
            composition = composition,
            plan = plan,
            requiresMultipleInputVideoGraph = true,
        )
    }

    private fun buildCrossfadeLaneSequence(
        lane: Int,
        topology: Media3CrossfadeTopology,
        mediaInfo: MediaInfo,
        editPlan: EditPlan,
        input: File,
        freezeFrame: File?,
        plan: Media3CompositionPlan,
        forCompositionPreview: Boolean,
        targetFrameRate: Int,
    ): EditedMediaItemSequence {
        val trackTypes = buildSet {
            add(C.TRACK_TYPE_VIDEO)
            if (mediaInfo.hasAudio && !plan.removeSourceAudio) add(C.TRACK_TYPE_AUDIO)
        }
        val builder = EditedMediaItemSequence.Builder(trackTypes)
        var cursorUs = 0L

        if (lane == 0 && plan.freeze != null) {
            builder.addItem(
                buildFreezeItem(
                    editPlan = editPlan,
                    freezeFrame = checkNotNull(freezeFrame),
                    freeze = plan.freeze,
                    targetFrameRate = targetFrameRate,
                ),
            )
            cursorUs = plan.freeze.durationMs * 1_000L
        }

        topology.slotsForLane(lane).forEach { slot ->
            val gapUs = slot.presentationStartUs - cursorUs
            require(gapUs >= 0L) {
                "Crossfade lane $lane has negative gap before clip ${slot.rangeIndex}"
            }
            if (gapUs > 0L) builder.addGap(gapUs)
            builder.addItem(
                buildEditedVideoItem(
                    mediaInfo = mediaInfo,
                    editPlan = editPlan,
                    input = input,
                    range = slot.sourceRange,
                    plan = plan,
                    compositionOffsetUs = slot.presentationStartUs,
                    forCompositionPreview = forCompositionPreview,
                    targetFrameRate = targetFrameRate,
                    crossfadeSlot = slot,
                ),
            )
            cursorUs = slot.presentationEndUs
        }

        return builder.build()
    }

    private fun buildEditedVideoItem(
        mediaInfo: MediaInfo,
        editPlan: EditPlan,
        input: File,
        range: TrimRange,
        plan: Media3CompositionPlan,
        compositionOffsetUs: Long,
        forCompositionPreview: Boolean,
        targetFrameRate: Int,
        crossfadeSlot: Media3CrossfadeClipSlot?,
    ): EditedMediaItem {
        val speedEffects = if (forCompositionPreview) {
            TransformSpeedEffectsFactory.forCompositionPreview(
                settings = editPlan.transform,
                hasAudio = mediaInfo.hasAudio && !plan.removeSourceAudio,
            )
        } else {
            TransformSpeedEffectsFactory.forRender(
                settings = editPlan.transform,
                hasAudio = mediaInfo.hasAudio && !plan.removeSourceAudio,
            )
        }
        val audioProcessors = buildList<AudioProcessor> {
            speedEffects?.audioProcessor?.let(::add)
            if (!plan.removeSourceAudio && plan.sourceLinearGain != AudioCompiler.UNITY_LINEAR_GAIN) {
                add(
                    if (plan.mixesSourceAudio) {
                        StereoPcmMixAudioProcessor(plan.sourceLinearGain)
                    } else {
                        PcmVolumeAudioProcessor(plan.sourceLinearGain)
                    },
                )
            } else if (!plan.removeSourceAudio && plan.mixesSourceAudio) {
                add(StereoPcmMixAudioProcessor(plan.sourceLinearGain))
            }
            if (!plan.removeSourceAudio && mediaInfo.hasAudio && crossfadeSlot != null) {
                add(CrossfadePcmAudioProcessor(crossfadeSlot))
            }
        }

        val localOverlays = OverlayCompiler.projectToRange(editPlan.overlays, range)
        val sourceTimeOffsetUs = CompositionOverlayTimelinePolicy.localEffectTimeOffsetUs(
            compositionOffsetUs,
        )
        val videoEffects = if (forCompositionPreview) {
            TransformVideoEffects.forCompositionPreview(
                settings = editPlan.transform,
                preset = RenderPreset.HD_720P,
                targetFrameRate = targetFrameRate.toFloat(),
                sourceDurationMs = range.durationMs,
                speedEffect = speedEffects?.videoEffect,
                overlays = localOverlays,
                timelineOffsetUs = compositionOffsetUs,
                sourceTimeOffsetUs = sourceTimeOffsetUs,
            )
        } else {
            TransformVideoEffects.forRender(
                settings = editPlan.transform,
                preset = editPlan.exportPreset,
                targetFrameRate = targetFrameRate.toFloat(),
                sourceDurationMs = range.durationMs,
                speedEffect = speedEffects?.videoEffect,
                overlays = localOverlays,
                sourceTimeOffsetUs = sourceTimeOffsetUs,
            )
        }
        val mediaItem = MediaItem.Builder()
            .setUri(input.toURI().toString())
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(range.startMs)
                    .setEndPositionMs(range.endMs)
                    .build(),
            )
            .build()
        return EditedMediaItem.Builder(mediaItem)
            .apply {
                if (forCompositionPreview) {
                    setDurationUs(mediaInfo.durationMs * 1_000L)
                }
            }
            .setRemoveAudio(plan.removeSourceAudio)
            .setEffects(Effects(audioProcessors, videoEffects))
            .build()
    }

    private fun buildFreezeItem(
        editPlan: EditPlan,
        freezeFrame: File,
        freeze: Media3FreezePlan,
        targetFrameRate: Int,
    ): EditedMediaItem = EditedMediaItem.Builder(
        MediaItem.Builder()
            .setUri(freezeFrame.toURI().toString())
            .setImageDurationMs(freeze.durationMs)
            .build(),
    )
        .setDurationUs(freeze.durationMs * 1_000L)
        .setFrameRate(targetFrameRate)
        .setEffects(
            Effects(
                emptyList(),
                TransformVideoEffects.forRender(
                    settings = frozenVisualSettings(editPlan.transform),
                    preset = editPlan.exportPreset,
                    targetFrameRate = targetFrameRate.toFloat(),
                    sourceDurationMs = freeze.durationMs,
                    overlays = editPlan.overlays.takeIf {
                        OverlayCompiler.hasOperationActiveAt(it, freeze.sourceFrameTimeMs)
                    } ?: OverlaySettings(),
                    fixedSourceTimeUs = freeze.sourceFrameTimeMs * 1_000L,
                ),
            ),
        )
        .build()

    private fun buildReplacementAudioSequence(
        replacement: ReplacementAudioAsset,
        linearGain: Float,
        normalizeForMix: Boolean,
    ): EditedMediaItemSequence {
        val audioProcessors = when {
            normalizeForMix -> listOf(StereoPcmMixAudioProcessor(linearGain))
            linearGain == AudioCompiler.UNITY_LINEAR_GAIN -> emptyList()
            else -> listOf(PcmVolumeAudioProcessor(linearGain))
        }
        val item = EditedMediaItem.Builder(
            MediaItem.fromUri(File(replacement.workingFilePath).toURI().toString()),
        )
            .setDurationUs(replacement.durationMs * 1_000L)
            .setRemoveVideo(true)
            .setEffects(Effects(audioProcessors, emptyList()))
            .build()
        return EditedMediaItemSequence.Builder()
            .addItem(item)
            .setIsLooping(true)
            .build()
    }

    private fun frozenVisualSettings(settings: TransformSettings): TransformSettings =
        settings.copy(
            speedEnabled = false,
            freeze = settings.freeze.copy(enabled = false),
            transition = settings.transition.copy(enabled = false),
            zoom = if (settings.zoom.mode == ZoomMode.ALTERNATE) {
                settings.zoom.copy(enabled = false)
            } else {
                settings.zoom
            },
        )
}
