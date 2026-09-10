#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
EN = ROOT / "app/src/main/res/values/strings.xml"
MY = ROOT / "app/src/main/res/values-my/strings_core.xml"
MARKER = "PHASE6H1F_TARGET_DURATION_UI"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 anchor, found {count}")
    return text.replace(old, new, 1)


def require(path: Path) -> str:
    if not path.is_file():
        raise SystemExit(f"Missing required file: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


main = require(MAIN)
if MARKER in main:
    print("Phase 6H.1F Target Duration UI already applied.")
    raise SystemExit(0)

# Imports.
main = replace_once(
    main,
    "import com.recapflow.ai.media.edit.ClipTransitionSettings\n",
    "import com.recapflow.ai.media.edit.ClipPlanningMode\n"
    "import com.recapflow.ai.media.edit.ClipTransitionSettings\n",
    "ClipPlanningMode import",
)
main = replace_once(
    main,
    "import com.recapflow.ai.media.edit.TransformSettings\n",
    "import com.recapflow.ai.media.edit.TargetDurationClipIntegration\n"
    "import com.recapflow.ai.media.edit.TargetDurationClipPlanner\n"
    "import com.recapflow.ai.media.edit.TransformSettings\n",
    "Target Duration integration imports",
)
main = replace_once(
    main,
    "import com.recapflow.ai.ui.MediaFormatters\n",
    "import com.recapflow.ai.ui.MediaFormatters\n"
    "import com.recapflow.ai.ui.TargetDurationClipsController\n",
    "Target Duration controller import",
)

# Controller/state. Keep Target Duration UI out of the giant generated editor ViewBinding.
main = replace_once(
    main,
    "    private lateinit var clipTransitionEditorController: ClipTransitionEditorController\n",
    "    private lateinit var clipTransitionEditorController: ClipTransitionEditorController\n"
    "    private lateinit var targetDurationClipsController: TargetDurationClipsController\n",
    "Target Duration controller state",
)
main = replace_once(
    main,
    "    private var adaptivePreset = AdaptiveCutPreset.BALANCED\n",
    "    // PHASE6H1F_TARGET_DURATION_UI: target output length is the primary Clips authority.\n"
    "    private var targetDurationMs: Long? = null\n"
    "    private var targetDurationTimingSignature: String? = null\n"
    "    private var adaptivePreset = AdaptiveCutPreset.BALANCED\n",
    "Target Duration state",
)

# Restore the target, but deliberately do not restore old head/tail Trim intent.
main = replace_once(
    main,
    "        adaptivePreset = savedInstanceState?.getString(KEY_ADAPTIVE_PRESET)\n"
    "            ?.let { savedName -> AdaptiveCutPreset.entries.firstOrNull { it.name == savedName } }\n"
    "            ?: AdaptiveCutPreset.BALANCED\n",
    "        adaptivePreset = savedInstanceState?.getString(KEY_ADAPTIVE_PRESET)\n"
    "            ?.let { savedName -> AdaptiveCutPreset.entries.firstOrNull { it.name == savedName } }\n"
    "            ?: AdaptiveCutPreset.BALANCED\n"
    "        targetDurationMs = savedInstanceState\n"
    "            ?.takeIf { it.containsKey(KEY_TARGET_DURATION_MS) }\n"
    "            ?.getLong(KEY_TARGET_DURATION_MS)\n"
    "            ?.takeIf { it >= TargetDurationClipPlanner.MIN_TARGET_DURATION_MS }\n",
    "Target Duration restore",
)

# Install the runtime-inflated Target Duration controls before the legacy Trim controls.
main = replace_once(
    main,
    "        editor.resetTrimButton.setOnClickListener { resetTrimToFullSource() }\n"
    "        bindAdaptiveCutControls()\n",
    "        editor.resetTrimButton.setOnClickListener { resetTrimToFullSource() }\n"
    "        bindTargetDurationClipsControls()\n"
    "        bindAdaptiveCutControls()\n",
    "Target Duration bindActions hook",
)

helpers = r'''    private fun currentAdaptiveCutSettings(): AdaptiveCutSettings = AdaptiveCutSettings(
        enabled = adaptiveApplied,
        preset = adaptivePreset,
        reviewedRanges = adaptiveDraftRanges,
        mode = if (targetDurationMs != null) {
            ClipPlanningMode.TARGET_DURATION
        } else {
            ClipPlanningMode.PRESET_PACING
        },
        targetDurationMs = targetDurationMs,
    )

    private fun bindTargetDurationClipsControls() {
        val parent = editor.editCard.getChildAt(0) as? ViewGroup
            ?: error("Clips editor card must expose a ViewGroup content root")

        // User-facing head/tail Trim is intentionally retired for the normal Clips workflow.
        // Keep its bound slider as an internal full-source boundary so older IR/compiler code stays
        // stable while the authoritative user input moves to final target duration.
        editor.trimRangeSlider.isVisible = false
        ((editor.trimStartValue.parent as? View)?.parent as? View)?.isVisible = false
        editor.trimDurationValue.isVisible = false
        editor.trimValidationMessage.isVisible = false
        editor.resetTrimButton.isVisible = false

        targetDurationClipsController = TargetDurationClipsController(
            context = this,
            parent = parent,
            insertionIndex = 3,
            onGenerate = ::generateTargetDurationPlan,
        )
        targetDurationClipsController.setTargetDurationMs(targetDurationMs)
        renderTargetDurationClipsControls()
    }

    private fun renderTargetDurationClipsControls() {
        if (!::targetDurationClipsController.isInitialized) return
        val info = activeMediaInfo
        val target = targetDurationMs
        val hasTargetDraft = target != null && adaptiveDraftRanges.isNotEmpty()
        val sourceKeepRatio = if (info != null && hasTargetDraft && info.durationMs > 0L) {
            adaptiveDraftRanges.sumOf { it.durationMs }.toDouble() / info.durationMs.toDouble()
        } else {
            null
        }
        val estimatedFinalDurationMs = when {
            info == null || !hasTargetDraft -> null
            adaptiveApplied -> currentEditPlan(RenderPreset.HD_720P).plannedDurationMs
            else -> target
        }
        val renderActive = ::renderCoordinator.isInitialized &&
            renderCoordinator.currentState.isActiveRender()
        targetDurationClipsController.render(
            sourceDurationMs = info?.durationMs,
            targetDurationMs = target,
            estimatedFinalDurationMs = estimatedFinalDurationMs,
            sourceKeepRatio = sourceKeepRatio,
            renderActive = renderActive,
            targetApplied = target != null && adaptiveApplied,
            hasDraft = hasTargetDraft,
        )
    }

    private fun generateTargetDurationPlan(targetMs: Long): Boolean {
        cancelAdaptivePreview()
        val generated = applyTargetDurationPlan(targetMs, resetCandidate = true)
        if (!generated) return false
        onUserChangedAdaptiveCuts()
        seekToAdaptiveCandidate()
        return true
    }

    private fun applyTargetDurationPlan(
        targetMs: Long,
        resetCandidate: Boolean,
    ): Boolean {
        val info = activeMediaInfo ?: return false
        if (renderCoordinator.currentState.isActiveRender()) return false
        val sourceRange = TrimRange(0L, info.durationMs)
        val currentRanges = currentSelectedClipRanges(info)
        val currentTransitions = if (::clipTransitionEditorController.isInitialized) {
            clipTransitionEditorController.currentSettings()
        } else {
            ClipTransitionSettings()
        }
        val result = TargetDurationClipIntegration.generate(
            sourceRange = sourceRange,
            targetDurationMs = targetMs,
            currentAdaptiveCuts = currentAdaptiveCutSettings(),
            currentSelectedRanges = currentRanges,
            transform = currentTransformSettings(),
            clipTransitions = currentTransitions,
        ) ?: return false

        targetDurationMs = targetMs
        adaptiveDraftRanges = result.adaptiveCuts.reviewedRanges
        adaptiveApplied = true
        adaptiveCandidateIndex = if (resetCandidate) {
            0
        } else {
            adaptiveCandidateIndex.coerceIn(0, adaptiveDraftRanges.lastIndex.coerceAtLeast(0))
        }
        targetDurationTimingSignature = currentTargetDurationTimingSignature()
        if (::clipTransitionEditorController.isInitialized) {
            clipTransitionEditorController.replaceSettings(result.clipTransitions)
        }
        if (editor.adaptiveApplySwitch.isChecked != true) {
            editor.adaptiveApplySwitch.isChecked = true
        }
        renderAdaptiveCutControls()
        renderTargetDurationClipsControls()
        return true
    }

    private fun currentTargetDurationTimingSignature(): String {
        val transform = currentTransformSettings()
        val speed = SpeedCompiler.compile(transform)?.multiplier ?: 1f
        val freezeMs = FreezeCompiler.compile(transform)?.durationMs ?: 0L
        return "${speed.toRawBits()}:$freezeMs"
    }

    private fun reconcileTargetDurationForTimingChange() {
        val target = targetDurationMs ?: return
        if (!adaptiveApplied || adaptiveDraftRanges.isEmpty()) return
        val signature = currentTargetDurationTimingSignature()
        if (signature == targetDurationTimingSignature) return
        if (!applyTargetDurationPlan(target, resetCandidate = false)) {
            adaptiveApplied = false
            targetDurationTimingSignature = null
            if (editor.adaptiveApplySwitch.isChecked) {
                editor.adaptiveApplySwitch.isChecked = false
            }
            if (::targetDurationClipsController.isInitialized) {
                targetDurationClipsController.showImpossibleTarget()
            }
            renderAdaptiveCutControls()
            renderTargetDurationClipsControls()
        }
    }

    private fun clearTargetDurationMode(clearFields: Boolean) {
        targetDurationMs = null
        targetDurationTimingSignature = null
        if (::targetDurationClipsController.isInitialized) {
            if (clearFields) targetDurationClipsController.setTargetDurationMs(null)
            renderTargetDurationClipsControls()
        }
    }

'''
main = replace_once(
    main,
    "    private fun bindAdaptiveCutControls() {\n",
    helpers + "    private fun bindAdaptiveCutControls() {\n",
    "Target Duration helper block",
)

# Fixed pacing is now secondary. Choosing/generating it explicitly leaves target mode.
main = replace_once(
    main,
    "            if (adaptivePreset != selected) {\n"
    "                val wasApplied = adaptiveApplied\n",
    "            if (adaptivePreset != selected) {\n"
    "                clearTargetDurationMode(clearFields = true)\n"
    "                val wasApplied = adaptiveApplied\n",
    "Preset leaves target mode",
)
main = replace_once(
    main,
    "        editor.generateAdaptiveDraftButton.setOnClickListener {\n"
    "            val info = activeMediaInfo ?: return@setOnClickListener\n"
    "            cancelAdaptivePreview()\n"
    "            adaptiveDraftRanges = AdaptiveCutDraftEngine.generate(\n",
    "        editor.generateAdaptiveDraftButton.setOnClickListener {\n"
    "            val info = activeMediaInfo ?: return@setOnClickListener\n"
    "            cancelAdaptivePreview()\n"
    "            clearTargetDurationMode(clearFields = true)\n"
    "            adaptiveDraftRanges = AdaptiveCutDraftEngine.generate(\n",
    "Preset Generate leaves target mode",
)
main = replace_once(
    main,
    "        editor.adaptiveClearButton.setOnClickListener {\n"
    "            val wasApplied = adaptiveApplied\n"
    "            cancelAdaptivePreview()\n",
    "        editor.adaptiveClearButton.setOnClickListener {\n"
    "            val wasApplied = adaptiveApplied\n"
    "            cancelAdaptivePreview()\n"
    "            clearTargetDurationMode(clearFields = true)\n",
    "Adaptive Clear clears target mode",
)

# Any source/draft reset retires a target that was bound to the previous ranges.
main = replace_once(
    main,
    "    private fun clearAdaptiveDraft() {\n"
    "        cancelAdaptivePreview()\n"
    "        adaptiveDraftRanges = emptyList()\n",
    "    private fun clearAdaptiveDraft() {\n"
    "        cancelAdaptivePreview()\n"
    "        clearTargetDurationMode(clearFields = true)\n"
    "        adaptiveDraftRanges = emptyList()\n",
    "clearAdaptiveDraft target reset",
)

# Canonical ranges/settings now carry target mode and target value everywhere.
main = replace_once(
    main,
    "        return AdaptiveCutCompiler.compile(\n"
    "            AdaptiveCutSettings(\n"
    "                enabled = adaptiveApplied,\n"
    "                preset = adaptivePreset,\n"
    "                reviewedRanges = adaptiveDraftRanges,\n"
    "            ),\n"
    "            trim,\n"
    "        ) ?: listOf(trim)\n",
    "        return AdaptiveCutCompiler.compile(\n"
    "            currentAdaptiveCutSettings(),\n"
    "            trim,\n"
    "        ) ?: listOf(trim)\n",
    "currentSelectedClipRanges canonical settings",
)
main = replace_once(
    main,
    "            adaptiveCuts = AdaptiveCutSettings(\n"
    "                enabled = adaptiveApplied,\n"
    "                preset = adaptivePreset,\n"
    "                reviewedRanges = adaptiveDraftRanges,\n"
    "            ),\n",
    "            adaptiveCuts = currentAdaptiveCutSettings(),\n",
    "currentEditPlan canonical settings",
)

# Crossfade is part of the same duration authority. Reconcile target after a boundary edit.
main = replace_once(
    main,
    "        cancelFreezePreview()\n"
    "        cancelAdaptivePreview()\n"
    "        clipTransitionEditorController.reconcile()\n",
    "        cancelFreezePreview()\n"
    "        cancelAdaptivePreview()\n"
    "        val target = targetDurationMs\n"
    "        if (target != null && adaptiveApplied && adaptiveDraftRanges.isNotEmpty()) {\n"
    "            if (!applyTargetDurationPlan(target, resetCandidate = false)) {\n"
    "                adaptiveApplied = false\n"
    "                targetDurationTimingSignature = null\n"
    "                if (editor.adaptiveApplySwitch.isChecked) {\n"
    "                    editor.adaptiveApplySwitch.isChecked = false\n"
    "                }\n"
    "                targetDurationClipsController.showImpossibleTarget()\n"
    "            }\n"
    "        }\n"
    "        clipTransitionEditorController.reconcile()\n",
    "Crossfade target reconciliation",
)
main = replace_once(
    main,
    "        updateTrimSummary()\n"
    "    }\n\n"
    "    private fun previewClipTransitionBoundary(\n",
    "        renderTargetDurationClipsControls()\n"
    "        updateTrimSummary()\n"
    "    }\n\n"
    "    private fun previewClipTransitionBoundary(\n",
    "Crossfade target render refresh",
)

# Internal Trim boundary is always full source. Old saved head/tail values are ignored.
old_configure_trim = '''    private fun configureTrim(info: MediaInfo) {
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
'''
new_configure_trim = '''    private fun configureTrim(info: MediaInfo) {
        val maxSeconds = (info.durationMs / 1_000f).coerceAtLeast(1f)
        editor.trimRangeSlider.valueFrom = 0f
        editor.trimRangeSlider.valueTo = maxSeconds
        editor.trimRangeSlider.values = listOf(0f, maxSeconds)
        restoredTrimStartMs = null
        restoredTrimEndMs = null
        updateTrimSummary()
        renderTargetDurationClipsControls()
    }
'''
main = replace_once(main, old_configure_trim, new_configure_trim, "full-source internal Trim")

# Only Speed / Freeze effective timing changes regenerate target ranges; color/mirror/zoom do not.
main = replace_once(
    main,
    "    private fun onUserChangedTransform() {\n"
    "        scheduleEditorPreferencesSave()\n"
    "        val info = activeMediaInfo ?: return\n"
    "        cancelFreezePreview()\n"
    "        cancelAdaptivePreview()\n",
    "    private fun onUserChangedTransform() {\n"
    "        scheduleEditorPreferencesSave()\n"
    "        val info = activeMediaInfo ?: return\n"
    "        cancelFreezePreview()\n"
    "        cancelAdaptivePreview()\n"
    "        reconcileTargetDurationForTimingChange()\n",
    "Speed/Freeze target reconciliation",
)
main = replace_once(
    main,
    "        renderAdaptiveCutControls()\n"
    "        updateTrimSummary()\n"
    "    }\n\n"
    "    private fun previewIntroFreeze() {\n",
    "        renderAdaptiveCutControls()\n"
    "        renderTargetDurationClipsControls()\n"
    "        updateTrimSummary()\n"
    "    }\n\n"
    "    private fun previewIntroFreeze() {\n",
    "Transform target render refresh",
)

# Adaptive change path refreshes the primary Target Duration panel as well.
main = replace_once(
    main,
    "        renderAdaptiveCutControls()\n"
    "        renderTransformControls()\n"
    "        updateTrimSummary()\n"
    "    }\n\n"
    "    private fun clearAdaptiveDraft() {\n",
    "        renderAdaptiveCutControls()\n"
    "        renderTransformControls()\n"
    "        renderTargetDurationClipsControls()\n"
    "        updateTrimSummary()\n"
    "    }\n\n"
    "    private fun clearAdaptiveDraft() {\n",
    "Adaptive target render refresh",
)

# Restored target plans are reconciled when their source is ready; otherwise just render the panel.
main = replace_once(
    main,
    "        if (::clipTransitionEditorController.isInitialized) {\n"
    "            clipTransitionEditorController.reconcile()\n"
    "        }\n\n"
    "        if (previewPath != info.workingFilePath) {\n",
    "        if (::clipTransitionEditorController.isInitialized) {\n"
    "            clipTransitionEditorController.reconcile()\n"
    "        }\n"
    "        val restoredTarget = targetDurationMs\n"
    "        if (restoredTarget != null && adaptiveApplied && adaptiveDraftRanges.isNotEmpty()) {\n"
    "            if (!applyTargetDurationPlan(restoredTarget, resetCandidate = false)) {\n"
    "                adaptiveApplied = false\n"
    "                targetDurationTimingSignature = null\n"
    "                if (editor.adaptiveApplySwitch.isChecked) {\n"
    "                    editor.adaptiveApplySwitch.isChecked = false\n"
    "                }\n"
    "                targetDurationClipsController.showImpossibleTarget()\n"
    "            }\n"
    "        }\n"
    "        renderTargetDurationClipsControls()\n\n"
    "        if (previewPath != info.workingFilePath) {\n",
    "renderReady target reconciliation",
)

# Hidden Trim must never become an invisible Export-duration mutation.
main = replace_once(
    main,
    "        } else {\n"
    "            editor.trimRangeSlider.values = listOf(\n"
    "                update.trimRange.startMs / 1_000f,\n"
    "                update.trimRange.endMs / 1_000f,\n"
    "            )\n"
    "            onUserChangedTrim()\n"
    "        }\n\n"
    "        Snackbar.make(\n",
    "        } else {\n"
    "            Snackbar.make(\n"
    "                binding.mainRoot,\n"
    "                R.string.export_duration_update_unavailable,\n"
    "                Snackbar.LENGTH_SHORT,\n"
    "            ).show()\n"
    "            renderDurationFitAdvisor()\n"
    "            return\n"
    "        }\n\n"
    "        Snackbar.make(\n",
    "Disable hidden Trim duration-fit fallback",
)

# Persist target duration through Activity recreation.
main = replace_once(
    main,
    "        outState.putString(KEY_ADAPTIVE_PRESET, adaptivePreset.name)\n",
    "        outState.putString(KEY_ADAPTIVE_PRESET, adaptivePreset.name)\n"
    "        targetDurationMs?.let { outState.putLong(KEY_TARGET_DURATION_MS, it) }\n",
    "Target Duration save",
)
main = replace_once(
    main,
    "        private const val KEY_ADAPTIVE_PRESET = \"recapflow.adaptive.preset\"\n",
    "        private const val KEY_ADAPTIVE_PRESET = \"recapflow.adaptive.preset\"\n"
    "        private const val KEY_TARGET_DURATION_MS = \"recapflow.adaptive.targetDurationMs\"\n",
    "Target Duration state key",
)

# Preference/UI sync should redraw the target panel too.
main = replace_once(
    main,
    "        renderAdaptiveCutControls()\n"
    "        renderExportQualityControls()\n",
    "        renderAdaptiveCutControls()\n"
    "        renderTargetDurationClipsControls()\n"
    "        renderExportQualityControls()\n",
    "Preference target render refresh",
)

MAIN.write_text(main, encoding="utf-8")

# Replace obsolete user-facing Trim framing without deleting internal legacy strings/types.
en = require(EN)
en = replace_once(
    en,
    '    <string name="review_editor_clips_tab">CLIPS • TRIM</string>\n',
    '    <string name="review_editor_clips_tab">CLIPS • TARGET</string>\n',
    "English Clips badge",
)
en = replace_once(
    en,
    '    <string name="review_editor_subtitle">Choose the source range to keep. Reset means no trim.</string>\n',
    '    <string name="review_editor_subtitle">Choose the final duration, generate a distributed clip plan, then review the selected clips.</string>\n',
    "English Clips subtitle",
)
EN.write_text(en, encoding="utf-8")

my = require(MY)
my = replace_once(
    my,
    '    <string name="review_editor_clips_tab">ကလစ်များ • ဖြတ်ရွေး</string>\n',
    '    <string name="review_editor_clips_tab">ကလစ်များ • လိုချင်တဲ့ကြာချိန်</string>\n',
    "Myanmar Clips badge",
)
my = replace_once(
    my,
    '    <string name="review_editor_subtitle">သိမ်းချင်တဲ့ ဗီဒီယိုအပိုင်းကို ရွေးပါ။ မဖြတ်ချင်ရင် ဗီဒီယိုအပြည့်ကို ပြန်ရွေးပါ။</string>\n',
    '    <string name="review_editor_subtitle">လိုချင်တဲ့ နောက်ဆုံးကြာချိန်ကို သတ်မှတ်ပြီး ဗီဒီယိုတစ်လျှောက် ကလစ်အစီအစဉ် ဖန်တီးပါ။ ပြီးရင် ရွေးထားတဲ့ကလစ်တွေကို စစ်ဆေးပါ။</string>\n',
    "Myanmar Clips subtitle",
)
MY.write_text(my, encoding="utf-8")

print("Phase 6H.1F Target Duration editor/UI integration applied.")
print("Next: run Myanmar localization verifier, git diff --check, unit tests, and assembleDebug.")
