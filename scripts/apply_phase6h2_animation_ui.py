#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
STRINGS_EN = ROOT / "app/src/main/res/values/strings.xml"
STRINGS_MY = ROOT / "app/src/main/res/values-my/strings_core.xml"
MARKER = "PHASE6H2_ANIMATION_UI"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"FAIL: {label}: expected one anchor, found {count}")
    return text.replace(old, new, 1)


def replace_string_resource(text: str, name: str, value: str, label: str) -> str:
    pattern = re.compile(rf'<string name="{re.escape(name)}">.*?</string>')
    replacement = f'<string name="{name}">{value}</string>'
    updated, count = pattern.subn(replacement, text, count=1)
    if count != 1:
        raise SystemExit(f"FAIL: {label}: resource {name} not found exactly once")
    return updated


main = MAIN.read_text()
if MARKER in main:
    print("PASS: Phase 6H.2 animation UI already applied.")
    raise SystemExit(0)

main = replace_once(
    main,
    "import com.recapflow.ai.media.edit.ImageOverlayAsset\n",
    "import com.recapflow.ai.media.edit.ImageOverlayAsset\n"
    "import com.recapflow.ai.media.edit.ImageOverlayAnimationPolicy\n"
    "import com.recapflow.ai.media.edit.ImageOverlayAnimationPreset\n"
    "import com.recapflow.ai.media.edit.ImageOverlayAnimationSettings\n",
    "animation model imports",
)
main = replace_once(
    main,
    "import com.recapflow.ai.ui.ClipTransitionEditorController\n",
    "import com.recapflow.ai.ui.ClipTransitionEditorController\n"
    "import com.recapflow.ai.ui.ImageOverlayAnimationController\n",
    "animation controller import",
)
main = replace_once(
    main,
    "    private lateinit var targetDurationClipsController: TargetDurationClipsController\n",
    "    private lateinit var targetDurationClipsController: TargetDurationClipsController\n"
    "    private lateinit var imageOverlayAnimationController: ImageOverlayAnimationController\n",
    "controller field",
)
main = replace_once(
    main,
    "    private var imageOverlayOpacity = OverlayCompiler.DEFAULT_IMAGE_OPACITY\n"
    "    private var imageOverlayStartMs = 0L\n",
    "    private var imageOverlayOpacity = OverlayCompiler.DEFAULT_IMAGE_OPACITY\n"
    "    // PHASE6H2_ANIMATION_UI: source-time animation semantics feed live preview + final export.\n"
    "    private var imageOverlayAnimation = ImageOverlayAnimationSettings()\n"
    "    private var imageOverlayStartMs = 0L\n",
    "animation editor state",
)

restore_anchor = (
    "        imageOverlayRangeInitialized = savedInstanceState?.let {\n"
    "            it.containsKey(KEY_IMAGE_OVERLAY_START_MS) &&\n"
)
restore_block = (
    "        val restoredImageAnimationDurationMs = savedInstanceState\n"
    "            ?.takeIf { it.containsKey(KEY_IMAGE_OVERLAY_ANIMATION_DURATION_MS) }\n"
    "            ?.getLong(KEY_IMAGE_OVERLAY_ANIMATION_DURATION_MS)\n"
    "            ?.coerceIn(\n"
    "                ImageOverlayAnimationPolicy.MIN_DURATION_MS,\n"
    "                ImageOverlayAnimationPolicy.MAX_DURATION_MS,\n"
    "            )\n"
    "            ?: ImageOverlayAnimationPolicy.DEFAULT_DURATION_MS\n"
    "        val restoredImageAnimationPeriodMs = savedInstanceState\n"
    "            ?.takeIf { it.containsKey(KEY_IMAGE_OVERLAY_ANIMATION_PERIOD_MS) }\n"
    "            ?.getLong(KEY_IMAGE_OVERLAY_ANIMATION_PERIOD_MS)\n"
    "            ?.coerceIn(\n"
    "                maxOf(ImageOverlayAnimationPolicy.MIN_PERIOD_MS, restoredImageAnimationDurationMs),\n"
    "                ImageOverlayAnimationPolicy.MAX_PERIOD_MS,\n"
    "            )\n"
    "            ?: maxOf(\n"
    "                ImageOverlayAnimationPolicy.DEFAULT_PERIOD_MS,\n"
    "                restoredImageAnimationDurationMs,\n"
    "            )\n"
    "        imageOverlayAnimation = ImageOverlayAnimationSettings(\n"
    "            preset = savedInstanceState?.getString(KEY_IMAGE_OVERLAY_ANIMATION_PRESET)\n"
    "                ?.let { savedName ->\n"
    "                    ImageOverlayAnimationPreset.entries.firstOrNull { it.name == savedName }\n"
    "                }\n"
    "                ?: ImageOverlayAnimationPreset.NONE,\n"
    "            loopEnabled = savedInstanceState\n"
    "                ?.getBoolean(KEY_IMAGE_OVERLAY_ANIMATION_LOOP_ENABLED)\n"
    "                ?: false,\n"
    "            durationMs = restoredImageAnimationDurationMs,\n"
    "            periodMs = restoredImageAnimationPeriodMs,\n"
    "        )\n"
)
main = replace_once(main, restore_anchor, restore_block + restore_anchor, "saved-state restore")

bind_anchor = (
    "        editor.imageOverlayOpacitySlider.value = imageOverlayOpacity * 100f\n"
    "        renderOverlayControls()\n"
)
bind_block = (
    "        editor.imageOverlayOpacitySlider.value = imageOverlayOpacity * 100f\n"
    "        if (!::imageOverlayAnimationController.isInitialized) {\n"
    "            imageOverlayAnimationController = ImageOverlayAnimationController(\n"
    "                container = editor.imageOverlayControlsGroup,\n"
    "            ) { settings ->\n"
    "                if (imageOverlayAnimation != settings) {\n"
    "                    imageOverlayAnimation = settings.copy(phaseOffsetMs = 0L)\n"
    "                    renderImageOverlayControls()\n"
    "                    onUserChangedOverlay(\n"
    "                        throttleSourceBlurPreview = true,\n"
    "                        reason = \"image animation controls\",\n"
    "                    )\n"
    "                }\n"
    "            }\n"
    "        }\n"
    "        renderOverlayControls()\n"
)
main = replace_once(main, bind_anchor, bind_block, "controller binding")

main = replace_once(
    main,
    "            imageOverlayOpacity = OverlayCompiler.DEFAULT_IMAGE_OPACITY\n"
    "            imageOverlayRangeInitialized = false\n",
    "            imageOverlayOpacity = OverlayCompiler.DEFAULT_IMAGE_OPACITY\n"
    "            imageOverlayAnimation = ImageOverlayAnimationSettings()\n"
    "            imageOverlayRangeInitialized = false\n",
    "reset image controls",
)

render_anchor = (
    "        editor.imageOverlayOpacitySlider.value = imageOverlayOpacity * 100f\n"
    "        val preset = ImageOverlayPositionPreset.entries.firstOrNull {\n"
)
render_block = (
    "        editor.imageOverlayOpacitySlider.value = imageOverlayOpacity * 100f\n"
    "        if (::imageOverlayAnimationController.isInitialized) {\n"
    "            val renderActive = ::renderCoordinator.isInitialized &&\n"
    "                renderCoordinator.currentState.isActiveRender()\n"
    "            val animationVisible = overlayEnabled && imageOverlayEnabled\n"
    "            imageOverlayAnimationController.render(\n"
    "                settings = imageOverlayAnimation,\n"
    "                visible = animationVisible,\n"
    "                enabled = animationVisible && !renderActive && !imageOverlayImporting,\n"
    "            )\n"
    "        }\n"
    "        val preset = ImageOverlayPositionPreset.entries.firstOrNull {\n"
)
main = replace_once(main, render_anchor, render_block, "animation control rendering")

main = replace_once(
    main,
    "                    imageOpacity = imageOverlayOpacity,\n"
    "                ),\n",
    "                    imageOpacity = imageOverlayOpacity,\n"
    "                    imageAnimationPreset = imageOverlayAnimation.preset,\n"
    "                    imageAnimationLoopEnabled = imageOverlayAnimation.loopEnabled,\n"
    "                    imageAnimationDurationMs = imageOverlayAnimation.durationMs,\n"
    "                    imageAnimationPeriodMs = imageOverlayAnimation.periodMs,\n"
    "                ),\n",
    "preference snapshot",
)
main = replace_once(
    main,
    "        imageOverlayOpacity = snapshot.overlay.imageOpacity\n"
    "        imageOverlayEnabled = snapshot.overlay.imageEnabled &&\n",
    "        imageOverlayOpacity = snapshot.overlay.imageOpacity\n"
    "        imageOverlayAnimation = ImageOverlayAnimationSettings(\n"
    "            preset = snapshot.overlay.imageAnimationPreset,\n"
    "            loopEnabled = snapshot.overlay.imageAnimationLoopEnabled,\n"
    "            durationMs = snapshot.overlay.imageAnimationDurationMs,\n"
    "            periodMs = snapshot.overlay.imageAnimationPeriodMs,\n"
    "        )\n"
    "        imageOverlayEnabled = snapshot.overlay.imageEnabled &&\n",
    "preference restore",
)

settings_pattern = re.compile(
    r"(image = ImageOverlaySettings\(\n"
    r"\s+enabled = imageOverlayEnabled,\n"
    r"\s+asset = imageOverlayAsset,\n"
    r"\s+centerX = imageOverlayCenterX,\n"
    r"\s+centerY = imageOverlayCenterY,\n"
    r"\s+widthFraction = imageOverlayWidthFraction,\n"
    r"\s+opacity = imageOverlayOpacity,\n)"
)
main, count = settings_pattern.subn(r"\1            animation = imageOverlayAnimation,\n", main, count=1)
if count != 1:
    raise SystemExit(f"FAIL: EditPlan image animation: expected one settings block, found {count}")

save_anchor = "        outState.putFloat(KEY_IMAGE_OVERLAY_OPACITY, imageOverlayOpacity)\n"
save_block = (
    save_anchor
    + "        outState.putString(KEY_IMAGE_OVERLAY_ANIMATION_PRESET, imageOverlayAnimation.preset.name)\n"
    + "        outState.putBoolean(\n"
    + "            KEY_IMAGE_OVERLAY_ANIMATION_LOOP_ENABLED,\n"
    + "            imageOverlayAnimation.loopEnabled,\n"
    + "        )\n"
    + "        outState.putLong(\n"
    + "            KEY_IMAGE_OVERLAY_ANIMATION_DURATION_MS,\n"
    + "            imageOverlayAnimation.durationMs,\n"
    + "        )\n"
    + "        outState.putLong(\n"
    + "            KEY_IMAGE_OVERLAY_ANIMATION_PERIOD_MS,\n"
    + "            imageOverlayAnimation.periodMs,\n"
    + "        )\n"
)
main = replace_once(main, save_anchor, save_block, "saved-state write")

key_anchor = (
    '        private const val KEY_IMAGE_OVERLAY_OPACITY = "recapflow.overlay.image.opacity"\n'
)
key_block = (
    key_anchor
    + '        private const val KEY_IMAGE_OVERLAY_ANIMATION_PRESET = "recapflow.overlay.image.animation.preset"\n'
    + '        private const val KEY_IMAGE_OVERLAY_ANIMATION_LOOP_ENABLED = "recapflow.overlay.image.animation.loop"\n'
    + '        private const val KEY_IMAGE_OVERLAY_ANIMATION_DURATION_MS = "recapflow.overlay.image.animation.durationMs"\n'
    + '        private const val KEY_IMAGE_OVERLAY_ANIMATION_PERIOD_MS = "recapflow.overlay.image.animation.periodMs"\n'
)
main = replace_once(main, key_anchor, key_block, "saved-state keys")

MAIN.write_text(main)

english = STRINGS_EN.read_text()
english = replace_string_resource(
    english,
    "image_overlay_instruction",
    "Add one logo or image. Position, size, opacity, active time, and optional animation are previewed live and exported through the same effect graph.",
    "English image instruction",
)
english = replace_string_resource(
    english,
    "image_overlay_preview_note",
    "PNG transparency is preserved. Realtime preview and 720p/1080p export share the same geometry, opacity, source-time range, and animation timing.",
    "English preview note",
)
STRINGS_EN.write_text(english)

myanmar = STRINGS_MY.read_text()
myanmar = replace_string_resource(
    myanmar,
    "image_overlay_instruction",
    "Logo သို့မဟုတ် image တစ်ပုံထည့်နိုင်ပါတယ်။ နေရာ၊ အရွယ်အစား၊ opacity၊ အသုံးပြုချိန်နဲ့ animation ကို live preview မှာ ကြည့်ပြီး effect graph တစ်ခုတည်းနဲ့ export လုပ်ပါတယ်။",
    "Myanmar image instruction",
)
myanmar = replace_string_resource(
    myanmar,
    "image_overlay_preview_note",
    "PNG transparency ကို ထိန်းထားပါတယ်။ Realtime preview နဲ့ 720p/1080p export က geometry၊ opacity၊ source-time range နဲ့ animation timing တစ်ခုတည်းကို သုံးပါတယ်။",
    "Myanmar preview note",
)
STRINGS_MY.write_text(myanmar)

print("PASS: Phase 6H.2 animation UI integration applied to canonical source files.")
