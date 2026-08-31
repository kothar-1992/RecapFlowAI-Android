package com.recapflow.ai.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.recapflow.ai.R
import com.recapflow.ai.media.edit.ClipTransitionBoundary
import com.recapflow.ai.media.edit.ClipTransitionEasing
import com.recapflow.ai.media.edit.ClipTransitionEditorPolicy
import com.recapflow.ai.media.edit.ClipTransitionEditorState
import com.recapflow.ai.media.edit.ClipTransitionPolicy
import com.recapflow.ai.media.edit.ClipTransitionSettings
import com.recapflow.ai.media.edit.TrimRange
import java.text.NumberFormat

/**
 * Small View-based adapter for the Phase 6H.1D boundary editor.
 *
 * The semantic state remains in [ClipTransitionEditorPolicy]. This class only renders it and emits
 * one callback when the reviewed EditPlan meaning changes. Boundary navigation and preview do not
 * mutate media or start an encode.
 */
class ClipTransitionEditorController(
    private val context: Context,
    parent: ViewGroup,
    private val selectedRangesProvider: () -> List<TrimRange>,
    private val onSettingsChanged: () -> Unit,
    private val onPreviewBoundary: (left: TrimRange, right: TrimRange, enabled: Boolean) -> Unit,
) {
    private val root: View = LayoutInflater.from(context)
        .inflate(R.layout.view_clip_transition_controls, parent, false)
    private val unavailable: TextView = root.findViewById(R.id.clipTransitionUnavailable)
    private val availableGroup: LinearLayout = root.findViewById(R.id.clipTransitionAvailableGroup)
    private val boundaryValue: TextView = root.findViewById(R.id.clipTransitionBoundaryValue)
    private val previousButton: MaterialButton = root.findViewById(R.id.clipTransitionPreviousButton)
    private val nextButton: MaterialButton = root.findViewById(R.id.clipTransitionNextButton)
    private val enabledSwitch: MaterialSwitch = root.findViewById(R.id.clipTransitionEnabledSwitch)
    private val summary: TextView = root.findViewById(R.id.clipTransitionSummary)
    private val durationSlider: Slider = root.findViewById(R.id.clipTransitionDurationSlider)
    private val durationValue: TextView = root.findViewById(R.id.clipTransitionDurationValue)
    private val easingGroup: MaterialButtonToggleGroup = root.findViewById(R.id.clipTransitionEasingGroup)
    private val previewButton: MaterialButton = root.findViewById(R.id.clipTransitionPreviewButton)
    private val resetButton: MaterialButton = root.findViewById(R.id.clipTransitionResetButton)

    private var state = ClipTransitionEditorState()
    private var busy = false
    private var rendering = false

    init {
        parent.addView(root)
        bind()
        render()
    }

    fun currentSettings(): ClipTransitionSettings {
        state = ClipTransitionEditorPolicy.reconcile(state, selectedRangesProvider())
        return state.settings
    }

    fun reconcile() {
        state = ClipTransitionEditorPolicy.reconcile(state, selectedRangesProvider())
        render()
    }

    /**
     * Replaces semantic settings without emitting [onSettingsChanged].
     * Used when target-duration replanning rebinds source-boundary identities by clip index.
     */
    fun replaceSettings(settings: ClipTransitionSettings) {
        state = state.copy(settings = settings)
        state = ClipTransitionEditorPolicy.reconcile(state, selectedRangesProvider())
        render()
    }

    fun reset() {
        state = ClipTransitionEditorState()
        render()
    }

    fun setBusy(value: Boolean) {
        if (busy == value) return
        busy = value
        render()
    }

    fun selectedBoundaryEnabled(): Boolean =
        ClipTransitionEditorPolicy.selectedBoundary(state, selectedRangesProvider())?.enabled == true

    private fun bind() {
        previousButton.setOnClickListener {
            state = ClipTransitionEditorPolicy.selectPrevious(state, selectedRangesProvider())
            render()
        }
        nextButton.setOnClickListener {
            state = ClipTransitionEditorPolicy.selectNext(state, selectedRangesProvider())
            render()
        }
        enabledSwitch.setOnCheckedChangeListener { _, enabled ->
            if (rendering) return@setOnCheckedChangeListener
            state = ClipTransitionEditorPolicy.setSelectedEnabled(
                state,
                selectedRangesProvider(),
                enabled,
            )
            render()
            onSettingsChanged()
        }
        durationSlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || rendering) return@addOnChangeListener
            state = ClipTransitionEditorPolicy.setSelectedDuration(
                state,
                selectedRangesProvider(),
                value.toLong(),
            )
            render()
            onSettingsChanged()
        }
        easingGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || rendering) return@addOnButtonCheckedListener
            val easing = if (checkedId == R.id.clipTransitionLinearButton) {
                ClipTransitionEasing.LINEAR
            } else {
                ClipTransitionEasing.EASE_IN_OUT
            }
            state = ClipTransitionEditorPolicy.setSelectedEasing(
                state,
                selectedRangesProvider(),
                easing,
            )
            render()
            onSettingsChanged()
        }
        previewButton.setOnClickListener {
            val ranges = selectedRangesProvider()
            val pair = ClipTransitionEditorPolicy.selectedRangePair(state, ranges)
                ?: return@setOnClickListener
            onPreviewBoundary(pair.first, pair.second, selectedBoundaryEnabled())
        }
        resetButton.setOnClickListener {
            state = ClipTransitionEditorPolicy.resetSelectedToHardCut(
                state,
                selectedRangesProvider(),
            )
            render()
            onSettingsChanged()
        }
    }

    private fun render() {
        val ranges = selectedRangesProvider()
        state = ClipTransitionEditorPolicy.reconcile(state, ranges)
        val pair = ClipTransitionEditorPolicy.selectedRangePair(state, ranges)
        rendering = true
        try {
            unavailable.visibility = if (pair == null) View.VISIBLE else View.GONE
            availableGroup.visibility = if (pair == null) View.GONE else View.VISIBLE
            if (pair == null) return

            val boundaryCount = ranges.size - 1
            val selectedIndex = state.selectedBoundaryIndex
            val boundary = ClipTransitionEditorPolicy.selectedBoundary(state, ranges)
            val effective = boundary ?: defaultBoundary(pair.first, pair.second)
            val enabled = boundary?.enabled == true
            val readableDuration = humanDurationLabel(effective.durationMs)

            boundaryValue.text = context.getString(
                R.string.clip_transition_boundary_value,
                selectedIndex + 1,
                boundaryCount,
                MediaFormatters.duration(pair.first.endMs),
                MediaFormatters.duration(pair.second.startMs),
            )
            previousButton.isEnabled = !busy && selectedIndex > 0
            nextButton.isEnabled = !busy && selectedIndex < boundaryCount - 1
            enabledSwitch.isEnabled = !busy
            enabledSwitch.isChecked = enabled
            summary.text = if (enabled) {
                context.getString(
                    R.string.clip_transition_on_summary,
                    readableDuration,
                    easingLabel(effective.easing),
                )
            } else {
                context.getString(R.string.clip_transition_off_summary)
            }
            durationSlider.value = effective.durationMs.toFloat()
            durationSlider.isEnabled = !busy && enabled
            durationValue.text = context.getString(
                R.string.clip_transition_duration_value,
                readableDuration,
            )
            easingGroup.check(
                if (effective.easing == ClipTransitionEasing.LINEAR) {
                    R.id.clipTransitionLinearButton
                } else {
                    R.id.clipTransitionEaseButton
                },
            )
            setChildrenEnabled(easingGroup, !busy && enabled)
            previewButton.isEnabled = !busy
            resetButton.isEnabled = !busy && boundary != null
        } finally {
            rendering = false
        }
    }

    private fun humanDurationLabel(durationMs: Long): String {
        val locale = context.resources.configuration.locales.get(0)
        val number = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }.format(durationMs.toDouble() / 1_000.0)
        return context.getString(R.string.clip_transition_seconds_value, number)
    }

    private fun defaultBoundary(left: TrimRange, right: TrimRange): ClipTransitionBoundary =
        ClipTransitionBoundary(
            leftSourceEndMs = left.endMs,
            rightSourceStartMs = right.startMs,
            durationMs = ClipTransitionPolicy.DEFAULT_DURATION_MS,
            easing = ClipTransitionEasing.EASE_IN_OUT,
            enabled = false,
        )

    private fun easingLabel(easing: ClipTransitionEasing): String = context.getString(
        if (easing == ClipTransitionEasing.LINEAR) {
            R.string.clip_transition_linear
        } else {
            R.string.clip_transition_ease_in_out
        },
    )

    private fun setChildrenEnabled(group: ViewGroup, enabled: Boolean) {
        for (index in 0 until group.childCount) {
            group.getChildAt(index).isEnabled = enabled
        }
    }
}
