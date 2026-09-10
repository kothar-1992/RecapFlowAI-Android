package com.recapflow.ai.ui

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.recapflow.ai.R
import com.recapflow.ai.media.ai.*
import com.recapflow.ai.media.edit.*
import java.io.File
import java.util.concurrent.Executors

/** Session-only BYOK workflow. No key is saved in preferences, bundles, backups or logs. */
class SmartClipsController(
    private val activity: Activity,
    parent: ViewGroup,
    private val currentPlan: () -> EditPlan?,
    private val applyPlan: (EditPlan) -> Unit,
    private val previewPlan: (EditPlan) -> Unit,
) : AutoCloseable {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val analyzer = GeminiSmartCutAnalyzer()
    private var key = ""
    private var model = GeminiSmartCutAnalyzer.DEFAULT_MODEL
    private var token: GeminiCancellation? = null
    private var closed = false
    private var undo: AppliedSmartCut? = null
    private var undoFingerprint: String? = null
    private val dialogs = mutableListOf<AlertDialog>()
    private val entry = MaterialButton(activity).apply {
        setText(R.string.smart_clips_title)
        setOnClickListener { open() }
    }

    init { parent.addView(entry, 0) }

    private fun open() {
        if (token != null || closed) return
        val plan = currentPlan()
        if (plan == null) { message(R.string.smart_clips_unavailable); return }
        val content = column()
        fun selector(label: Int, items: List<Int>): Spinner {
            content.addView(TextView(activity).apply { setText(label) })
            return Spinner(activity).apply {
                adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, items.map(activity::getString))
                content.addView(this)
            }
        }
        val contentType = selector(R.string.auto_content_type, listOf(R.string.auto_general, R.string.auto_movie,
            R.string.auto_podcast, R.string.auto_interview, R.string.auto_tutorial))
        val duration = selector(R.string.auto_clip_duration, listOf(R.string.auto_duration_auto, R.string.auto_duration_30,
            R.string.auto_duration_60, R.string.auto_duration_180, R.string.auto_duration_300, R.string.auto_duration_long))
        content.addView(TextView(activity).apply { setText(R.string.auto_instructions) })
        val instructions = EditText(activity).apply {
            hint = activity.getString(R.string.auto_instructions_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2; maxLines = 4
            filters = arrayOf(android.text.InputFilter.LengthFilter(2_000))
            content.addView(this)
        }
        val keyInput = EditText(activity).apply {
            hint = activity.getString(R.string.smart_clips_key)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true; isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            setText(key)
        }
        val modelInput = EditText(activity).apply {
            hint = activity.getString(R.string.smart_clips_model); isSingleLine = true; setText(model)
        }
        val modes = Spinner(activity).apply {
            adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item,
                listOf(R.string.smart_clips_pauses, R.string.smart_clips_dialogue, R.string.smart_clips_recap).map(activity::getString))
        }
        val consent = CheckBox(activity).apply { setText(R.string.smart_clips_consent) }
        val count = GeminiSmartCutAnalyzer.windows(plan.sourceDurationMs.coerceIn(1, GeminiSmartCutAnalyzer.MAX_DURATION_MS)).size
        content.addView(TextView(activity).apply { text = activity.getString(R.string.smart_clips_explanation, count) })
        content.addView(keyInput); content.addView(modelInput); content.addView(modes); content.addView(consent)
        content.addView(MaterialButton(activity).apply {
            setText(R.string.smart_clips_test)
            setOnClickListener {
                if (keyInput.text.isNotEmpty()) key = keyInput.text.toString().trim()
                model = modelInput.text.toString().trim()
                keyInput.text.clear()
                runJob { cancellation, _ -> analyzer.testConnection(key, model, cancellation); { message(R.string.smart_clips_connected) } }
            }
        })
        content.addView(MaterialButton(activity).apply {
            setText(R.string.smart_clips_remove_key)
            setOnClickListener { key = ""; keyInput.text.clear(); token?.cancel() }
        })
        content.addView(MaterialButton(activity).apply {
            setText(R.string.smart_clips_undo)
            isEnabled = undo?.undo(plan) != null
            setOnClickListener {
                val current = currentPlan()
                val restored = current?.let { undo?.undo(it) }
                val fingerprint = undoFingerprint
                if (restored == null || fingerprint == null) message(R.string.smart_clips_stale) else {
                    runJob { cancellation, _ ->
                        analyzer.verifySource(File(restored.sourcePath), fingerprint, cancellation)
                        val next: () -> Unit = {
                            if (currentPlan() != current) message(R.string.smart_clips_stale) else {
                                applyPlan(restored); undo = null; undoFingerprint = null
                                dialogs.toList().forEach { it.dismiss() }
                            }
                        }
                        next
                    }
                }
            }
        })
        val dialog = show(MaterialAlertDialogBuilder(activity).setTitle(R.string.smart_clips_title)
            .setView(ScrollView(activity).apply { addView(content) })
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.smart_clips_analyze, null).create())
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!consent.isChecked) { message(R.string.smart_clips_consent_required); return@setOnClickListener }
            if (keyInput.text.isNotEmpty()) key = keyInput.text.toString().trim()
            model = modelInput.text.toString().trim()
            keyInput.text.clear()
            val selectedMode = SmartCutAnalysisMode.entries[modes.selectedItemPosition]
            val options = AutoClipOptions(AutoContentType.entries[contentType.selectedItemPosition],
                AutoClipDuration.entries[duration.selectedItemPosition], instructions.text.toString())
            dialog.dismiss()
            runJob { cancellation, progress ->
                val file = File(plan.sourcePath)
                val mime = when (file.extension.lowercase()) {
                    "mp4", "m4v" -> "video/mp4"
                    "mov" -> "video/mov"
                    "webm" -> "video/webm"
                    "3gp" -> "video/3gpp"
                    "avi" -> "video/avi"
                    "mkv" -> throw GeminiException(GeminiFailure.MEDIA_LIMIT)
                    else -> throw GeminiException(GeminiFailure.MEDIA_LIMIT)
                }
                val result = analyzer.analyze(file, mime, plan.sourceDurationMs, key, model, selectedMode, cancellation, progress, options)
                val next: () -> Unit = { review(plan, result) }
                next
            }
        }
    }

    private fun runJob(task: (GeminiCancellation, (GeminiProgress) -> Unit) -> (() -> Unit)) {
        if (token != null || closed) return
        val cancellation = GeminiCancellation()
        token = cancellation; entry.isEnabled = false
        val status = TextView(activity).apply { setPadding(32, 32, 32, 32); setText(R.string.smart_clips_preparing) }
        val progressDialog = show(MaterialAlertDialogBuilder(activity).setTitle(R.string.smart_clips_title)
            .setView(status).setNegativeButton(android.R.string.cancel, null).create())
        fun cancel() { cancellation.cancel(); status.setText(R.string.smart_clips_cancelling) }
        progressDialog.setOnCancelListener { cancel() }
        progressDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { cancel(); it.isEnabled = false }
        worker.execute {
            var result: (() -> Unit)? = null
            var error: GeminiException? = null
            try {
                result = task(cancellation) { progress -> main.post {
                    if (!closed && token === cancellation) status.text = activity.getString(stage(progress.stage), progress.completed + 1, progress.total)
                } }
            } catch (failure: GeminiException) { error = failure }
            catch (_: Exception) { error = GeminiException(GeminiFailure.INVALID_RESPONSE) }
            main.post {
                if (!closed && token === cancellation) {
                    token = null; entry.isEnabled = true; progressDialog.dismiss()
                    if (error == null) try { cancellation.check() } catch (cancelled: GeminiException) { error = cancelled }
                    if (error != null) {
                        message(errorMessage(error!!.failure))
                        if (error!!.cleanupFailed) message(R.string.smart_clips_cleanup_failed)
                    } else result?.invoke()
                }
            }
        }
    }

    private fun review(before: EditPlan, result: GeminiAnalysisResult) {
        if (currentPlan() != before) { message(R.string.smart_clips_stale); return }
        val original = AdaptiveCutCompiler.compile(before.adaptiveCuts, before.trimRange) ?: listOf(before.trimRange)
        val selected = result.candidates.map { it.id }.toMutableSet()
        val content = column()
        val summary = TextView(activity)
        content.addView(summary)
        var applied: AppliedSmartCut? = null
        var removedMs = 0L
        fun recalculate() {
            val draft = SmartCutPlanner.draft(result.sourceFingerprint, before.trimRange, original,
                result.candidates.filter { it.id in selected })
            applied = SmartCutIntegration.apply(before, before, result.sourceFingerprint, draft)
            removedMs = draft.removedDurationMs
            summary.text = activity.getString(R.string.smart_clips_summary,
                MediaFormatters.duration(before.plannedDurationMs),
                MediaFormatters.duration(applied?.after?.plannedDurationMs ?: before.plannedDurationMs),
                draft.decisions.count { it.rejection == null }, draft.decisions.count { it.rejection != null })
        }
        result.candidates.forEach { candidate ->
            content.addView(CheckBox(activity).apply {
                text = "${MediaFormatters.duration(candidate.range.startMs)}–${MediaFormatters.duration(candidate.range.endMs)}: ${candidate.evidence}"
                isChecked = true
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selected.add(candidate.id) else selected.remove(candidate.id)
                    recalculate()
                }
            })
        }
        recalculate()
        content.addView(MaterialButton(activity).apply {
            setText(R.string.smart_clips_preview_original); setOnClickListener { previewPlan(before) }
        })
        content.addView(MaterialButton(activity).apply {
            setText(R.string.smart_clips_preview_draft); setOnClickListener { applied?.after?.let(previewPlan) }
        })
        if (result.cleanupFailed) content.addView(TextView(activity).apply { setText(R.string.smart_clips_cleanup_failed) })
        val dialog = show(MaterialAlertDialogBuilder(activity).setTitle(R.string.smart_clips_review)
            .setView(ScrollView(activity).apply { addView(content) }).setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.smart_clips_apply, null).create())
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (currentPlan() != before) { message(R.string.smart_clips_stale); return@setOnClickListener }
            if (removedMs == 0L) { message(R.string.smart_clips_no_cuts); return@setOnClickListener }
            val transaction = applied ?: run { message(R.string.smart_clips_invalid); return@setOnClickListener }
            runJob { cancellation, _ ->
                analyzer.verifySource(File(before.sourcePath), result.sourceFingerprint, cancellation)
                val next: () -> Unit = {
                    if (currentPlan() != before) message(R.string.smart_clips_stale) else {
                        applyPlan(transaction.after); undo = transaction
                        undoFingerprint = result.sourceFingerprint; dialog.dismiss()
                    }
                }
                next
            }
        }
    }

    private fun column() = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 16) }
    private fun show(dialog: AlertDialog): AlertDialog {
        dialogs += dialog; dialog.setOnDismissListener { dialogs.remove(dialog) }; dialog.show(); return dialog
    }
    private fun message(id: Int) { show(MaterialAlertDialogBuilder(activity).setMessage(id).setPositiveButton(android.R.string.ok, null).create()) }
    override fun close() {
        closed = true; key = ""; token?.cancel(); worker.shutdown(); main.removeCallbacksAndMessages(null)
        dialogs.toList().forEach { it.dismiss() }
    }

    private fun stage(stage: GeminiStage) = when (stage) {
        GeminiStage.PREPARING -> R.string.smart_clips_preparing
        GeminiStage.UPLOADING -> R.string.smart_clips_uploading
        GeminiStage.PROCESSING -> R.string.smart_clips_processing
        GeminiStage.ANALYZING -> R.string.smart_clips_analyzing
        GeminiStage.CLEANING_UP -> R.string.smart_clips_cleaning
    }
    private fun errorMessage(failure: GeminiFailure) = when (failure) {
        GeminiFailure.REGION -> R.string.smart_clips_region
        GeminiFailure.KEY -> R.string.smart_clips_key_error
        GeminiFailure.QUOTA -> R.string.smart_clips_quota
        GeminiFailure.TIMEOUT -> R.string.smart_clips_timeout
        GeminiFailure.NETWORK -> R.string.smart_clips_network
        GeminiFailure.CANCELLED -> R.string.smart_clips_cancelled
        GeminiFailure.STALE_SOURCE -> R.string.smart_clips_stale
        GeminiFailure.MODEL -> R.string.smart_clips_model_error
        GeminiFailure.MEDIA_LIMIT -> R.string.smart_clips_media_limit
        GeminiFailure.SERVER -> R.string.smart_clips_server
        else -> R.string.smart_clips_invalid
    }
}
