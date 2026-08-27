package com.recapflow.ai.media.render

import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
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
)

/** Builds the single authoritative Media3 Composition used by final export. */
@UnstableApi
object Media3CompositionCompiler {
    const val TARGET_FRAME_RATE = 30

    fun compile(
        mediaInfo: MediaInfo,
        editPlan: EditPlan,
        input: File,
        freezeFrame: File?,
        plan: Media3CompositionPlan = Media3CompositionPlanCompiler.compile(mediaInfo, editPlan),
    ): CompiledMedia3Composition {
        require(input.isFile) { "Composition source is unavailable: ${input.absolutePath}" }
        require((plan.freeze == null) == (freezeFrame == null)) {
            "Freeze-frame asset must match the compiled composition plan"
        }

        val videoSequence = EditedMediaItemSequence.Builder().apply {
            var compositionOffsetUs = 0L
            if (plan.freeze != null) {
                addItem(
                    buildFreezeItem(
                        editPlan = editPlan,
                        freezeFrame = checkNotNull(freezeFrame),
                        freeze = plan.freeze,
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
        )
    }

    private fun buildEditedVideoItem(
        mediaInfo: MediaInfo,
        editPlan: EditPlan,
        input: File,
        range: TrimRange,
        plan: Media3CompositionPlan,
        compositionOffsetUs: Long,
    ): EditedMediaItem {
        val speedEffects = TransformSpeedEffectsFactory.forRender(
            settings = editPlan.transform,
            hasAudio = mediaInfo.hasAudio && !plan.removeSourceAudio,
        )
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
        }
        // Keep EditPlan overlay windows on the absolute source timeline, then project them to this
        // clipped item. Media3 adds preceding sequence-item duration before GlEffects execute, so
        // the shader also subtracts this item's composition offset and evaluates 0-based local
        // time. This prevents blur/logo windows from expiring early in later adaptive-cut items.
        val localOverlays = OverlayCompiler.projectToRange(editPlan.overlays, range)
        val videoEffects = TransformVideoEffects.forRender(
            settings = editPlan.transform,
            preset = editPlan.exportPreset,
            targetFrameRate = TARGET_FRAME_RATE.toFloat(),
            sourceDurationMs = range.durationMs,
            speedEffect = speedEffects?.videoEffect,
            overlays = localOverlays,
            // Media3 adds the sequence item offset before GlEffects run. Remove that offset so
            // the projected overlay windows are evaluated on this clipped item's 0-based time.
            sourceTimeOffsetUs = CompositionOverlayTimelinePolicy.localEffectTimeOffsetUs(
                compositionOffsetUs,
            ),
        )
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
            // Encoded video has an intrinsic duration. Media3's setDurationUs contract expects the
            // original pre-clipping source duration, not the clipped range duration, so leave it
            // unset for Transformer and let the asset loader report the clipped duration.
            .setRemoveAudio(plan.removeSourceAudio)
            .setEffects(Effects(audioProcessors, videoEffects))
            .build()
    }

    private fun buildFreezeItem(
        editPlan: EditPlan,
        freezeFrame: File,
        freeze: Media3FreezePlan,
    ): EditedMediaItem = EditedMediaItem.Builder(
        MediaItem.Builder()
            .setUri(freezeFrame.toURI().toString())
            .setImageDurationMs(freeze.durationMs)
            .build(),
    )
        .setDurationUs(freeze.durationMs * 1_000L)
        .setFrameRate(TARGET_FRAME_RATE)
        .setEffects(
            Effects(
                emptyList(),
                TransformVideoEffects.forRender(
                    settings = frozenVisualSettings(editPlan.transform),
                    preset = editPlan.exportPreset,
                    targetFrameRate = TARGET_FRAME_RATE.toFloat(),
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
