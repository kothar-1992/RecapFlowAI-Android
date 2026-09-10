package com.recapflow.ai.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.recapflow.ai.R
import com.recapflow.ai.media.edit.ImageOverlayAnimationPolicy
import com.recapflow.ai.media.edit.ImageOverlayAnimationPreset
import com.recapflow.ai.media.edit.ImageOverlayAnimationSettings
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Small child controller for image/logo animation controls.
 *
 * It is inflated into the existing image-overlay group at runtime so the very large editor
 * ViewBinding does not gain another set of generated fields. The controller owns only UI state;
 * canonical animation semantics remain in [ImageOverlayAnimationSettings].
 */
class ImageOverlayAnimationController(
    container: ViewGroup,
    private val onSettingsChanged: (ImageOverlayAnimationSettings) -> Unit,
) {
    private val context = container.context
    private val root: View = LayoutInflater.from(context)
        .inflate(R.layout.view_image_overlay_animation_controls, container, false)
        .also(container::addView)
    private val presetDropdown =
        root.findViewById<MaterialAutoCompleteTextView>(R.id.imageOverlayAnimationPresetDropdown)
    private val loopSwitch =
        root.findViewById<MaterialSwitch>(R.id.imageOverlayAnimationLoopSwitch)
    private val durationValue =
        root.findViewById<TextView>(R.id.imageOverlayAnimationDurationValue)
    private val durationSlider =
        root.findViewById<Slider>(R.id.imageOverlayAnimationDurationSlider)
    private val periodGroup =
        root.findViewById<LinearLayout>(R.id.imageOverlayAnimationPeriodGroup)
    private val periodValue =
        root.findViewById<TextView>(R.id.imageOverlayAnimationPeriodValue)
    private val periodSlider =
        root.findViewById<Slider>(R.id.imageOverlayAnimationPeriodSlider)
    private val summary =
        root.findViewById<TextView>(R.id.imageOverlayAnimationSummary)

    private val choices = listOf(
        Choice(ImageOverlayAnimationPreset.NONE, R.string.image_overlay_animation_preset_static),
        Choice(ImageOverlayAnimationPreset.FADE, R.string.image_overlay_animation_preset_fade),
        Choice(
            ImageOverlayAnimationPreset.FADE_SCALE,
            R.string.image_overlay_animation_preset_fade_scale,
        ),
        Choice(ImageOverlayAnimationPreset.POP, R.string.image_overlay_animation_preset_pop),
        Choice(ImageOverlayAnimationPreset.SLIDE, R.string.image_overlay_animation_preset_slide),
        Choice(ImageOverlayAnimationPreset.PULSE, R.string.image_overlay_animation_preset_pulse),
        Choice(ImageOverlayAnimationPreset.FLOAT, R.string.image_overlay_animation_preset_float),
        Choice(ImageOverlayAnimationPreset.ROTATE, R.string.image_overlay_animation_preset_rotate),
        Choice(ImageOverlayAnimationPreset.BOUNCE, R.string.image_overlay_animation_preset_bounce),
    )

    private var current = ImageOverlayAnimationSettings()
    private var callbacksSuppressed = false
    private var controlsEnabled = false
    private var controlsVisible = false

    init {
        val labels = choices.map { context.getString(it.labelRes) }
        presetDropdown.setAdapter(
            ArrayAdapter(context, android.R.layout.simple_list_item_1, labels),
        )
        presetDropdown.setOnItemClickListener { _, _, position, _ ->
            if (callbacksSuppressed) return@setOnItemClickListener
            val choice = choices.getOrNull(position) ?: return@setOnItemClickListener
            emit(current.copy(preset = choice.preset, phaseOffsetMs = 0L))
        }
        loopSwitch.setOnCheckedChangeListener { _, checked ->
            if (!callbacksSuppressed) {
                emit(current.copy(loopEnabled = checked, phaseOffsetMs = 0L))
            }
        }
        durationSlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || callbacksSuppressed) return@addOnChangeListener
            val durationMs = value.roundToLong().coerceIn(
                ImageOverlayAnimationPolicy.MIN_DURATION_MS,
                ImageOverlayAnimationPolicy.MAX_DURATION_MS,
            )
            emit(
                current.copy(
                    durationMs = durationMs,
                    periodMs = maxOf(current.periodMs, durationMs),
                    phaseOffsetMs = 0L,
                ),
            )
        }
        periodSlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || callbacksSuppressed) return@addOnChangeListener
            emit(
                current.copy(
                    periodMs = value.roundToLong().coerceAtLeast(current.durationMs),
                    phaseOffsetMs = 0L,
                ),
            )
        }
    }

    fun render(
        settings: ImageOverlayAnimationSettings,
        visible: Boolean,
        enabled: Boolean,
    ) {
        current = normalize(settings)
        controlsVisible = visible
        controlsEnabled = enabled
        callbacksSuppressed = true
        try {
            root.isVisible = visible
            presetDropdown.setText(label(current.preset), false)
            loopSwitch.isChecked = current.loopEnabled
            durationSlider.value = current.durationMs.toFloat()
            periodSlider.value = current.periodMs.toFloat()
            durationValue.text = context.getString(
                R.string.image_overlay_animation_duration_value,
                seconds(current.durationMs),
            )
            periodValue.text = context.getString(
                R.string.image_overlay_animation_period_value,
                seconds(current.periodMs),
            )
            val animated = current.preset != ImageOverlayAnimationPreset.NONE
            periodGroup.isVisible = animated && current.loopEnabled
            presetDropdown.isEnabled = enabled
            loopSwitch.isEnabled = enabled && animated
            durationSlider.isEnabled = enabled && animated
            periodSlider.isEnabled = enabled && animated && current.loopEnabled
            summary.text = when {
                !animated -> context.getString(R.string.image_overlay_animation_summary_static)
                current.loopEnabled -> context.getString(
                    R.string.image_overlay_animation_summary_loop,
                    label(current.preset),
                    seconds(current.durationMs),
                    seconds(current.periodMs),
                )
                else -> context.getString(
                    R.string.image_overlay_animation_summary_once,
                    label(current.preset),
                    seconds(current.durationMs),
                )
            }
        } finally {
            callbacksSuppressed = false
        }
    }

    private fun emit(settings: ImageOverlayAnimationSettings) {
        val normalized = normalize(settings)
        if (normalized == current) return
        current = normalized
        render(current, controlsVisible, controlsEnabled)
        onSettingsChanged(current)
    }

    private fun normalize(settings: ImageOverlayAnimationSettings): ImageOverlayAnimationSettings {
        val duration = settings.durationMs.coerceIn(
            ImageOverlayAnimationPolicy.MIN_DURATION_MS,
            ImageOverlayAnimationPolicy.MAX_DURATION_MS,
        )
        val period = settings.periodMs.coerceIn(
            maxOf(ImageOverlayAnimationPolicy.MIN_PERIOD_MS, duration),
            ImageOverlayAnimationPolicy.MAX_PERIOD_MS,
        )
        return settings.copy(
            durationMs = duration,
            periodMs = period,
            phaseOffsetMs = 0L,
        )
    }

    private fun label(preset: ImageOverlayAnimationPreset): String =
        context.getString(choices.first { it.preset == preset }.labelRes)

    private fun seconds(timeMs: Long): String =
        String.format(Locale.US, "%.1f", timeMs / 1_000.0)

    private data class Choice(
        val preset: ImageOverlayAnimationPreset,
        val labelRes: Int,
    )
}
