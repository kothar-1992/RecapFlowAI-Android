#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
EN = ROOT / "app/src/main/res/values/strings.xml"
MY = ROOT / "app/src/main/res/values-my/strings_core.xml"
FOUNDATION_MARKER = "PHASE6H1F_TARGET_DURATION_UI"
MARKER = "PHASE6H1F2_CLIPS_UX_UNIFICATION"


def require(path: Path) -> str:
    if not path.is_file():
        raise SystemExit(f"Missing required file: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 anchor, found {count}")
    return text.replace(old, new, 1)


def replace_string_value(text: str, name: str, value: str) -> str:
    pattern = rf'(<string\s+name="{re.escape(name)}"[^>]*>)(.*?)(</string>)'
    updated, count = re.subn(
        pattern,
        lambda match: f"{match.group(1)}{value}{match.group(3)}",
        text,
        count=1,
        flags=re.DOTALL,
    )
    if count != 1:
        raise SystemExit(f"string {name}: expected exactly 1 entry, found {count}")
    return updated


main = require(MAIN)
if MARKER in main:
    print("Phase 6H.1F.2 Clips UX unification already applied.")
    raise SystemExit(0)
if FOUNDATION_MARKER not in main:
    raise SystemExit(
        "Phase 6H.1F Target Duration UI is not applied to MainActivity.kt. "
        "Run scripts/apply_phase6h1f_target_duration_ui.py first."
    )

# Target Duration is the normal/default Clips workflow. The legacy preset planner remains in code
# for compatibility and a future explicit Advanced planning mode, but must not appear as a second
# simultaneous planner beneath Target Duration.
anchor = """        editor.trimValidationMessage.isVisible = false
        editor.resetTrimButton.isVisible = false

        targetDurationClipsController = TargetDurationClipsController(
"""
replacement = """        editor.trimValidationMessage.isVisible = false
        editor.resetTrimButton.isVisible = false

        // PHASE6H1F2_CLIPS_UX_UNIFICATION: Target Duration is the single normal planning authority.
        // Keep only the shared downstream review controls visible. Preset pacing stays implemented
        // internally until an explicit Advanced planning mode is introduced.
        editor.adaptivePresetGroup.isVisible = false
        editor.generateAdaptiveDraftButton.isVisible = false
        editor.adaptiveApplySwitch.isVisible = false
        editor.adaptiveApplyNote.isVisible = false

        targetDurationClipsController = TargetDurationClipsController(
"""
main = replace_once(main, anchor, replacement, "Target-mode legacy planner visibility")
MAIN.write_text(main, encoding="utf-8")

# Reframe the surviving Adaptive section as shared review, not a second generation/apply workflow.
en = require(EN)
for name, value in {
    "adaptive_badge": "REVIEW CLIPS",
    "adaptive_title": "Review generated clips",
    "adaptive_description": (
        "Review the target-duration clip plan in source order. Preview one candidate or the full "
        "sequence before export."
    ),
    "adaptive_empty_summary": (
        "No clip plan yet. Choose a target duration above and generate a clip plan."
    ),
    "adaptive_draft_summary": "Plan: %1$d clips • keep %2$s • remove %3$s.",
    "adaptive_applied_summary": "Active plan: %1$d clips • keep %2$s • remove %3$s.",
    "adaptive_clear": "Clear clip plan",
}.items():
    en = replace_string_value(en, name, value)
EN.write_text(en, encoding="utf-8")

my = require(MY)
for name, value in {
    "adaptive_badge": "ကလစ်များ စစ်ဆေးရန်",
    "adaptive_title": "ဖန်တီးထားတဲ့ ကလစ်တွေကို စစ်ဆေးပါ",
    "adaptive_description": (
        "Target Duration နဲ့ ဖန်တီးထားတဲ့ ကလစ်တွေကို မူရင်းအစဉ်အတိုင်း စစ်ဆေးပြီး "
        "ကလစ်တစ်ခုချင်း သို့မဟုတ် အစီအစဉ်အပြည့်ကို ကြိုကြည့်နိုင်ပါတယ်။"
    ),
    "adaptive_empty_summary": (
        "Clip plan မရှိသေးပါ။ အပေါ်က Target Duration ကိုရွေးပြီး Generate clip plan ကိုနှိပ်ပါ။"
    ),
    "adaptive_draft_summary": "Plan: %1$d ကလစ် • သိမ်း %2$s • ဖယ် %3$s။",
    "adaptive_applied_summary": "အသုံးပြုနေသော Plan: %1$d ကလစ် • သိမ်း %2$s • ဖယ် %3$s။",
    "adaptive_clear": "Clip plan ဖျက်မယ်",
}.items():
    my = replace_string_value(my, name, value)
MY.write_text(my, encoding="utf-8")

print("Phase 6H.1F.2 Clips UX unification applied.")
print("Target Duration is now the single normal planner; shared clip review remains visible.")
print("Next: commit generated MainActivity/strings changes, then run localization, diff, unit, build, and device gates.")
