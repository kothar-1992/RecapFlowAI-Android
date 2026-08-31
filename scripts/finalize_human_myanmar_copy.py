#!/usr/bin/env python3
"""Final small Human Myanmar polish pass.

Run after the safe Human Myanmar pass. This script only replaces complete Android
<string> elements by resource name, preserves format placeholders, and validates XML.
"""

from __future__ import annotations

from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
VALUES_MY = ROOT / "app" / "src" / "main" / "res" / "values-my"

REPLACEMENTS = {
    "editor_workspace_title": "ဗီဒီယို တည်းဖြတ်မယ်",
    "settings_editor_preferences_title": "တည်းဖြတ်ဆက်တင် သိမ်းထားမယ်",
    "transition_on_summary": "ဖွင့်ထားပါတယ် • %1$s • %2$s — ကြိုကြည့်မှာ မြင်ရပြီး ဗီဒီယိုထုတ်တဲ့အခါလည်း သုံးပါမယ်။",
    "transition_preview_note": "ဒီပေါ်/ပျောက်ပုံက ဗီဒီယိုပုံအတွက်ပဲ ဖြစ်ပါတယ်။ ကလစ်အများကြီး သုံးထားရင် ကလစ်တိုင်းရဲ့ အစ/အဆုံးမှာ သက်ရောက်ပါမယ်။ အစနဲ့အဆုံး နှစ်ဖက်လုံးရွေးထားရင် ကလစ်ကြားမှာ အမည်းရောင်ကို ဖြတ်ပြီး ဖြည်းဖြည်းကူးပါမယ်။",
    "render_encoder_unavailable": "ဒီစက်မှာ လိုအပ်တဲ့ ဗီဒီယိုထုတ်စနစ် မရှိလို့ ဗီဒီယိုမထုတ်နိုင်ပါ။",
    "export_duration_apply": "ကလစ်တွေကို %1$s အထိ ပြင်မယ်",
    "export_duration_updated": "ကလစ်တွေကို %1$s အထိ ပြင်ပြီးပါပြီ။",
    "export_duration_update_unavailable": "အနီးဆုံးကြာချိန်ကို အခု မသုံးနိုင်တော့ပါ။ ရွေးထားတဲ့အပိုင်း ဒါမှမဟုတ် ကလစ်အကြမ်းကို ပြန်စစ်ပါ။",
    "overlay_source_blur_summary": "စာတန်းဖျော့ • အား %1$d • %2$s–%3$s",
    "overlay_image_summary": "ပုံ • %1$s • အရွယ် %2$d%% • မြင်သာမှု %3$d%%",
    "source_blur_strength_value": "ဖျော့အား: %1$d",
    "image_overlay_on_summary": "ဖွင့်ထားပါတယ် — %1$s ပုံကို တည်းဖြတ်ပြီးတဲ့ ဗီဒီယိုပေါ်မှာ ထည့်ပါမယ်။",
    "image_overlay_importing": "%1$s ကို ပြင်ဆင်နေပါတယ်…",
    "image_overlay_x_value": "ဘယ်/ညာ: %1$d%%",
    "image_overlay_y_value": "အပေါ်/အောက်: %1$d%%",
    "image_overlay_size_value": "ပုံအကျယ်: %1$d%%",
    "adaptive_description": "ဗီဒီယိုထဲက ထားချင်တဲ့အပိုင်းတွေကို အကြမ်းရွေးပေးမယ်။ ကလစ်တစ်ခုချင်းစီ စမ်းကြည့်ပြီး စိတ်ကြိုက်ဖြစ်မှ သုံးပါ။ ဒီလုပ်ဆောင်ချက်က AI နဲ့ ဇာတ်ဝင်ခန်းခွဲတာ မဟုတ်ပါ။",
    "next_gate_title": "ရွေးထားတဲ့ အရည်အသွေးနဲ့ ဗီဒီယိုထုတ်ပြီး သိမ်းမယ်",
    "render_selected_title": "%1$s နဲ့ ဗီဒီယိုထုတ်ပြီး သိမ်းမယ်",
    "render_quality_upscale_warning": "မူရင်းဗီဒီယိုရဲ့ အတိုဘက်က %1$d px ပါ။ %2$s အဖြစ် ချဲ့ထုတ်လို့ရပေမယ့် မူရင်းမှာမရှိတဲ့ အသေးစိတ်ကို ပိုကြည်လာအောင် မဖန်တီးပေးနိုင်ပါ။",
    "render_quality_generation_warning": "ဒီဗီဒီယိုကို RecapFlowAI နဲ့ အရင်ထုတ်ထားတာ ဖြစ်နိုင်ပါတယ်။ ထပ်ထုတ်တိုင်း အရည်အသွေး နည်းနည်းလျော့နိုင်လို့ အကောင်းဆုံးအရည်အသွေးရချင်ရင် မူရင်းဗီဒီယိုကို ပြန်ရွေးပါ။",
}

MYANMAR_DIGITS = set("၀၁၂၃၄၅၆၇၈၉")


def replace_resource(text: str, name: str, value: str) -> tuple[str, bool]:
    pattern = re.compile(
        rf'(<string\b[^>]*\bname="{re.escape(name)}"[^>]*>)(.*?)(</string>)'
    )
    matches = list(pattern.finditer(text))
    if len(matches) != 1:
        raise SystemExit(f"Expected exactly one <string> named {name}, found {len(matches)}")
    current = matches[0].group(2)
    if current == value:
        return text, False
    updated = pattern.sub(lambda m: f"{m.group(1)}{value}{m.group(3)}", text, count=1)
    return updated, True


def main() -> int:
    changed = 0
    found: set[str] = set()

    for path in sorted(VALUES_MY.glob("*.xml")):
        text = path.read_text(encoding="utf-8")
        original = text
        for name, value in REPLACEMENTS.items():
            if f'name="{name}"' not in text:
                continue
            text, did_change = replace_resource(text, name, value)
            found.add(name)
            changed += int(did_change)
        if text != original:
            path.write_text(text, encoding="utf-8")

    missing = sorted(set(REPLACEMENTS) - found)
    if missing:
        print("Missing expected resources: " + ", ".join(missing), file=sys.stderr)
        return 1

    for path in sorted(VALUES_MY.glob("*.xml")):
        try:
            ET.parse(path)
        except ET.ParseError as exc:
            print(f"XML parse failed: {path}: {exc}", file=sys.stderr)
            return 1
        content = path.read_text(encoding="utf-8")
        if any(ch in content for ch in MYANMAR_DIGITS):
            print(f"Myanmar numeral found in {path}", file=sys.stderr)
            return 1

    print(f"Final Human Myanmar polish PASS: {changed} strings updated.")
    print("XML structure validation PASS; English numerals 0-9 policy preserved.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
