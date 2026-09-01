#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
STRINGS_EN = ROOT / "app/src/main/res/values/strings.xml"
STRINGS_MY = ROOT / "app/src/main/res/values-my/strings_overlay_adaptive.xml"
MARKER = "PHASE6H2_ANIMATION_UI"


def replace_string_resource(text: str, name: str, value: str, label: str) -> str:
    pattern = re.compile(rf'<string name="{re.escape(name)}">.*?</string>')
    replacement = f'<string name="{name}">{value}</string>'
    updated, count = pattern.subn(replacement, text, count=1)
    if count != 1:
        raise SystemExit(f"FAIL: {label}: resource {name} not found exactly once")
    return updated


main = MAIN.read_text()
if MARKER not in main:
    raise SystemExit(
        "FAIL: MainActivity UI integration marker missing; run apply_phase6h2_animation_ui.py first"
    )

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

print("PASS: Phase 6H.2 animation UI localization resumed and finalized.")
