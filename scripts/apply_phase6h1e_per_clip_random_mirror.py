#!/usr/bin/env python3
"""Apply Phase 6H.1E — deterministic Per-Clip Random Mirror.

This helper is intentionally idempotent. It patches the reviewed 6H.1 branch without
rendering intermediate media. Preview/export continue to share Media3 Composition.

Run from repo root on branch feature/phase-6h1e-random-mirror:
    python3 scripts/apply_phase6h1e_per_clip_random_mirror.py
"""

from __future__ import annotations

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]

EDIT_PLAN = ROOT / "app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlan.kt"
COMPILER = ROOT / "app/src/main/kotlin/com/recapflow/ai/media/render/Media3CompositionCompiler.kt"
MAIN = ROOT / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
PREFS = ROOT / "app/src/main/kotlin/com/recapflow/ai/preferences/EditorPreferencesStore.kt"
LAYOUT = ROOT / "app/src/main/res/layout/view_editor_destination.xml"
STRINGS = ROOT / "app/src/main/res/values/strings.xml"
POLICY = ROOT / "app/src/main/kotlin/com/recapflow/ai/media/edit/PerClipMirrorPolicy.kt"
TEST = ROOT / "app/src/test/kotlin/com/recapflow/ai/media/edit/PerClipMirrorPolicyTest.kt"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one anchor, found {count}")
    return text.replace(old, new, 1)


def write_if_changed(path: Path, text: str) -> bool:
    old = path.read_text(encoding="utf-8") if path.exists() else None
    if old == text:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    return True


def patch_edit_plan() -> bool:
    text = EDIT_PLAN.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "    val mirrorEnabled: Boolean = false,\n    val color: ColorSettings = ColorSettings(),",
        "    val mirrorEnabled: Boolean = false,\n    val randomMirrorPerClipEnabled: Boolean = false,\n    val color: ColorSettings = ColorSettings(),",
        "EditPlan random mirror field",
    )
    return write_if_changed(EDIT_PLAN, text)


def policy_source() -> str:
    return '''package com.recapflow.ai.media.edit

/**
 * Resolves the visual Mirror choice for each reviewed clip.
 *
 * "Random" is deterministic by design: the same EditPlan + source ranges always produce the
 * same mirror decisions so CompositionPlayer preview and Transformer export cannot disagree.
 * The global Mirror toggle remains unchanged. Random-per-clip mode is a separate, mutually
 * exclusive UI choice and only becomes active when the timeline contains at least two clips.
 */
object PerClipMirrorPolicy {

    fun resolvedSettings(
        settings: TransformSettings,
        range: TrimRange,
        rangeIndex: Int,
        clipCount: Int,
    ): TransformSettings {
        val resolvedMirror = shouldMirror(settings, range, rangeIndex, clipCount)
        return settings.copy(
            mirrorEnabled = resolvedMirror,
            randomMirrorPerClipEnabled = false,
        )
    }

    fun shouldMirror(
        settings: TransformSettings,
        range: TrimRange,
        rangeIndex: Int,
        clipCount: Int,
    ): Boolean {
        if (!settings.enabled) return false
        if (!settings.randomMirrorPerClipEnabled) return settings.mirrorEnabled
        if (clipCount < 2) return false
        return (stableMix(range, rangeIndex) and 1L) == 0L
    }

    private fun stableMix(range: TrimRange, rangeIndex: Int): Long {
        var value = range.startMs * 31L
        value = value xor (range.endMs * 17L)
        value = value xor (rangeIndex.toLong() * GOLDEN_GAMMA)
        value = (value xor (value ushr 30)) * MIX_1
        value = (value xor (value ushr 27)) * MIX_2
        return value xor (value ushr 31)
    }

    private const val GOLDEN_GAMMA = -7046029254386353131L
    private const val MIX_1 = -4658895280553007687L
    private const val MIX_2 = -7723592293110705685L
}
'''


def test_source() -> str:
    return '''package com.recapflow.ai.media.edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PerClipMirrorPolicyTest {

    @Test
    fun transformOffAlwaysOmitsMirror() {
        val settings = TransformSettings(
            enabled = false,
            mirrorEnabled = true,
            randomMirrorPerClipEnabled = true,
        )
        assertFalse(
            PerClipMirrorPolicy.shouldMirror(settings, TrimRange(0L, 3_000L), 0, 4),
        )
    }

    @Test
    fun globalMirrorKeepsExistingBehavior() {
        val settings = TransformSettings(enabled = true, mirrorEnabled = true)
        assertTrue(
            PerClipMirrorPolicy.shouldMirror(settings, TrimRange(3_000L, 6_000L), 1, 4),
        )
    }

    @Test
    fun randomModeIsInactiveForSingleClip() {
        val settings = TransformSettings(enabled = true, randomMirrorPerClipEnabled = true)
        assertFalse(
            PerClipMirrorPolicy.shouldMirror(settings, TrimRange(0L, 5_000L), 0, 1),
        )
    }

    @Test
    fun randomModeIsDeterministicForPreviewAndExport() {
        val settings = TransformSettings(enabled = true, randomMirrorPerClipEnabled = true)
        val ranges = listOf(
            TrimRange(0L, 3_000L),
            TrimRange(4_000L, 7_000L),
            TrimRange(8_000L, 11_000L),
            TrimRange(12_000L, 15_000L),
            TrimRange(16_000L, 19_000L),
            TrimRange(20_000L, 23_000L),
        )
        val first = ranges.mapIndexed { index, range ->
            PerClipMirrorPolicy.shouldMirror(settings, range, index, ranges.size)
        }
        val second = ranges.mapIndexed { index, range ->
            PerClipMirrorPolicy.shouldMirror(settings, range, index, ranges.size)
        }
        assertEquals(first, second)
        assertTrue(first.any { it })
        assertTrue(first.any { !it })
    }

    @Test
    fun resolvedSettingsConsumesRandomModeIntoPerItemMirror() {
        val settings = TransformSettings(enabled = true, randomMirrorPerClipEnabled = true)
        val resolved = PerClipMirrorPolicy.resolvedSettings(
            settings = settings,
            range = TrimRange(4_000L, 7_000L),
            rangeIndex = 1,
            clipCount = 3,
        )
        assertFalse(resolved.randomMirrorPerClipEnabled)
        assertEquals(
            PerClipMirrorPolicy.shouldMirror(settings, TrimRange(4_000L, 7_000L), 1, 3),
            resolved.mirrorEnabled,
        )
    }
}
'''


def patch_compiler() -> bool:
    text = COMPILER.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "import com.recapflow.ai.media.edit.OverlaySettings\n",
        "import com.recapflow.ai.media.edit.OverlaySettings\nimport com.recapflow.ai.media.edit.PerClipMirrorPolicy\n",
        "compiler import",
    )
    text = replace_once(
        text,
        "            plan.selectedRanges.forEach { range ->",
        "            plan.selectedRanges.forEachIndexed { rangeIndex, range ->",
        "sequential range index",
    )
    text = replace_once(
        text,
        "                        crossfadeSlot = null,\n                    ),",
        "                        crossfadeSlot = null,\n                        rangeIndex = rangeIndex,\n                    ),",
        "sequential range index arg",
    )
    text = replace_once(
        text,
        "                    crossfadeSlot = slot,\n                ),",
        "                    crossfadeSlot = slot,\n                    rangeIndex = slot.rangeIndex,\n                ),",
        "crossfade range index arg",
    )
    text = replace_once(
        text,
        "        crossfadeSlot: Media3CrossfadeClipSlot?,\n    ): EditedMediaItem {\n        val speedEffects = if (forCompositionPreview) {",
        "        crossfadeSlot: Media3CrossfadeClipSlot?,\n        rangeIndex: Int,\n    ): EditedMediaItem {\n        val rangeTransformSettings = PerClipMirrorPolicy.resolvedSettings(\n            settings = editPlan.transform,\n            range = range,\n            rangeIndex = rangeIndex,\n            clipCount = plan.selectedRanges.size,\n        )\n        val speedEffects = if (forCompositionPreview) {",
        "range settings resolution",
    )

    start = text.index("    private fun buildEditedVideoItem(")
    end = text.index("    private fun buildFreezeItem(", start)
    block = text[start:end]
    block2 = block.replace("settings = editPlan.transform,", "settings = rangeTransformSettings,")
    if block2 == block and "settings = rangeTransformSettings," not in block:
        raise RuntimeError("compiler visual settings anchors were not found")
    text = text[:start] + block2 + text[end:]

    # Freeze must use the same mirror decision as the first moving clip.
    text = replace_once(
        text,
        "                    settings = frozenVisualSettings(editPlan.transform),",
        "                    settings = frozenVisualSettings(\n                        PerClipMirrorPolicy.resolvedSettings(\n                            settings = editPlan.transform,\n                            range = (AdaptiveCutCompiler.compile(\n                                editPlan.adaptiveCuts,\n                                editPlan.trimRange,\n                            ) ?: listOf(editPlan.trimRange)).first(),\n                            rangeIndex = 0,\n                            clipCount = (AdaptiveCutCompiler.compile(\n                                editPlan.adaptiveCuts,\n                                editPlan.trimRange,\n                            ) ?: listOf(editPlan.trimRange)).size,\n                        ),\n                    ),",
        "freeze first clip mirror",
    )
    text = replace_once(
        text,
        "import com.recapflow.ai.media.edit.AudioCompiler\n",
        "import com.recapflow.ai.media.edit.AdaptiveCutCompiler\nimport com.recapflow.ai.media.edit.AudioCompiler\n",
        "adaptive compiler import",
    )
    return write_if_changed(COMPILER, text)


def patch_preferences() -> bool:
    text = PREFS.read_text(encoding="utf-8")
    text = replace_once(
        text,
        '.putBoolean(key(prefix, "transform.mirror"), transform.mirrorEnabled)\n',
        '.putBoolean(key(prefix, "transform.mirror"), transform.mirrorEnabled)\n            .putBoolean(\n                key(prefix, "transform.mirror.randomPerClip"),\n                transform.randomMirrorPerClipEnabled,\n            )\n',
        "preference write",
    )
    text = replace_once(
        text,
        '            mirrorEnabled = bool(prefix, "transform.mirror", false),\n            color = color,',
        '            mirrorEnabled = bool(prefix, "transform.mirror", false),\n            randomMirrorPerClipEnabled = bool(\n                prefix,\n                "transform.mirror.randomPerClip",\n                false,\n            ),\n            color = color,',
        "preference read",
    )
    text = text.replace("const val SCHEMA_VERSION = 2", "const val SCHEMA_VERSION = 3")
    return write_if_changed(PREFS, text)


def patch_strings() -> bool:
    text = STRINGS.read_text(encoding="utf-8")
    text = replace_once(
        text,
        '    <string name="transform_mirror_suffix"> • Mirror</string>\n',
        '    <string name="transform_mirror_suffix"> • Mirror</string>\n'
        '    <string name="transform_random_mirror_suffix"> • Random mirror by clip</string>\n',
        "transform random mirror summary suffix",
    )
    text = replace_once(
        text,
        '    <string name="mirror_remembered_summary">Remembered On — turn Transform on to include Mirror.</string>\n',
        '    <string name="mirror_remembered_summary">Remembered On — turn Transform on to include Mirror.</string>\n'
        '    <string name="random_mirror_per_clip_enable">Random mirror each clip</string>\n'
        '    <string name="random_mirror_per_clip_off_summary">Off — reviewed clips keep the normal left/right direction.</string>\n'
        '    <string name="random_mirror_per_clip_on_summary">On — each reviewed clip gets a stable random left/right flip. Preview and export use the same choices.</string>\n'
        '    <string name="random_mirror_per_clip_remembered_summary">Remembered On — turn Transform on and use at least 2 reviewed clips.</string>\n',
        "random mirror strings",
    )
    return write_if_changed(STRINGS, text)


def patch_layout() -> bool:
    text = LAYOUT.read_text(encoding="utf-8")
    if "@+id/randomMirrorPerClipSwitch" in text:
        return False
    pattern = re.compile(
        r'(\s*<TextView\n\s*android:id="@\+id/mirrorSummary".*?\n\s*/>)',
        re.DOTALL,
    )
    match = pattern.search(text)
    if not match:
        raise RuntimeError("layout mirrorSummary block not found")
    indent = "                                        "
    addition = f'''\n\n{indent}<com.google.android.material.materialswitch.MaterialSwitch
{indent}    android:id="@+id/randomMirrorPerClipSwitch"
{indent}    android:layout_width="match_parent"
{indent}    android:layout_height="wrap_content"
{indent}    android:layout_marginTop="14dp"
{indent}    android:minHeight="48dp"
{indent}    android:text="@string/random_mirror_per_clip_enable"
{indent}    android:textAppearance="@style/TextAppearance.Material3.TitleSmall"
{indent}    android:textColor="@color/rf_on_surface" />

{indent}<TextView
{indent}    android:id="@+id/randomMirrorPerClipSummary"
{indent}    android:layout_width="match_parent"
{indent}    android:layout_height="wrap_content"
{indent}    android:layout_marginTop="2dp"
{indent}    android:text="@string/random_mirror_per_clip_off_summary"
{indent}    android:textAppearance="@style/TextAppearance.Material3.BodySmall"
{indent}    android:textColor="@color/rf_on_surface_variant" />'''
    text = text[:match.end()] + addition + text[match.end():]
    return write_if_changed(LAYOUT, text)


def patch_main() -> bool:
    text = MAIN.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "    private var mirrorEnabled = false\n    private var colorEnabled = false",
        "    private var mirrorEnabled = false\n    private var randomMirrorPerClipEnabled = false\n    private var colorEnabled = false",
        "main random mirror state",
    )
    text = replace_once(
        text,
        "        mirrorEnabled = savedInstanceState?.getBoolean(KEY_MIRROR_ENABLED) ?: false\n        colorEnabled = savedInstanceState?.getBoolean(KEY_COLOR_ENABLED) ?: false",
        "        mirrorEnabled = savedInstanceState?.getBoolean(KEY_MIRROR_ENABLED) ?: false\n        randomMirrorPerClipEnabled = savedInstanceState?.getBoolean(\n            KEY_RANDOM_MIRROR_PER_CLIP_ENABLED,\n        ) ?: false\n        colorEnabled = savedInstanceState?.getBoolean(KEY_COLOR_ENABLED) ?: false",
        "main restore state",
    )

    # Both initial binding and render path contain this assignment. Add the new switch after each.
    needle = "        editor.mirrorEnabledSwitch.isChecked = mirrorEnabled\n"
    if "editor.randomMirrorPerClipSwitch.isChecked = randomMirrorPerClipEnabled" not in text:
        count = text.count(needle)
        if count < 2:
            raise RuntimeError(f"main mirror checked anchors expected >=2, found {count}")
        text = text.replace(
            needle,
            needle + "        editor.randomMirrorPerClipSwitch.isChecked = randomMirrorPerClipEnabled\n",
        )

    old_listener = '''        editor.mirrorEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (mirrorEnabled != isChecked) {
                mirrorEnabled = isChecked
                renderTransformControls()
                onUserChangedTransform()
            }
        }
'''
    new_listener = '''        editor.mirrorEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (mirrorEnabled != isChecked) {
                mirrorEnabled = isChecked
                if (isChecked && randomMirrorPerClipEnabled) {
                    randomMirrorPerClipEnabled = false
                    editor.randomMirrorPerClipSwitch.isChecked = false
                }
                renderTransformControls()
                onUserChangedTransform()
            }
        }
        editor.randomMirrorPerClipSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (randomMirrorPerClipEnabled != isChecked) {
                randomMirrorPerClipEnabled = isChecked
                if (isChecked && mirrorEnabled) {
                    mirrorEnabled = false
                    editor.mirrorEnabledSwitch.isChecked = false
                }
                renderTransformControls()
                onUserChangedTransform()
            }
        }
'''
    text = replace_once(text, old_listener, new_listener, "main mirror listeners")

    text = replace_once(
        text,
        "        mirrorEnabled = transform.mirrorEnabled\n        colorEnabled = transform.color.enabled",
        "        mirrorEnabled = transform.mirrorEnabled\n        randomMirrorPerClipEnabled = transform.randomMirrorPerClipEnabled\n        colorEnabled = transform.color.enabled",
        "main apply transform",
    )
    text = replace_once(
        text,
        "        editor.mirrorEnabledSwitch.isEnabled = controlsEnabled\n        editor.colorEnabledSwitch.isEnabled = controlsEnabled",
        "        editor.mirrorEnabledSwitch.isEnabled = controlsEnabled\n        editor.randomMirrorPerClipSwitch.isEnabled = controlsEnabled\n        editor.colorEnabledSwitch.isEnabled = controlsEnabled",
        "main random mirror enabled state",
    )
    text = replace_once(
        text,
        "            if (transformEnabled && mirrorEnabled) append(getString(R.string.transform_mirror_suffix))\n            if (transformEnabled && colorEnabled)",
        "            if (transformEnabled && mirrorEnabled) append(getString(R.string.transform_mirror_suffix))\n            if (transformEnabled && randomMirrorPerClipEnabled) {\n                append(getString(R.string.transform_random_mirror_suffix))\n            }\n            if (transformEnabled && colorEnabled)",
        "main transform summary",
    )
    summary_anchor = '''        editor.mirrorSummary.setText(
            when {
                !mirrorEnabled -> R.string.mirror_off_summary
                transformEnabled -> R.string.mirror_on_summary
                else -> R.string.mirror_remembered_summary
            },
        )
'''
    summary_new = summary_anchor + '''        editor.randomMirrorPerClipSummary.setText(
            when {
                !randomMirrorPerClipEnabled -> R.string.random_mirror_per_clip_off_summary
                transformEnabled -> R.string.random_mirror_per_clip_on_summary
                else -> R.string.random_mirror_per_clip_remembered_summary
            },
        )
'''
    text = replace_once(text, summary_anchor, summary_new, "main random mirror summary")
    text = replace_once(
        text,
        "        mirrorEnabled = mirrorEnabled,\n        color = ColorSettings(",
        "        mirrorEnabled = mirrorEnabled,\n        randomMirrorPerClipEnabled = randomMirrorPerClipEnabled,\n        color = ColorSettings(",
        "main current transform settings",
    )
    text = replace_once(
        text,
        "        outState.putBoolean(KEY_MIRROR_ENABLED, mirrorEnabled)\n        outState.putBoolean(KEY_COLOR_ENABLED, colorEnabled)",
        "        outState.putBoolean(KEY_MIRROR_ENABLED, mirrorEnabled)\n        outState.putBoolean(KEY_RANDOM_MIRROR_PER_CLIP_ENABLED, randomMirrorPerClipEnabled)\n        outState.putBoolean(KEY_COLOR_ENABLED, colorEnabled)",
        "main save state",
    )
    text = replace_once(
        text,
        '        private const val KEY_MIRROR_ENABLED = "recapflow.transform.mirror.enabled"\n        private const val KEY_COLOR_ENABLED',
        '        private const val KEY_MIRROR_ENABLED = "recapflow.transform.mirror.enabled"\n        private const val KEY_RANDOM_MIRROR_PER_CLIP_ENABLED =\n            "recapflow.transform.mirror.randomPerClip.enabled"\n        private const val KEY_COLOR_ENABLED',
        "main state key",
    )
    return write_if_changed(MAIN, text)


def verify() -> None:
    required = {
        EDIT_PLAN: ["randomMirrorPerClipEnabled"],
        COMPILER: ["PerClipMirrorPolicy.resolvedSettings", "rangeIndex = slot.rangeIndex"],
        MAIN: ["randomMirrorPerClipSwitch", "KEY_RANDOM_MIRROR_PER_CLIP_ENABLED"],
        PREFS: ["transform.mirror.randomPerClip", "SCHEMA_VERSION = 3"],
        LAYOUT: ["randomMirrorPerClipSwitch", "randomMirrorPerClipSummary"],
        STRINGS: ["random_mirror_per_clip_enable", "transform_random_mirror_suffix"],
        POLICY: ["object PerClipMirrorPolicy", "stableMix"],
        TEST: ["class PerClipMirrorPolicyTest"],
    }
    for path, tokens in required.items():
        text = path.read_text(encoding="utf-8")
        for token in tokens:
            if token not in text:
                raise RuntimeError(f"verification failed: {token!r} missing from {path}")


def main() -> int:
    required_inputs = [EDIT_PLAN, COMPILER, MAIN, PREFS, LAYOUT, STRINGS]
    missing = [str(path) for path in required_inputs if not path.exists()]
    if missing:
        print("ERROR: missing expected files:", file=sys.stderr)
        for path in missing:
            print(f"- {path}", file=sys.stderr)
        return 2

    changed: list[str] = []
    for path, action in (
        (EDIT_PLAN, patch_edit_plan),
        (COMPILER, patch_compiler),
        (PREFS, patch_preferences),
        (STRINGS, patch_strings),
        (LAYOUT, patch_layout),
        (MAIN, patch_main),
    ):
        if action():
            changed.append(str(path.relative_to(ROOT)))

    if write_if_changed(POLICY, policy_source()):
        changed.append(str(POLICY.relative_to(ROOT)))
    if write_if_changed(TEST, test_source()):
        changed.append(str(TEST.relative_to(ROOT)))

    verify()
    print("Phase 6H.1E Per-Clip Random Mirror patch applied.")
    if changed:
        print("Changed files:")
        for path in changed:
            print(f"- {path}")
    else:
        print("No changes needed; patch is already applied.")
    print("Next: run git diff --check, :app:testDebugUnitTest, then :app:assembleDebug.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
