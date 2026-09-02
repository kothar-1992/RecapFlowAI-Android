package com.recapflow.ai.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.recapflow.ai.R
import com.recapflow.ai.media.edit.TargetDurationInputPolicy

/**
 * View-only adapter for the Target Duration Clips workflow.
 *
 * The slider selects a whole-second final target. Timeline decisions remain in
 * TargetDurationClipPlanner / TargetDurationClipIntegration and run only when [onGenerate] fires.
 */
class TargetDurationClipsController(
    private val context: Context,
    parent: ViewGroup,
    insertionIndex: Int,
    private val onGenerate: (targetDurationMs: Long) -> Boolean,
) {
    private val root: View = LayoutInflater.from(context)
        .inflate(R.layout.view_target_duration_clips, parent, false)
    private val sourceValue: TextView = root.findViewById(R.id.targetDurationSourceValue)
    private val selectedValue: TextView = root.findViewById(R.id.targetDurationSelectedValue)
    private val slider: Slider = root.findViewById(R.id.targetDurationSlider)
    private val minimumValue: TextView = root.findViewById(R.id.targetDurationMinimumValue)
    private val maximumValue: TextView = root.findViewById(R.id.targetDurationMaximumValue)
    private val coverageValue: TextView = root.findViewById(R.id.targetDurationCoverageValue)
    private val estimatedValue: TextView = root.findViewById(R.id.targetDurationEstimatedValue)
    private val validationMessage: TextView = root.findViewById(R.id.targetDurationValidationMessage)
    private val generateButton: MaterialButton = root.findViewById(R.id.targetDurationGenerateButton)
    private val status: TextView = root.findViewById(R.id.targetDurationStatus)

    private var sourceDurationMs: Long? = null
    private var appliedTargetDurationMs: Long? = null
    private var selectedTargetDurationMs: Long? = null
    private var pendingSelection = false
    private var updatingSlider = false

    init {
        parent.addView(root, insertionIndex.coerceIn(0, parent.childCount))
        slider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || updatingSlider) return@addOnChangeListener
            selectedTargetDurationMs = TargetDurationInputPolicy.sliderValueToDurationMs(value)
            pendingSelection = true
            validationMessage.visibility = View.GONE
            renderPendingSelection()
        }
        generateButton.setOnClickListener {
            val targetMs = selectedTargetDurationMs
            if (targetMs == null) {
                showError(R.string.target_duration_invalid)
                return@setOnClickListener
            }
            validationMessage.visibility = View.GONE
            if (!onGenerate(targetMs)) {
                showError(R.string.target_duration_impossible)
            } else {
                pendingSelection = false
                appliedTargetDurationMs = targetMs
            }
        }
    }

    fun render(
        sourceDurationMs: Long?,
        targetDurationMs: Long?,
        estimatedFinalDurationMs: Long?,
        sourceKeepRatio: Double?,
        renderActive: Boolean,
        targetApplied: Boolean,
        hasDraft: Boolean,
    ) {
        val sourceChanged = this.sourceDurationMs != sourceDurationMs
        val appliedTargetChanged = this.appliedTargetDurationMs != targetDurationMs
        this.sourceDurationMs = sourceDurationMs
        this.appliedTargetDurationMs = targetDurationMs

        sourceValue.text = sourceDurationMs?.let(MediaFormatters::duration) ?: "--:--"

        if (sourceChanged) {
            pendingSelection = false
            selectedTargetDurationMs = targetDurationMs
                ?: sourceDurationMs?.let(TargetDurationInputPolicy::defaultSliderTargetDurationMs)
        } else if (targetDurationMs != null && (!pendingSelection || appliedTargetChanged)) {
            pendingSelection = false
            selectedTargetDurationMs = targetDurationMs
        } else if (selectedTargetDurationMs == null) {
            selectedTargetDurationMs = sourceDurationMs
                ?.let(TargetDurationInputPolicy::defaultSliderTargetDurationMs)
        }

        configureSlider(renderActive)
        renderSelectedValue()

        if (pendingSelection) {
            renderPendingSelection()
            return
        }

        coverageValue.text = if (sourceKeepRatio != null) {
            context.getString(
                R.string.target_duration_source_keep_value,
                TargetDurationInputPolicy.sourceKeepPercent(sourceKeepRatio).toString(),
            )
        } else {
            context.getString(R.string.target_duration_compression_waiting)
        }
        estimatedValue.text = if (estimatedFinalDurationMs != null) {
            context.getString(
                R.string.target_duration_estimated_value,
                MediaFormatters.duration(estimatedFinalDurationMs),
            )
        } else {
            selectedTargetDurationMs?.let { target ->
                context.getString(
                    R.string.target_duration_estimated_value,
                    MediaFormatters.duration(target),
                )
            } ?: context.getString(R.string.target_duration_estimated_waiting)
        }
        status.text = when {
            sourceDurationMs == null -> context.getString(R.string.target_duration_no_source)
            selectedTargetDurationMs == null -> context.getString(R.string.target_duration_source_too_short)
            targetDurationMs == null -> context.getString(R.string.target_duration_slider_ready)
            targetApplied -> context.getString(R.string.target_duration_ready)
            hasDraft -> context.getString(R.string.target_duration_draft_off)
            else -> context.getString(R.string.target_duration_pending_changes)
        }
    }

    fun setTargetDurationMs(durationMs: Long?) {
        appliedTargetDurationMs = durationMs
        pendingSelection = false
        selectedTargetDurationMs = durationMs
            ?: sourceDurationMs?.let(TargetDurationInputPolicy::defaultSliderTargetDurationMs)
        configureSlider(renderActive = false)
        renderSelectedValue()
    }

    fun showImpossibleTarget() {
        showError(R.string.target_duration_impossible)
    }

    private fun configureSlider(renderActive: Boolean) {
        val sourceMs = sourceDurationMs
        val maxSeconds = sourceMs?.let(TargetDurationInputPolicy::sliderMaximumSeconds) ?: 0
        val minSeconds = TargetDurationInputPolicy.minimumTargetSeconds
        val hasValidTarget = maxSeconds >= minSeconds
        val hasAdjustableRange = maxSeconds > minSeconds

        minimumValue.text = MediaFormatters.duration(minSeconds * 1_000L)
        maximumValue.text = if (hasValidTarget) {
            MediaFormatters.duration(maxSeconds * 1_000L)
        } else {
            "--:--"
        }

        updatingSlider = true
        try {
            // Move through a neutral continuous range first so a shorter newly imported source
            // cannot leave the old Slider value outside the next valueTo bound.
            slider.stepSize = 0f
            slider.value = slider.valueFrom
            slider.valueFrom = 0f
            slider.value = 0f
            slider.valueTo = maxSeconds.coerceAtLeast(1).toFloat()

            if (hasAdjustableRange) {
                val selectedSeconds = ((selectedTargetDurationMs ?: maxSeconds * 1_000L) / 1_000L)
                    .toInt()
                    .coerceIn(minSeconds, maxSeconds)
                selectedTargetDurationMs = selectedSeconds * 1_000L
                slider.value = selectedSeconds.toFloat()
                slider.valueFrom = minSeconds.toFloat()
                slider.stepSize = 1f
            } else {
                slider.valueTo = 1f
                slider.stepSize = 1f
                slider.value = if (hasValidTarget) 1f else 0f
                if (hasValidTarget) {
                    selectedTargetDurationMs = maxSeconds * 1_000L
                } else {
                    selectedTargetDurationMs = null
                }
            }
        } finally {
            updatingSlider = false
        }

        slider.isEnabled = hasAdjustableRange && !renderActive
        generateButton.isEnabled = hasValidTarget && selectedTargetDurationMs != null && !renderActive
    }

    private fun renderPendingSelection() {
        renderSelectedValue()
        val sourceMs = sourceDurationMs
        val targetMs = selectedTargetDurationMs
        if (sourceMs != null && targetMs != null) {
            coverageValue.text = context.getString(
                R.string.target_duration_selected_ratio,
                TargetDurationInputPolicy.targetOutputPercent(targetMs, sourceMs).toString(),
            )
            estimatedValue.text = context.getString(
                R.string.target_duration_estimated_value,
                MediaFormatters.duration(targetMs),
            )
            status.setText(R.string.target_duration_pending_changes)
        }
    }

    private fun renderSelectedValue() {
        selectedValue.text = selectedTargetDurationMs?.let(MediaFormatters::duration) ?: "--:--"
    }

    private fun showError(messageRes: Int) {
        validationMessage.setText(messageRes)
        validationMessage.visibility = View.VISIBLE
    }
}
