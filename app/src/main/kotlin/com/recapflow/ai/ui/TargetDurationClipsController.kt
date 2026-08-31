package com.recapflow.ai.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.recapflow.ai.R
import com.recapflow.ai.media.edit.TargetDurationInputPolicy

/**
 * View-only adapter for the Target Duration Clips workflow.
 *
 * It owns mm:ss input/rendering only. Timeline decisions remain in TargetDurationClipPlanner /
 * TargetDurationClipIntegration and are supplied through [onGenerate].
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
    private val minutesInput: TextInputEditText = root.findViewById(R.id.targetDurationMinutesInput)
    private val secondsInput: TextInputEditText = root.findViewById(R.id.targetDurationSecondsInput)
    private val coverageValue: TextView = root.findViewById(R.id.targetDurationCoverageValue)
    private val estimatedValue: TextView = root.findViewById(R.id.targetDurationEstimatedValue)
    private val validationMessage: TextView = root.findViewById(R.id.targetDurationValidationMessage)
    private val generateButton: MaterialButton = root.findViewById(R.id.targetDurationGenerateButton)
    private val status: TextView = root.findViewById(R.id.targetDurationStatus)

    private var sourceDurationMs: Long? = null
    private var appliedTargetDurationMs: Long? = null
    private var updatingFields = false

    init {
        parent.addView(root, insertionIndex.coerceIn(0, parent.childCount))
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (updatingFields) return
                validationMessage.visibility = View.GONE
                if (sourceDurationMs != null && parsedTargetDurationMs() != appliedTargetDurationMs) {
                    status.setText(R.string.target_duration_pending_changes)
                }
            }
        }
        minutesInput.addTextChangedListener(watcher)
        secondsInput.addTextChangedListener(watcher)
        generateButton.setOnClickListener {
            val targetMs = parsedTargetDurationMs()
            if (targetMs == null) {
                showError(R.string.target_duration_invalid)
                return@setOnClickListener
            }
            validationMessage.visibility = View.GONE
            if (!onGenerate(targetMs)) {
                showError(R.string.target_duration_impossible)
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
        this.sourceDurationMs = sourceDurationMs
        this.appliedTargetDurationMs = targetDurationMs
        sourceValue.text = sourceDurationMs?.let(MediaFormatters::duration) ?: "--:--"
        generateButton.isEnabled = sourceDurationMs != null && !renderActive
        minutesInput.isEnabled = sourceDurationMs != null && !renderActive
        secondsInput.isEnabled = sourceDurationMs != null && !renderActive

        if (
            targetDurationMs != null &&
            !minutesInput.hasFocus() &&
            !secondsInput.hasFocus() &&
            parsedTargetDurationMs() != targetDurationMs
        ) {
            setTargetDurationMs(targetDurationMs)
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
            context.getString(R.string.target_duration_estimated_waiting)
        }
        status.text = when {
            sourceDurationMs == null -> context.getString(R.string.target_duration_no_source)
            targetDurationMs == null -> context.getString(R.string.target_duration_description)
            targetApplied -> context.getString(R.string.target_duration_ready)
            hasDraft -> context.getString(R.string.target_duration_draft_off)
            else -> context.getString(R.string.target_duration_pending_changes)
        }
    }

    fun setTargetDurationMs(durationMs: Long?) {
        updatingFields = true
        try {
            if (durationMs == null) {
                minutesInput.setText("")
                secondsInput.setText("")
                return
            }
            minutesInput.setText(TargetDurationInputPolicy.minutesPart(durationMs).toString())
            secondsInput.setText(TargetDurationInputPolicy.secondsPart(durationMs).toString())
        } finally {
            updatingFields = false
        }
    }

    fun showImpossibleTarget() {
        showError(R.string.target_duration_impossible)
    }

    private fun parsedTargetDurationMs(): Long? = TargetDurationInputPolicy.parseDurationMs(
        minutesText = minutesInput.text?.toString().orEmpty(),
        secondsText = secondsInput.text?.toString().orEmpty(),
    )

    private fun showError(messageRes: Int) {
        validationMessage.setText(messageRes)
        validationMessage.visibility = View.VISIBLE
    }
}
