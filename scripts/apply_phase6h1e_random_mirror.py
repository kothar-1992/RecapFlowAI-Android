#!/usr/bin/env python3
"""Safe launcher for Phase 6H.1E Per-Clip Random Mirror.

Uses the shared helper for all files, but replaces its compiler and layout patches
with scoped implementations that are safe against the reviewed 6H.1 source shape.
"""

from __future__ import annotations

import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HELPER_PATH = ROOT / "scripts/apply_phase6h1e_per_clip_random_mirror.py"

spec = importlib.util.spec_from_file_location("phase6h1e_helper", HELPER_PATH)
if spec is None or spec.loader is None:
    raise SystemExit(f"Unable to load {HELPER_PATH}")
helper = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helper)


def safe_patch_compiler() -> bool:
    text = helper.COMPILER.read_text(encoding="utf-8")
    text = helper.replace_once(
        text,
        "import com.recapflow.ai.media.edit.OverlaySettings\n",
        "import com.recapflow.ai.media.edit.OverlaySettings\n"
        "import com.recapflow.ai.media.edit.PerClipMirrorPolicy\n",
        "compiler import",
    )
    text = helper.replace_once(
        text,
        "import com.recapflow.ai.media.edit.AudioCompiler\n",
        "import com.recapflow.ai.media.edit.AdaptiveCutCompiler\n"
        "import com.recapflow.ai.media.edit.AudioCompiler\n",
        "adaptive compiler import",
    )
    text = helper.replace_once(
        text,
        "            plan.selectedRanges.forEach { range ->",
        "            plan.selectedRanges.forEachIndexed { rangeIndex, range ->",
        "sequential range index",
    )
    text = helper.replace_once(
        text,
        "                        crossfadeSlot = null,\n                    ),",
        "                        crossfadeSlot = null,\n"
        "                        rangeIndex = rangeIndex,\n                    ),",
        "sequential range index arg",
    )
    text = helper.replace_once(
        text,
        "                    crossfadeSlot = slot,\n                ),",
        "                    crossfadeSlot = slot,\n"
        "                    rangeIndex = slot.rangeIndex,\n                ),",
        "crossfade range index arg",
    )
    text = helper.replace_once(
        text,
        "        crossfadeSlot: Media3CrossfadeClipSlot?,\n"
        "    ): EditedMediaItem {\n"
        "        val speedEffects = if (forCompositionPreview) {",
        "        crossfadeSlot: Media3CrossfadeClipSlot?,\n"
        "        rangeIndex: Int,\n"
        "    ): EditedMediaItem {\n"
        "        val rangeTransformSettings = PerClipMirrorPolicy.resolvedSettings(\n"
        "            settings = editPlan.transform,\n"
        "            range = range,\n"
        "            rangeIndex = rangeIndex,\n"
        "            clipCount = plan.selectedRanges.size,\n"
        "        )\n"
        "        val speedEffects = if (forCompositionPreview) {",
        "range settings resolution",
    )

    start = text.index("    private fun buildEditedVideoItem(")
    end = text.index("    private fun buildFreezeItem(", start)
    block = text[start:end]
    speed_start = block.index("        val speedEffects = if (forCompositionPreview) {")
    prefix = block[:speed_start]
    body = block[speed_start:]
    body2 = body.replace("settings = editPlan.transform,", "settings = rangeTransformSettings,")
    if body2 == body and "settings = rangeTransformSettings," not in body:
        raise RuntimeError("compiler per-item visual settings anchors were not found")
    text = text[:start] + prefix + body2 + text[end:]

    text = helper.replace_once(
        text,
        "                    settings = frozenVisualSettings(editPlan.transform),",
        "                    settings = frozenVisualSettings(\n"
        "                        PerClipMirrorPolicy.resolvedSettings(\n"
        "                            settings = editPlan.transform,\n"
        "                            range = (AdaptiveCutCompiler.compile(\n"
        "                                editPlan.adaptiveCuts,\n"
        "                                editPlan.trimRange,\n"
        "                            ) ?: listOf(editPlan.trimRange)).first(),\n"
        "                            rangeIndex = 0,\n"
        "                            clipCount = (AdaptiveCutCompiler.compile(\n"
        "                                editPlan.adaptiveCuts,\n"
        "                                editPlan.trimRange,\n"
        "                            ) ?: listOf(editPlan.trimRange)).size,\n"
        "                        ),\n"
        "                    ),",
        "freeze first clip mirror",
    )
    return helper.write_if_changed(helper.COMPILER, text)


def safe_patch_layout() -> bool:
    text = helper.LAYOUT.read_text(encoding="utf-8")
    if "@+id/randomMirrorPerClipSwitch" in text:
        return False

    marker = 'android:id="@+id/mirrorSummary"'
    marker_index = text.find(marker)
    if marker_index < 0:
        raise RuntimeError("layout mirrorSummary id not found")

    block_start = text.rfind("<TextView", 0, marker_index)
    block_end = text.find("/>", marker_index)
    if block_start < 0 or block_end < 0:
        raise RuntimeError("layout mirrorSummary block boundaries not found")
    block_end += 2

    line_start = text.rfind("\n", 0, block_start) + 1
    indent = text[line_start:block_start]
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
    text = text[:block_end] + addition + text[block_end:]
    return helper.write_if_changed(helper.LAYOUT, text)


helper.patch_compiler = safe_patch_compiler
helper.patch_layout = safe_patch_layout
raise SystemExit(helper.main())
