#!/usr/bin/env python3
"""Second Human Myanmar review pass for RecapFlowAI.

Applies targeted wording fixes after manual review of the first Human Myanmar diff.
Also removes millisecond-facing duration copy from the normal Export UI by formatting
small duration deltas as decimal seconds in MainActivity.

Run from repository root after humanize_myanmar_copy.py:
    python3 scripts/refine_human_myanmar_copy.py
    python3 scripts/verify_myanmar_localization.py
"""

from __future__ import annotations

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
VALUES_MY = ROOT / "app" / "src" / "main" / "res" / "values-my"
DEFAULT_STRINGS = ROOT / "app" / "src" / "main" / "res" / "values" / "strings.xml"
MAIN_ACTIVITY = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "recapflow" / "ai" / "MainActivity.kt"

MY = {
    "video_preview": "ဗီဒီယို ကြိုကြည့်",
    "preview_overlay_reset": "မူလအတိုင်း",
    "settings_processing_title": "ဗီဒီယို ဘယ်မှာလုပ်မလဲ",
    "settings_device_profile_description": "RecapFlowAI က ကြိုကြည့်ပုံနဲ့ တည်းဖြတ်ပုံကို ဒီစက်နဲ့ ကိုက်ညီအောင်လုပ်ဖို့ လိုတဲ့ စက်အချက်အလက်အနည်းငယ်ကိုပဲ ဖတ်ပါတယ်။ ကိုယ်ရေးအချက်အလက်တွေကို အပြင်မပို့ပါ။",
    "settings_reset_cancel": "မလုပ်တော့ဘူး",
    "settings_reset_all_message": "ပုံစံ၊ အသံ၊ ပုံ/လိုဂို၊ ကလစ်ဖြတ်ပုံ၊ ထုတ်မယ့်အရည်အသွေးနဲ့ ကြိုကြည့်နေရာတွေကို မူလအတိုင်း ပြန်ထားပါမယ်။ ရွေးထားတဲ့ မူရင်းဗီဒီယိုကို မဖျက်ပါ။",
    "settings_export_section_no_preferences": "ဗီဒီယိုအရည်အသွေးကို 1080p ပြန်ထားပြီးပါပြီ။ ဖုန်းပြခန်းထဲက ဖိုင်ဟောင်းတွေကို မပြောင်းပါ။",
    "settings_future_note": "ဖုန်းပြခန်းထဲ သိမ်းတာနဲ့ တည်းဖြတ်ဆက်တင်သိမ်းတာကို ဒီဖုန်းထဲမှာပဲ လုပ်ပါတယ်။ Telegram ပို့တာနဲ့ AI လုပ်ဆောင်ချက်တွေက အဆင်သင့်ဖြစ်ပြီး စမ်းသပ်ပြီးမှ ပြပါမယ်။",
    "preparing_unknown_file": "ရွေးထားတဲ့ ဗီဒီယို",
    "progress_of_total": "%2$s ထဲက %1$s",
    "orientation_label": "ဗီဒီယိုအနေအထား",
    "container_label": "ဖိုင်ပုံစံ",
    "video_codec_label": "ဗီဒီယိုဖိသိပ်ပုံ",
    "review_editor_tab_transform": "ပုံစံပြင်",
    "scale_fit": "မဖြတ်ဘဲ အပြည့်မြင်",
    "scale_fill": "မျက်နှာပြင်အပြည့်",
    "mirror_off_summary": "ပိတ်ထားပါတယ် — အရင်ရွေးထားတဲ့ ဘယ်ညာပြောင်းမှုကို ကြိုကြည့်နဲ့ ဗီဒီယိုထုတ်တဲ့အခါ မသုံးပါ။",
    "mirror_on_summary": "ဖွင့်ထားပါတယ် — ကြိုကြည့်နဲ့ ထွက်ဗီဒီယိုကို ဘယ်ညာပြောင်းပြပါမယ်။",
    "mirror_remembered_summary": "အရင်ရွေးထားတာကို မှတ်ထားပါတယ် — သုံးချင်ရင် ပုံစံပြင်တာကို ဖွင့်ပါ။",
    "color_off_summary": "ပိတ်ထားပါတယ် — အရင်အရောင်ဆက်တင်ကို ကြိုကြည့်နဲ့ ဗီဒီယိုထုတ်တဲ့အခါ မသုံးပါ။",
    "color_remembered_summary": "အရင်အရောင်ဆက်တင်ကို မှတ်ထားပါတယ် — သုံးချင်ရင် ပုံစံပြင်တာကို ဖွင့်ပါ။",
    "color_contrast_default": "အလင်းအမှောင် ကွာခြားမှု: 0",
    "color_contrast_value": "အလင်းအမှောင် ကွာခြားမှု: %1$+d",
    "color_settings_error": "အရောင်ဆက်တင်တစ်ခုက ရွေးလို့ရတဲ့ အတိုင်းအတာကို ကျော်နေပါတယ်။",
    "transition_remembered_summary": "အရင်ရွေးထားတဲ့ %1$s • %2$s ကို မှတ်ထားပါတယ် — သုံးချင်ရင် ပုံစံပြင်တာကို ဖွင့်ပါ။",
    "transition_preview_note": "ဒီအကျိုးသက်ရောက်မှုက ပုံအတွက်ပဲ ဖြစ်ပါတယ်။ ကလစ်အများကြီး သုံးထားရင် ကလစ်တိုင်းရဲ့ အစ/အဆုံးမှာ သက်ရောက်ပါမယ်။ အစနဲ့အဆုံး နှစ်ဖက်လုံးရွေးထားရင် ကလစ်ကြားမှာ အမည်းရောင်ကို ဖြတ်ပြီး ဖြည်းဖြည်းကူးပါမယ်။",
    "transition_duration_error": "ပေါ်/ပျောက်ချိန်ကို 0.5၊ 1 ဒါမှမဟုတ် 1.5 စက္ကန့်ထဲက တစ်ခုရွေးပါ။",
    "transition_too_long_error": "ဒီကလစ်က ရွေးထားတဲ့ ပေါ်/ပျောက်ချိန်အတွက် တိုလွန်းပါတယ်။ ကြာချိန်လျှော့ပါ၊ ဒါမှမဟုတ် ပိုရှည်တဲ့ကလစ် ရွေးပါ။",
    "transition_adaptive_summary": "ရွေးထားတဲ့ကလစ်တွေ • %1$s • ကလစ်တစ်ခုစီ %2$s — ကြိုကြည့်နဲ့ ဗီဒီယိုထုတ်တဲ့အခါ ဒီအတိုင်း သုံးပါမယ်။",
    "audio_keep_volume_summary": "ဖွင့်ထားပါတယ် • မူရင်းအသံ • အသံ %1$d%%",
    "audio_replace_summary": "ဖွင့်ထားပါတယ် • %1$s နဲ့ အစားထိုး • အသံ %2$d%%",
    "audio_mix_summary": "ဖွင့်ထားပါတယ် • %1$s ကို ပေါင်းထည့် • မူရင်း %2$d%% • ထည့်သံ %3$d%%",
    "audio_replace_importing": "%1$s ကို ပြင်ဆင်နေပါတယ်…",
    "audio_replace_duration_policy": "အသံဖိုင်က ဗီဒီယိုထက် တိုရင် အလိုအလျောက် ပြန်စပါမယ်။ ပိုရှည်ရင် ဗီဒီယိုဆုံးတဲ့နေရာမှာ အသံလည်း ရပ်ပါမယ်။ မူရင်းဗီဒီယိုအသံကို မသုံးပါ။",
    "audio_mix_duration_policy": "မူရင်းအသံကို ဆက်ထားပါမယ်။ ထည့်သံက တိုရင် အလိုအလျောက် ပြန်စပြီး ပိုရှည်ရင် ဗီဒီယိုဆုံးတဲ့နေရာမှာ ရပ်ပါမယ်။ အသံ 2 ခုရဲ့ အတိုးအကျယ်ကို သီးသန့်ညှိနိုင်ပါတယ်။",
    "audio_replace_preview_error": "ဒီစက်မှာ ရွေးထားတဲ့အသံကို ကြိုနားထောင်လို့ မရသေးပါ။ ဖိုင်ကို မဖျက်ထားတာကြောင့် ဗီဒီယိုထုတ်တဲ့အခါ ဆက်စမ်းနိုင်ပါတယ်။",
    "audio_replace_missing_error": "ဗီဒီယိုမထုတ်ခင် အစားထိုးမယ့် အသံဖိုင်ကို ရွေးပါ။",
    "audio_mix_missing_error": "ဗီဒီယိုမထုတ်ခင် မူရင်းအသံနဲ့ ပေါင်းမယ့် အသံဖိုင်ကို ရွေးပါ။",
    "audio_mix_source_missing_error": "ဒီဗီဒီယိုမှာ မူရင်းအသံ မပါပါ။ အသံပေါင်းမယ့်အစား အခြားအသံနဲ့ အစားထိုးပါ။",
    "audio_replace_invalid_error": "ရွေးထားတဲ့ အသံဖိုင်ကို ဖတ်လို့မရတော့ပါ။ အသံဖိုင်ကို ထပ်ရွေးပါ။",
    "overlay_badge_off": "စာတန်းဖျော့ / ပုံထည့် • ပိတ်",
    "overlay_badge_on": "စာတန်းဖျော့ / ပုံထည့် • ဖွင့်",
    "overlay_enable": "စာတန်းဖျော့ / ပုံထည့်မယ်",
    "overlay_hide_controls": "ရွေးချယ်စရာတွေ ဖျောက်မယ် ▲",
    "overlay_show_controls": "ရွေးချယ်စရာတွေ ပြမယ် ▼",
    "overlay_off_summary": "ပိတ်ထားပါတယ် — အရင်ရွေးထားတာတွေကို ကြိုကြည့်နဲ့ ဗီဒီယိုထုတ်တဲ့အခါ မသုံးပါ။",
    "overlay_on_no_items_summary": "ဖွင့်ထားပါတယ် — စာတန်းဖျော့မလား၊ ပုံ/လိုဂို ထည့်မလား ရွေးပါ။",
    "overlay_blur_and_image_summary": "စာတန်းဖျော့ + ပုံထည့် • %1$s",
    "source_blur_region_slider_description": "ဖျော့မယ့်နေရာနဲ့ အရွယ်ကို အောက်က ခလုတ်တန်းတွေနဲ့ ပြင်နိုင်ပါတယ်",
    "source_blur_resize_description": "ဖျော့မယ့်နေရာကို ချဲ့/ချုံ့ဖို့ ဆွဲပါ",
    "source_blur_strength_value": "ဖျော့မှု: %1$d",
    "image_overlay_opacity_value": "ပုံမြင်သာမှု: %1$d%%",
    "image_overlay_preview_note": "ပုံရဲ့ ဖောက်မြင်နိုင်မှုကို မပျက်အောင် ထားပါမယ်။ နေရာ၊ အရွယ်၊ ပုံမြင်သာမှုနဲ့ ပြမယ့်အချိန်ကို ကြိုကြည့်နဲ့ 720p/1080p ထုတ်တဲ့အခါ တူတူသုံးပါမယ်။",
    "image_overlay_asset_error": "ဗီဒီယိုမထုတ်ခင် PNG၊ JPEG ဒါမှမဟုတ် WebP ပုံတစ်ပုံကို ပြန်ရွေးပါ။",
    "image_overlay_geometry_error": "ပုံရဲ့ နေရာ၊ အရွယ် ဒါမှမဟုတ် ပုံမြင်သာမှုက ရွေးလို့ရတဲ့ အတိုင်းအတာကို ကျော်နေပါတယ်။",
    "image_overlay_time_error": "ပုံပြမယ့်အချိန်ကို မူရင်းဗီဒီယိုအတွင်းမှာ ရွေးပြီး အနည်းဆုံး 0.25 စက္ကန့်ထားပါ။",
    "adaptive_sequence_preview_error": "ဒီစက်မှာ ကလစ်အများကြီးကို ဆက်တိုက်ကြိုကြည့်လို့ မရသေးပါ။ မူရင်းဗီဒီယိုကို ပြန်ပြထားပြီး ဗီဒီယိုထုတ်မယ့် ဆက်တင်တွေကို မပြောင်းပါ။",
    "adaptive_apply_note_with_transition": "ရွေးထားတဲ့ကလစ်တွေကို အစဉ်လိုက် ဆက်ပြီး ရွေးထားတဲ့ ပေါ်/ပျောက်ကူးပြောင်းမှုကို ကလစ်တိုင်းမှာ သုံးပါမယ်။ အသံကို မဖျော့ပါ။",
    "adaptive_ranges_missing_error": "သုံးဖို့ အနည်းဆုံး ကလစ် 1 ခု အရင်ဖန်တီးပြီး စမ်းကြည့်ပါ။",
    "adaptive_ranges_invalid_error": "ကလစ်တွေက အစဉ်လိုက် ဖြစ်ရပြီး တစ်ခုနဲ့တစ်ခု မထပ်ရပါ။ ကလစ်တစ်ခုစီ အနည်းဆုံး 1 စက္ကန့်ရှိပြီး ရွေးထားတဲ့ ဗီဒီယိုအတွင်းမှာ ရှိရပါမယ်။",
    "render_metrics": "ထုတ်ဖို့ကြာချိန် %1$s • ဗီဒီယိုကြာချိန် %2$s • ပုံမှန်အချိန်ထက် %3$.2f×",
    "render_output_validated": "ထွက်ဗီဒီယိုကို စစ်ပြီးပါပြီ။ ရုပ်ထွက်၊ ကြာချိန်နဲ့ အသံဆက်တင်တွေ အဆင်ပြေပါတယ်။ နောက်ဆုံးအရည်အသွေးက မူရင်းဗီဒီယိုနဲ့ ဒီစက်ရဲ့ ထုတ်နိုင်စွမ်းပေါ် မူတည်ပါတယ်။",
    "rendering_local_summary": "ဒီဖုန်းထဲမှာ %1$s ဗီဒီယို ထုတ်နေပါတယ်။",
    "rendering_local_muted_summary": "ဒီဖုန်းထဲမှာ %1$s အသံမပါတဲ့ ဗီဒီယို ထုတ်နေပါတယ်။",
    "rendering_local_replaced_summary": "ဒီဖုန်းထဲမှာ %1$s ရွေးထားတဲ့အသံနဲ့ ဗီဒီယို ထုတ်နေပါတယ်။",
    "rendering_local_mixed_summary": "ဒီဖုန်းထဲမှာ %1$s မူရင်းအသံနဲ့ ထည့်သံကို ပေါင်းပြီး ဗီဒီယို ထုတ်နေပါတယ်။",
    "export_quality_720p_detail": "HD • ပုံမှန်အသုံးအတွက် သင့်တော်",
    "export_quality_1080p_detail": "Full HD • ပိုကြည်လင်တဲ့ ရုပ်ထွက်",
    "export_quality_2k_detail": "2K QHD • အမြင့်ဆုံး ရုပ်ထွက် • ဖိုင်ပိုကြီးနိုင်",
    "export_duration_aligned": "ဗီဒီယိုကြာချိန် %1$s။ စက္ကန့်ပြည့်နဲ့ ကိုက်ပြီးပါပြီ။ ထုတ်တဲ့အခါ ±%2$s စက္ကန့်အထိ အနည်းငယ် ကွာနိုင်ပါတယ်။",
    "export_duration_suggestion": "ဗီဒီယိုကြာချိန် %1$s • အနီးဆုံး %2$s • နောက်ဆုံးကလစ်ကို %3$s%4$s စက္ကန့် ပြင်မယ် • ထုတ်ပြီးရင် ±%5$s စက္ကန့်လောက် ကွာနိုင်ပါတယ်။",
    "export_duration_unavailable": "ဗီဒီယိုကြာချိန် %1$s • အနီးဆုံး %2$s • %3$s%4$s စက္ကန့် ကွာနေပါတယ်။ နောက်ဆုံးကလစ်ကို အလိုအလျောက် မပြင်နိုင်ပါ။ ကလစ်အဆုံးချိန်ကို ပြန်ရွေးပါ။",
    "clip_transition_on_summary": "ဖြည်းဖြည်းကူး • %1$s • %2$s",
    "clip_transition_ease_in_out": "အစနဲ့အဆုံး နူးညံ့အောင် ကူးမယ်",
}

DEFAULT = {
    "export_duration_aligned": "Reviewed video: %1$s. It already lands on a whole second. Export may vary by up to ±%2$s seconds.",
    "export_duration_suggestion": "Reviewed video: %1$s • closest whole second: %2$s • adjust final clip by %3$s%4$s seconds • export may vary by about ±%5$s seconds.",
    "export_duration_unavailable": "Reviewed video: %1$s • closest whole second: %2$s • difference: %3$s%4$s seconds. The final clip cannot be adjusted automatically; change the final clip end time.",
}

STRING_RE = re.compile(r'(<string\\b[^>]*\\bname="(?P<name>[^"]+)"[^>]*>)(?P<value>.*?)(</string>)')


def rewrite_file(path: Path, replacements: dict[str, str]) -> int:
    text = path.read_text(encoding="utf-8")
    changed = 0

    def repl(match: re.Match[str]) -> str:
        nonlocal changed
        name = match.group("name")
        new = replacements.get(name)
        if new is None or new == match.group("value"):
            return match.group(0)
        changed += 1
        return f"{match.group(1)}{new}{match.group(3)}"

    updated = STRING_RE.sub(repl, text)
    if updated != text:
        path.write_text(updated, encoding="utf-8")
    return changed


def patch_duration_units() -> bool:
    text = MAIN_ACTIVITY.read_text(encoding="utf-8")
    original = text

    text = text.replace(
        """                    exactDurationText(assessment.plannedDurationMs),\n                    toleranceMs,\n""",
        """                    exactDurationText(assessment.plannedDurationMs),\n                    humanDurationDeltaText(toleranceMs),\n""",
        1,
    )
    text = text.replace(
        """                    abs(assessment.adjustmentMs),\n                    toleranceMs,\n""",
        """                    humanDurationDeltaText(abs(assessment.adjustmentMs)),\n                    humanDurationDeltaText(toleranceMs),\n""",
        1,
    )
    text = text.replace(
        """                    abs(assessment.adjustmentMs),\n                )\n""",
        """                    humanDurationDeltaText(abs(assessment.adjustmentMs)),\n                )\n""",
        1,
    )

    helper_anchor = """    private fun exactDurationText(durationMs: Long): String = String.format(\n        Locale.US,\n        \"%s.%03d\",\n        MediaFormatters.duration(durationMs),\n        durationMs.coerceAtLeast(0L) % 1_000L,\n    )\n"""
    helper = helper_anchor + """\n    private fun humanDurationDeltaText(durationMs: Long): String = String.format(\n        Locale.US,\n        \"%.3f\",\n        durationMs.coerceAtLeast(0L) / 1_000.0,\n    ).trimEnd('0').trimEnd('.')\n"""
    if "private fun humanDurationDeltaText" not in text:
        if helper_anchor not in text:
            raise SystemExit("Could not find exactDurationText() anchor in MainActivity.kt")
        text = text.replace(helper_anchor, helper, 1)

    if text != original:
        MAIN_ACTIVITY.write_text(text, encoding="utf-8")
        return True
    return False


def main() -> int:
    if not VALUES_MY.is_dir():
        print(f"Missing Myanmar resource directory: {VALUES_MY}", file=sys.stderr)
        return 1

    my_changed = 0
    for path in sorted(VALUES_MY.glob("*.xml")):
        my_changed += rewrite_file(path, MY)

    default_changed = rewrite_file(DEFAULT_STRINGS, DEFAULT)
    kotlin_changed = patch_duration_units()

    print(
        "Human Myanmar review pass complete: "
        f"{my_changed} Myanmar strings refined; "
        f"{default_changed} default duration strings updated; "
        f"MainActivity duration formatting {'updated' if kotlin_changed else 'already current'}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
