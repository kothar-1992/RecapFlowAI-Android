#!/usr/bin/env python3
"""Apply the RecapFlowAI Human Myanmar copy pass.

Goals:
- everyday, spoken-style Burmese that is quick to understand
- short action labels for buttons/toggles
- keep technical jargon out of normal UI where possible
- keep Android format placeholders unchanged
- keep all numbers as ASCII/English digits 0-9 (never Myanmar digits)

The script is intentionally idempotent and only edits values-my string resources.
Run from the repository root:
    python3 scripts/humanize_myanmar_copy.py
    python3 scripts/verify_myanmar_localization.py
"""

from __future__ import annotations

from collections import Counter
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
VALUES_MY = ROOT / "app" / "src" / "main" / "res" / "values-my"

TARGET_FILES = (
    "strings_core.xml",
    "strings_editor_media.xml",
    "strings_export.xml",
    "strings_language.xml",
    "strings_overlay_adaptive.xml",
    "strings_phase6h1.xml",
)

STRING_RE = re.compile(
    r'(?P<open><string\b[^>]*\bname="(?P<name>[^"]+)"[^>]*>)'
    r'(?P<value>.*?)'
    r'(?P<close></string>)'
)
FORMAT_RE = re.compile(r'%(?!%)(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?[a-zA-Z]')
MYANMAR_DIGITS_RE = re.compile(r'[၀-၉]')

# High-impact user-facing strings. These are deliberately written like a human
# explaining the action, rather than a developer describing an implementation.
EXACT_OVERRIDES: dict[str, str] = {
    # Home / navigation / preview
    "local_media_workspace": "ဖုန်းထဲက ဗီဒီယို တည်းဖြတ်ခန်း",
    "home_toolbar_subtitle": "ဗီဒီယိုတွေကို ဒီဖုန်းထဲမှာပဲ တည်းဖြတ်မယ်",
    "nav_home": "ပင်မ",
    "nav_editor": "တည်းဖြတ်",
    "nav_settings": "ဆက်တင်",
    "editor_toolbar_subtitle": "ဗီဒီယိုကို ကြည့်ရင်း တည်းဖြတ်မယ်",
    "editor_workspace_title": "ဗီဒီယို တည်းဖြတ်ရန်",
    "editor_workspace_subtitle": "မူရင်းဗီဒီယိုကို ကြည့်ပြီး လိုတဲ့အပိုင်းတွေရွေး၊ ပုံစံနဲ့အသံပြင်၊ စာတန်းဖျော့ပြီး ဒီဖုန်းထဲကနေ ဗီဒီယိုထုတ်နိုင်ပါတယ်။",
    "editor_sheet_hint": "တည်းဖြတ်ကိရိယာနဲ့ ဗီဒီယိုအချက်အလက်ကြည့်ဖို့ အပေါ်ကို ဆွဲပါ",
    "settings_toolbar_subtitle": "အက်ပ်နဲ့ ဒီစက်အတွက် ဆက်တင်များ",
    "video_preview": "ဗီဒီယို ကြိုကြည့်ရန်",
    "preview_overlay_move": "ရွှေ့မယ်",
    "preview_overlay_reset": "မူလနေရာ",
    "preview_overlay_move_description": "ကြိုကြည့်ဗီဒီယိုကို ရွှေ့ဖို့ ဆွဲပါ",
    "preview_overlay_resize_description": "ကြိုကြည့်ဗီဒီယိုကို ချဲ့/ချုံ့ဖို့ ဆွဲပါ",
    "preview_overlay_reset_description": "ကြိုကြည့်ဗီဒီယိုရဲ့ အရွယ်နဲ့နေရာကို မူလအတိုင်း ပြန်ထားမယ်",
    "preview_overlay_reset_confirmation": "ကြိုကြည့်ဗီဒီယိုကို မူလအရွယ်နဲ့နေရာ ပြန်ထားပြီးပါပြီ",
    "native_status_checking": "ဗီဒီယိုစနစ်ကို စစ်နေပါတယ်…",
    "native_status_ready": "ဗီဒီယိုစနစ် အဆင်သင့်ဖြစ်ပါပြီ\n%1$s",
    "native_status_error": "ဗီဒီယိုစနစ်မှာ အမှားတစ်ခု ဖြစ်နေပါတယ် (%1$d)\n%2$s",
    "home_title": "နောက်ဗီဒီယိုကို စတင်တည်းဖြတ်မယ်",
    "home_subtitle": "ဗီဒီယို 1 ခုရွေးပြီး မတည်းဖြတ်ခင် အရင်စစ်ကြည့်နိုင်ပါတယ်။",
    "home_tab_subtitle": "ဗီဒီယိုအသစ်တစ်ခု စတင်ပါ၊ ဒါမှမဟုတ် လက်ရှိဗီဒီယိုကို ဆက်တည်းဖြတ်ပါ။",
    "home_active_project": "လက်ရှိ တည်းဖြတ်နေတဲ့ ဗီဒီယို",
    "home_import_title": "ဗီဒီယိုတစ်ခု ရွေးပြီး စတင်မယ်",
    "home_import_description": "ဖုန်းထဲက ဗီဒီယိုတစ်ခုရွေးပါ။ စစ်ကြည့်တာ၊ တည်းဖြတ်တာနဲ့ ဗီဒီယိုထုတ်တာအားလုံးကို ဒီဖုန်းထဲမှာပဲ လုပ်ပါတယ်။",
    "continue_editing": "ဆက်တည်းဖြတ်မယ်",
    "on_device_badge": "ဒီဖုန်းထဲမှာပဲ",
    "engine_checking": "ဗီဒီယိုစနစ်ကို စစ်နေပါတယ်",
    "engine_ready": "ဗီဒီယိုစနစ် အဆင်သင့်ဖြစ်ပါပြီ",
    "engine_unavailable": "ဗီဒီယိုစနစ်ကို အခု သုံးလို့မရသေးပါ",
    "local_processing_note": "ရွေးထားတဲ့ မူရင်းဗီဒီယိုကို အပြင်မပို့ဘဲ ဒီဖုန်းထဲမှာပဲ စစ်ပြီး တည်းဖြတ်ပါတယ်။",

    # Settings
    "settings_title": "ဆက်တင်များ",
    "settings_subtitle": "အက်ပ်ရဲ့ တည်းဖြတ်ဆက်တင်နဲ့ ဒီစက်အခြေအနေကို ဒီနေရာမှာ စစ်နိုင်ပါတယ်။",
    "settings_processing_title": "ဗီဒီယိုကို ဘယ်လိုလုပ်မလဲ",
    "settings_processing_value": "ဒီဖုန်းထဲမှာပဲ လုပ်မယ်",
    "settings_processing_description": "ဗီဒီယိုရွေးတာ၊ အပိုင်းဖြတ်တာ၊ ပုံစံနဲ့အသံပြင်တာ၊ စာတန်းဖျော့တာနဲ့ ဗီဒီယိုထုတ်တာတွေကို ပြင်ပဆာဗာမသုံးဘဲ ဒီဖုန်းထဲမှာပဲ လုပ်ပါတယ်။",
    "settings_device_profile_title": "ဒီစက်နဲ့ ကိုက်ညီအောင် အလိုအလျောက်ညှိမယ်",
    "settings_device_profile_description": "RecapFlowAI က ကြိုကြည့်ပုံနဲ့ တည်းဖြတ်လုပ်ဆောင်ပုံကို ဒီစက်နဲ့ကိုက်ညီအောင် စက်အချက်အလက်အနည်းငယ်ကိုပဲ ဖတ်ပါတယ်။ ကိုယ်ရေးအချက်အလက်တွေကို အပြင်မပို့ပါ။",
    "device_checking": "ဒီစက်အခြေအနေကို စစ်နေပါတယ်…",
    "settings_editor_preferences_title": "တည်းဖြတ်ဆက်တင် သိမ်းထားရန်",
    "settings_editor_preferences_description": "လက်ရှိတည်းဖြတ်ဆက်တင်ကို သိမ်းထားပြီး နောက်တစ်ခါ ပြန်သုံးနိုင်ပါတယ်။ မူရင်းဗီဒီယို၊ လိုဂို/အသံဖိုင်၊ ထွက်ဖိုင်နဲ့ API key တွေကို ဒီနေရာမှာ မသိမ်းပါ။",
    "settings_auto_restore": "အက်ပ်ဖွင့်တိုင်း နောက်ဆုံးတည်းဖြတ်ဆက်တင်ကို ပြန်သုံးမယ်",
    "settings_editor_preferences_status_default": "သိမ်းထားတဲ့ တည်းဖြတ်ဆက်တင် မရှိသေးပါ။",
    "settings_editor_preferences_status_saved": "လက်ရှိတည်းဖြတ်ဆက်တင်ကို ဒီဖုန်းထဲမှာ သိမ်းထားပြီးပါပြီ။",
    "settings_editor_preferences_status_available": "သိမ်းထားတဲ့ တည်းဖြတ်ဆက်တင် ရှိပါတယ်။",
    "settings_editor_preferences_status_restored": "သိမ်းထားတဲ့ တည်းဖြတ်ဆက်တင်ကို ပြန်သုံးပြီးပါပြီ။ မရှိတော့တဲ့ ပုံ/အသံဖိုင်တွေကို အလိုအလျောက် ပိတ်ထားပါတယ်။",
    "settings_editor_preferences_status_reset": "တည်းဖြတ်ဆက်တင်ကို မူလအတိုင်း ပြန်ထားပြီးပါပြီ။",
    "settings_save_preset": "လက်ရှိဆက်တင်ကို သိမ်းထားမယ်",
    "settings_restore_preset": "သိမ်းထားတဲ့ဆက်တင်ကို ပြန်သုံးမယ်",
    "settings_restore_last_session": "နောက်ဆုံးတည်းဖြတ်ထားတာကို ပြန်ဖွင့်မယ်",
    "settings_reset_current_section": "ဒီအပိုင်းကို မူလအတိုင်း ပြန်ထားမယ်",
    "settings_reset_all": "တည်းဖြတ်ဆက်တင်အားလုံးကို မူလအတိုင်း ပြန်ထားမယ်",
    "settings_no_saved_preset": "သိမ်းထားတဲ့ ဆက်တင် မရှိသေးပါ။",
    "settings_no_last_session": "ပြန်ဖွင့်နိုင်တဲ့ နောက်ဆုံးတည်းဖြတ်မှု မရှိသေးပါ။",
    "settings_preferences_busy": "အခု ဗီဒီယိုထုတ်နေပါတယ်။ ပြီးအောင်စောင့်ပါ၊ ဒါမှမဟုတ် အရင်ပယ်ဖျက်ပါ။",
    "settings_reset_all_title": "တည်းဖြတ်ဆက်တင်အားလုံးကို မူလအတိုင်း ပြန်ထားမလား?",
    "settings_reset_confirm": "အားလုံး ပြန်ထားမယ်",
    "settings_reset_cancel": "မလုပ်တော့ပါ",
    "settings_section_reset": "ဒီအပိုင်းရဲ့ ဆက်တင်ကို မူလအတိုင်း ပြန်ထားပြီးပါပြီ။",

    # Import / media details
    "empty_title": "ဗီဒီယိုတစ်ခုနဲ့ စတင်မယ်",
    "empty_description": "ဖုန်းထဲက ဗီဒီယိုတစ်ခုရွေးပါ။ RecapFlowAI က တည်းဖြတ်ဖို့ မိတ္တူတစ်ခု ပြင်ဆင်ပြီး ဗီဒီယိုအချက်အလက်ကို ဒီဖုန်းထဲမှာပဲ စစ်ပါတယ်။",
    "import_video": "ဗီဒီယို ရွေးမယ်",
    "choose_another_video": "အခြားဗီဒီယို ရွေးမယ်",
    "legacy_media_permission_title": "ဗီဒီယိုဖိုင်ကို ဖွင့်ခွင့်ပေးပါ",
    "legacy_media_permission_message": "Android ဗားရှင်းအဟောင်းအချို့မှာ ဗီဒီယိုဖိုင်ဖွင့်ဖို့ ဖိုင်အသုံးပြုခွင့် လိုနိုင်ပါတယ်။ ခွင့်မပေးချင်ရင် ဖုန်းရဲ့ ဗီဒီယိုရွေးချယ်စနစ်ကိုပဲ သုံးလို့ရပါတယ်။",
    "allow_access": "ခွင့်ပြုမယ်",
    "use_system_picker": "ဗီဒီယိုရွေးမယ်",
    "preparing_title": "ဗီဒီယိုကို ပြင်ဆင်နေပါတယ်",
    "probing_title": "ဗီဒီယိုအချက်အလက် စစ်နေပါတယ်",
    "probe_progress_detail": "ဗီဒီယိုကြာချိန်၊ ရုပ်ထွက်၊ အသံနဲ့ ဖိုင်အမျိုးအစားကို စစ်နေပါတယ်။",
    "probe_ready": "ဗီဒီယို စစ်ပြီးပါပြီ",
    "media_details_title": "ဗီဒီယို အသေးစိတ်",
    "technical_details": "အသေးစိတ်အချက်အလက်",
    "hide_technical_details": "အသေးစိတ်ကို ဖျောက်မယ်",

    # Clips / transform
    "review_editor_title": "လိုတဲ့အပိုင်းကို ရွေးမယ်",
    "review_editor_clips_tab": "ကလစ်များ • ဖြတ်ရွေး",
    "review_editor_subtitle": "သိမ်းချင်တဲ့ ဗီဒီယိုအပိုင်းကို ရွေးပါ။ မဖြတ်ချင်ရင် ဗီဒီယိုအပြည့်ကို ပြန်ရွေးပါ။",
    "review_editor_tab_clips": "ကလစ်",
    "review_editor_tab_transform": "ပုံစံ",
    "review_editor_tab_audio": "အသံ",
    "review_editor_tab_overlay": "ထပ်တင်",
    "review_editor_tab_export": "ဗီဒီယိုထုတ်",
    "trim_start_label": "စချိန်",
    "trim_end_label": "ဆုံးချိန်",
    "trim_selected_duration": "ရွေးထားတဲ့အပိုင်း: %1$s",
    "trim_reset": "ဗီဒီယိုအပြည့် ပြန်ရွေးမယ်",
    "trim_minimum_error": "အနည်းဆုံး 1 စက္ကန့်စာ ရွေးပါ။",
    "trim_bounds_error": "ရွေးထားတဲ့အပိုင်းက မူရင်းဗီဒီယိုအတွင်းမှာပဲ ရှိရပါမယ်။",
    "transform_badge_off": "ပုံစံပြင် • ပိတ်",
    "transform_badge_on": "ပုံစံပြင် • ဖွင့်",
    "transform_enable": "ဗီဒီယိုပုံစံ ပြင်မယ်",
    "transform_hide_controls": "ရွေးချယ်စရာတွေ ဖျောက်မယ် ▲",
    "transform_show_controls": "ရွေးချယ်စရာတွေ ပြမယ် ▼",
    "transform_off_summary": "ပိတ်ထားပါတယ် — မူရင်းဗီဒီယိုပုံစံအတိုင်း ထားပါမယ်။",
    "transform_original_summary": "ဖွင့်ထားပါတယ် • မူရင်းအချိုး — ဗီဒီယိုအချိုးကို မပြောင်းသေးပါ။",
    "aspect_ratio_label": "ဗီဒီယိုအချိုး",
    "scale_mode_label": "ဗီဒီယိုကို ဘယ်လိုဖြည့်မလဲ",
    "scale_fit": "ဗီဒီယိုအပြည့် မြင်ရမယ်",
    "scale_fill": "မျက်နှာပြင်အပြည့် ဖြည့်မယ်",
    "scale_fit_description": "ဗီဒီယိုကို မဖြတ်ဘဲ အပြည့်မြင်ရမယ်",
    "scale_fill_description": "မျက်နှာပြင်ပြည့်အောင် အနားတချို့ကို ဖြတ်နိုင်ပါတယ်",
    "crop_enable": "ကိုယ်လိုသလို ဖြတ်မယ်",
    "crop_off_summary": "ပိတ်ထားပါတယ် — အရင်ဖြတ်ထားတာကို ကြိုကြည့်နဲ့ ဗီဒီယိုထုတ်ရာမှာ မသုံးပါ။",
    "crop_left_edge": "ဘယ်ဘက်က ဖြတ်မယ်",
    "crop_top_edge": "အပေါ်ဘက်က ဖြတ်မယ်",
    "crop_right_edge": "ညာဘက်က ဖြတ်မယ်",
    "crop_bottom_edge": "အောက်ဘက်က ဖြတ်မယ်",
    "crop_rectangle_error": "ဖြတ်ပြီးနောက် မူရင်းပုံရဲ့ အနည်းဆုံး 10% ကျန်ရပါမယ်။",
    "mirror_enable": "ဗီဒီယိုကို ဘယ်ညာ ပြောင်းမယ်",
    "color_enable": "အရောင်နဲ့ အလင်းကို ပြင်မယ်",
    "color_neutral_summary": "ဖွင့်ထားပါတယ် • လက်ရှိအရောင်ကို မပြောင်းသေးပါ",
    "color_contrast": "အလင်းအမှောင် ကွာခြားမှု",
    "color_temperature": "အရောင် အေး/နွေး",
    "color_temperature_default": "အရောင် အေး/နွေး: 0",
    "color_temperature_value": "အရောင် အေး/နွေး: %1$+d",

    # Zoom / speed / freeze / fade
    "color_reset": "အရောင်ကို မူလအတိုင်း ပြန်ထားမယ်",
    "color_settings_error": "အရောင်ဆက်တင်တစ်ခုက ခွင့်ပြုထားတဲ့အတိုင်းအတာ ကျော်နေပါတယ်။",
    "zoom_enable": "အနီး/အဝေး ရွှေ့မယ်",
    "zoom_off_summary": "ပိတ်ထားပါတယ် — အနီး/အဝေး ရွှေ့တာကို မသုံးပါ။",
    "zoom_remembered_summary": "အရင်ရွေးထားတဲ့ %1$s ကို မှတ်ထားပါတယ် — သုံးချင်ရင် ပုံစံပြင်တာကို ဖွင့်ပါ။",
    "zoom_on_summary": "ဖွင့်ထားပါတယ် • %1$s — ကြိုကြည့်မှာလည်း မြင်ရပြီး ဗီဒီယိုထုတ်တဲ့အခါလည်း ဒီအတိုင်း သုံးပါမယ်။",
    "zoom_mode_in": "ဖြည်းဖြည်း ချဲ့မယ်",
    "zoom_mode_out": "ဖြည်းဖြည်း ချုံ့မယ်",
    "zoom_mode_alternate": "ချဲ့/ချုံ့ အလှည့်ကျ",
    "zoom_mode_note": "ချဲ့မယ်ဆိုရင် ပုံကို 115% အထိ ဖြည်းဖြည်းချဲ့မယ်။ ချုံ့မယ်ဆိုရင် 90% အထိ ဖြည်းဖြည်းချုံ့မယ်။ အလှည့်ကျကိုရွေးရင် 4 စက္ကန့်ခန့်စီ ချဲ့/ချုံ့ လုပ်ပါမယ်။",
    "speed_enable": "ဗီဒီယို အမြန်နှုန်းပြောင်းမယ်",
    "speed_off_summary": "ပိတ်ထားပါတယ် — မူရင်းဗီဒီယိုအမြန်နှုန်းအတိုင်း ထားပါမယ်။",
    "speed_remembered_summary": "အရင်ရွေးထားတဲ့ %1$s ကို မှတ်ထားပါတယ် — သုံးချင်ရင် ပုံစံပြင်တာကို ဖွင့်ပါ။",
    "speed_on_summary": "ဖွင့်ထားပါတယ် • %1$s • ထွက်ဗီဒီယိုကြာချိန်ခန့်မှန်း %2$s",
    "freeze_enable": "အစပုံကို ခဏရပ်ထားမယ်",
    "freeze_off_summary": "ပိတ်ထားပါတယ် — ကလစ်မစခင် ပုံရပ်ထားတာ မထည့်ပါ။",
    "freeze_remembered_summary": "အရင်ရွေးထားတဲ့ %1$s ကို မှတ်ထားပါတယ် — သုံးချင်ရင် ပုံစံပြင်တာကို ဖွင့်ပါ။",
    "freeze_on_summary": "ဖွင့်ထားပါတယ် • အစပုံကို %1$s ရပ်ထားမယ် • ထွက်ဗီဒီယိုကြာချိန်ခန့်မှန်း %2$s",
    "freeze_preview": "အစပုံရပ်ထားတာ စမ်းကြည့်မယ်",
    "freeze_previewing": "ပုံကို ခဏရပ်ပြနေပါတယ်…",
    "transition_enable": "အစ/အဆုံးကို ဖြည်းဖြည်း ပေါ်/ပျောက်အောင်လုပ်မယ်",
    "transition_off_summary": "ပိတ်ထားပါတယ် — အစ/အဆုံး ပေါ်ပျောက်တာ မထည့်ပါ။",
    "transition_mode_in": "အစမှာ ဖြည်းဖြည်းပေါ်မယ်",
    "transition_mode_out": "အဆုံးမှာ ဖြည်းဖြည်းပျောက်မယ်",
    "transition_mode_both": "အစနဲ့အဆုံး နှစ်ဖက်လုံး",
    "transition_duration_label": "ပေါ်/ပျောက် ကြာချိန်",
    "transform_preview_note": "ပြောင်းထားတာတွေကို ဗီဒီယိုမထုတ်ခင် ဒီမှာပဲ ကြိုကြည့်နိုင်ပါတယ်။ မျက်နှာပြင်ပြည့်ကိုရွေးရင် အနားတချို့ ဖြတ်သွားနိုင်ပါတယ်။",
    "live_preview_fallback": "ဒီစက်မှာ ပြောင်းထားတာတွေကို တိုက်ရိုက်ကြိုကြည့်လို့ မရသေးပါ။ အခုတော့ မူရင်းဗီဒီယိုကို ပြထားပြီး ရွေးထားတဲ့ဆက်တင်တွေကို မပျောက်အောင် ထိန်းထားပါတယ်။",
    "retry_live_effects": "တိုက်ရိုက်ကြိုကြည့်တာ ထပ်စမ်းမယ်",
    "retry_live_effects_description": "ပြောင်းထားတာတွေကို တိုက်ရိုက်ကြိုကြည့်လို့ ရမရ ထပ်စမ်းမယ်",

    # Audio
    "audio_badge_off": "အသံ • ပိတ်",
    "audio_badge_on": "အသံ • ဖွင့်",
    "audio_enable": "အသံပြင်မယ်",
    "audio_off_summary": "ပိတ်ထားပါတယ် — မူရင်းအသံအတိုင်း ထားပါမယ်။",
    "audio_keep_summary": "ဖွင့်ထားပါတယ် • မူရင်းအသံကို ဆက်သုံးမယ်",
    "audio_mute_summary": "ဖွင့်ထားပါတယ် • အသံပိတ် — ကြိုကြည့်နဲ့ ထွက်ဗီဒီယို နှစ်ခုလုံး အသံမပါပါ။",
    "audio_replace_missing_summary": "အသံအစားထိုးဖို့ အသံဖိုင် 1 ခု ရွေးပါ။",
    "audio_mix_missing_summary": "မူရင်းအသံနဲ့ ပေါင်းဖို့ အသံဖိုင် 1 ခု ရွေးပါ။",
    "audio_policy_label": "အသံကို ဘယ်လိုသုံးမလဲ",
    "audio_keep_original": "မူရင်းအသံကို သုံးမယ်",
    "audio_mute": "အသံပိတ်မယ်",
    "audio_replace": "အခြားအသံနဲ့ အစားထိုးမယ်",
    "audio_mix": "မူရင်းအသံနဲ့ ပေါင်းမယ်",
    "audio_replace_track_label": "အစားထိုးမယ့် အသံဖိုင်",
    "audio_mix_track_label": "ပေါင်းထည့်မယ့် အသံဖိုင်",
    "audio_replace_required": "အသံဖိုင် မရွေးရသေးပါ။ ဖုန်းထဲက အသံဖိုင်တစ်ခု ရွေးပါ။",
    "audio_replace_choose": "အသံဖိုင် ရွေးမယ်",
    "audio_replace_change": "အသံဖိုင် ပြောင်းမယ်",
    "audio_replace_clear": "အသံဖိုင် ဖယ်မယ်",
    "audio_volume_label": "အသံအတိုးအကျယ်",
    "audio_volume_reset": "အသံအတိုးအကျယ်ကို မူလအတိုင်း ပြန်ထားမယ်",
    "audio_mix_reset_balance": "အသံနှစ်ခုရဲ့ အတိုးအကျယ်ကို မူလအတိုင်း ပြန်ထားမယ်",
    "audio_preview_note": "အသံအတိုးအကျယ်ကို ဗီဒီယိုမထုတ်ခင် ကြားကြည့်နိုင်ပါတယ်။ 0–100% အတွင်း ရွေးနိုင်ပြီး အသံပေါင်းစပ်တဲ့အခါ မူရင်းအသံ 70% + ထည့်သံ 30% ကနေ စတင်ထားပါတယ်။",

    # Blur / image overlay / adaptive clips
    "source_blur_enable": "မူရင်းစာတန်းကို ဖျော့မယ်",
    "source_blur_off_summary": "ပိတ်ထားပါတယ် — မူရင်းဗီဒီယိုကို မပြောင်းပါ။",
    "source_blur_on_summary": "ဖွင့်ထားပါတယ် — အခုချိန်မှာ ဖျော့မယ့်နေရာကို အောက်က ခလုတ်တန်းတွေနဲ့ ရွှေ့/ချဲ့နိုင်ပါတယ်။",
    "source_blur_instruction": "မူရင်းဗီဒီယိုထဲက စာတန်းကို ကိုယ်တိုင် ဖျော့ပါ။ ဒီအက်ပ်က စာတန်းကို အလိုအလျောက်မရှာဘဲ အွန်လိုင်းကိုလည်း မပို့ပါ။",
    "source_blur_region_label": "ဖျော့မယ့်နေရာ",
    "source_blur_region_description": "ဖျော့မယ့်နေရာကို ရွှေ့ဖို့ ဆွဲပါ",
    "source_blur_time_label": "ဘယ်အချိန်မှာ ဖျော့မလဲ",
    "source_blur_strength_value": "ဖျော့အား: %1$d",
    "source_blur_reset": "ဖျော့မယ့်နေရာကို မူလအတိုင်း ပြန်ထားမယ်",
    "source_blur_preview_note": "ရွေးထားတဲ့နေရာ၊ အချိန်နဲ့ ဖျော့အားကို ဗီဒီယိုမထုတ်ခင် ကြိုကြည့်နိုင်ပါတယ်။ 720p/1080p ထုတ်တဲ့အခါလည်း ဒီအတိုင်း သုံးပါမယ်။",
    "source_blur_rectangle_error": "ဖျော့မယ့်နေရာက ဗီဒီယိုဘောင်အတွင်းမှာ ရှိရပြီး အကျယ်နဲ့အမြင့် အနည်းဆုံး 5% ရှိရပါမယ်။",
    "source_blur_strength_error": "ဖျော့အားကို 4 ကနေ 32 အတွင်း ရွေးပါ။",
    "source_blur_time_error": "ဖျော့မယ့်အချိန်ကို မူရင်းဗီဒီယိုအတွင်းမှာ ရွေးပြီး အနည်းဆုံး 0.25 စက္ကန့်ထားပါ။",
    "image_overlay_enable": "ပုံ / လိုဂို ထည့်မယ်",
    "image_overlay_off_summary": "ပိတ်ထားပါတယ် — အရင်ရွေးထားတဲ့ပုံကို မသုံးပါ။",
    "image_overlay_missing_summary": "ပုံတစ်ပုံ ရွေးပါ (PNG၊ JPEG သို့မဟုတ် WebP)။",
    "image_overlay_instruction": "လိုဂို ဒါမှမဟုတ် ပုံ 1 ပုံ ထည့်နိုင်ပါတယ်။ နေရာနဲ့အရွယ်ကို အောက်က ခလုတ်တွေနဲ့ ပြင်နိုင်ပါတယ်။",
    "image_overlay_choose": "ပုံ ရွေးမယ်",
    "image_overlay_replace": "ပုံ ပြောင်းမယ်",
    "image_overlay_remove": "ပုံ ဖယ်မယ်",
    "image_overlay_position_presets": "ပုံထားမယ့်နေရာ",
    "image_overlay_opacity_value": "ပုံကြည်လင်မှု: %1$d%%",
    "image_overlay_time_label": "ပုံကို ဘယ်အချိန်မှာ ပြမလဲ",
    "image_overlay_reset": "ပုံဆက်တင်ကို မူလအတိုင်း ပြန်ထားမယ်",
    "adaptive_badge": "ကလစ်ဖြတ်ဖို့ အကြမ်းရွေးချယ်မှု",
    "adaptive_title": "ကလစ်တွေကို အကြမ်းဖြတ်ကြည့်မယ်",
    "adaptive_description": "ဗီဒီယိုထဲက ထားချင်တဲ့အပိုင်းတွေကို အကြမ်းရွေးပေးမယ်။ ကလစ်တစ်ခုချင်းစီ စမ်းကြည့်ပြီး စိတ်ကြိုက်ဖြစ်မှ အသုံးပြုပါ။ ဒီလုပ်ဆောင်ချက်က AI နဲ့ ဇာတ်ဝင်ခန်းခွဲတာ မဟုတ်ပါ။",
    "adaptive_gentle": "နည်းနည်းပဲ ဖြတ်မယ်",
    "adaptive_balanced": "အလယ်အလတ် ဖြတ်မယ်",
    "adaptive_compact": "ပိုတိုအောင် ဖြတ်မယ်",
    "adaptive_generate": "ကလစ်အကြမ်း ဖန်တီးမယ်",
    "adaptive_empty_summary": "ကလစ်အကြမ်း မရှိသေးပါ။ အခုရွေးထားတဲ့ ဗီဒီယိုအပိုင်းကိုပဲ သုံးပါမယ်။",
    "adaptive_draft_summary": "အကြမ်းကလစ် %1$d ခု • ထားမယ့်အချိန် %2$s • ဖြတ်မယ့်အချိန် %3$s — သုံးမယ်ဆိုရင် အရင်စမ်းကြည့်ပါ။",
    "adaptive_applied_summary": "သုံးထားတဲ့ကလစ် %1$d ခု • ကျန်တဲ့အချိန် %2$s • ဖြတ်ထားတဲ့အချိန် %3$s",
    "adaptive_candidate_title": "ကလစ် %1$d / %2$d",
    "adaptive_previous": "အရင်ကလစ်",
    "adaptive_preview": "ဒီကလစ်ကို စမ်းကြည့်မယ်",
    "adaptive_previewing": "စမ်းပြနေပါတယ်…",
    "adaptive_next": "နောက်ကလစ်",
    "adaptive_sequence_preview": "ကလစ်အားလုံးကို ဆက်တိုက် စမ်းကြည့်မယ်",
    "adaptive_sequence_stop": "စမ်းပြတာ ရပ်မယ်",
    "adaptive_apply": "ဒီကလစ်တွေကို သုံးမယ်",
    "adaptive_apply_note": "ပိတ်ထားရင် ကလစ်အကြမ်းကို စမ်းကြည့်ရုံပဲ ဖြစ်ပြီး မူရင်းရွေးထားတဲ့အပိုင်းကို ဗီဒီယိုထုတ်မယ်။ ဖွင့်ထားရင် စစ်ပြီးသားကလစ်တွေကို အစဉ်လိုက် ဆက်သုံးမယ်။",
    "adaptive_clear": "ကလစ်အကြမ်း ဖျက်မယ်",

    # Crossfade
    "clip_transition_badge": "ကလစ်ကူးပြောင်းမှု",
    "clip_transition_title": "ကလစ် 2 ခုကြား ဘယ်လိုကူးမလဲ",
    "clip_transition_description": "ကလစ်တစ်ခုကနေ နောက်ကလစ်ကို ချက်ချင်းမပြောင်းဘဲ ဖြည်းဖြည်းချောချော ကူးသွားအောင် လုပ်နိုင်ပါတယ်။ မသုံးရင် ပုံမှန်အတိုင်း ချက်ချင်းပြောင်းပါမယ်။",
    "clip_transition_unavailable": "ဒီကူးပြောင်းမှုသုံးဖို့ စစ်ပြီးသားကလစ် အနည်းဆုံး 2 ခု လိုပါတယ်။",
    "clip_transition_boundary_value": "ကူးပြောင်းနေရာ %1$d / %2$d • %3$s → %4$s",
    "clip_transition_previous": "အရင်ကူးပြောင်းနေရာ",
    "clip_transition_next": "နောက်ကူးပြောင်းနေရာ",
    "clip_transition_enable": "ဒီကလစ် 2 ခုကို ဖြည်းဖြည်းကူးမယ်",
    "clip_transition_off_summary": "ပိတ်ထားပါတယ် — နောက်ကလစ်ကို ချက်ချင်းပြောင်းမယ်။",
    "clip_transition_on_summary": "ချောမွေ့ကူးပြောင်းမှု • %1$s • %2$s",
    "clip_transition_duration": "ကူးပြောင်းချိန်",
    "clip_transition_duration_value": "ကြာချိန်: %1$s",
    "clip_transition_easing": "ကူးပြောင်းတဲ့ပုံစံ",
    "clip_transition_linear": "တသမတ်တည်း ကူးမယ်",
    "clip_transition_ease_in_out": "အစနဲ့အဆုံးကို နူးနူးညံ့ညံ့ ကူးမယ်",
    "clip_transition_preview": "ဒီကူးပြောင်းမှု စမ်းကြည့်မယ်",
    "clip_transition_reset": "ကူးပြောင်းမှု ဖယ်မယ်",
    "clip_transition_preview_unavailable": "ဒီစက်မှာ အခုချိန် တိုက်ရိုက်ကြိုကြည့်လို့ မရသေးပါ။ တိုက်ရိုက်ကြိုကြည့်တာကို ထပ်စမ်းပြီးမှ ဒီကူးပြောင်းမှုကို စမ်းကြည့်ပါ။",
    "clip_transition_seconds_value": "%1$s စက္ကန့်",

    # Language
    "settings_language_title": "ဘာသာစကား",
    "settings_language_description": "အက်ပ်ထဲက ခလုတ်၊ ရှင်းလင်းချက်နဲ့ သတိပေးစာတွေကို ဘယ်ဘာသာနဲ့ ပြမလဲ ရွေးပါ။",
    "settings_language_english": "English",
    "settings_language_myanmar": "မြန်မာ",
    "settings_language_note": "ဘာသာစကားပြောင်းရင် ဒီစာမျက်နှာကို ပြန်ဖွင့်ပါမယ်။ လက်ရှိဗီဒီယိုနဲ့ တည်းဖြတ်ထားတာတွေ မပျောက်ပါ။",

    # Export — normal-user copy; technical codec/bitrate details can stay in details.
    "render_failed_title": "ဗီဒီယို မထုတ်နိုင်ပါ",
    "render_cancelled_title": "ဗီဒီယိုထုတ်တာ ရပ်လိုက်ပါပြီ",
    "render_cancelled_summary": "မပြီးသေးတဲ့ ဗီဒီယိုဖိုင်ကို ဖယ်ပြီးပါပြီ။ မူရင်းဗီဒီယို မပြောင်းပါ။",
    "render_elapsed": "ကြာချိန်: %1$s",
    "render_destination": "သိမ်းထားတဲ့နေရာ: %1$s",
    "render_progress_percent": "%1$d%% ပြီးပါပြီ",
    "render_progress_waiting": "ဗီဒီယိုထုတ်ဖို့ ပြင်ဆင်နေပါတယ်…",
    "render_again": "%1$s နဲ့ ထပ်ထုတ်မယ်",
    "render_try_again": "%1$s နဲ့ ထပ်စမ်းမယ်",
    "play_rendered_video": "ထွက်ဗီဒီယိုကို စမ်းကြည့်မယ်",
    "cancel_render": "ဗီဒီယိုထုတ်တာ ရပ်မယ်",
    "cancel_render_title": "အခုထုတ်နေတဲ့ ဗီဒီယိုကို ရပ်မလား?",
    "cancel_render_message": "မပြီးသေးတဲ့ MP4 ဖိုင်ကို ဖယ်ပါမယ်။ မူရင်းဗီဒီယိုကို မပြောင်းပါ။",
    "keep_rendering": "ဆက်ထုတ်မယ်",
    "cancel_and_delete": "ရပ်ပြီး မပြီးသေးတဲ့ဖိုင်ကို ဖျက်မယ်",
    "finish_or_cancel_render": "အခုထုတ်နေတဲ့ ဗီဒီယို ပြီးအောင်စောင့်ပါ၊ ဒါမှမဟုတ် အရင်ရပ်ပါ။",
    "export_badge": "နောက်ဆုံးဗီဒီယို",
    "export_title": "အရည်အသွေးရွေးပြီး ဗီဒီယိုထုတ်မယ်",
    "export_description": "ထုတ်ချင်တဲ့ ရုပ်ထွက်အရည်အသွေးကို ရွေးပါ။ ပြီးရင် RecapFlowAI က ဗီဒီယိုကို စစ်ပြီး ဖုန်းပြခန်းထဲ သိမ်းပါမယ်။",
    "export_video_quality": "ဗီဒီယို အရည်အသွေး",
    "export_duration_advisor_title": "နောက်ဆုံးဗီဒီယို ကြာချိန်",
    "export_duration_advisor_waiting": "ကြာချိန်တွက်ဖို့ ဗီဒီယိုတစ်ခု အရင်ရွေးပါ။",
}

# Conservative phrase-level cleanup for strings not explicitly overridden.
# Avoid broad grammar rewrites; the goal is to remove the most robotic wording
# without changing technical meaning.
SAFE_REPLACEMENTS: tuple[tuple[str, str], ...] = (
    ("အသုံးပြုနိုင်သည်", "သုံးလို့ရပါတယ်"),
    ("အသုံးပြုနိုင်ပါသည်", "သုံးလို့ရပါတယ်"),
    ("အသုံးပြုရန်", "သုံးဖို့"),
    ("အသုံးပြုမည်", "သုံးမယ်"),
    ("လုပ်ဆောင်မည်", "လုပ်မယ်"),
    ("ပြန်ထားမည်", "ပြန်ထားမယ်"),
    ("ရွေးချယ်မည်", "ရွေးမယ်"),
    ("ဖယ်ရှားမည်", "ဖယ်မယ်"),
    ("ထုတ်ယူမည်", "ထုတ်မယ်"),
    ("ပိတ်ထားသည်", "ပိတ်ထားပါတယ်"),
    ("ဖွင့်ထားသည်", "ဖွင့်ထားပါတယ်"),
    ("ဖြစ်သည်။", "ဖြစ်ပါတယ်။"),
    ("ရှိပါသည်။", "ရှိပါတယ်။"),
)


def placeholders(text: str) -> Counter[str]:
    return Counter(FORMAT_RE.findall(text.replace("%%", "")))


def humanize_value(name: str, value: str) -> str:
    if name in EXACT_OVERRIDES:
        return EXACT_OVERRIDES[name]
    updated = value
    for old, new in SAFE_REPLACEMENTS:
        updated = updated.replace(old, new)
    return updated


def process_file(path: Path) -> tuple[int, list[str]]:
    original_text = path.read_text(encoding="utf-8")
    changed_names: list[str] = []

    def replace(match: re.Match[str]) -> str:
        name = match.group("name")
        before = match.group("value")
        after = humanize_value(name, before)

        if placeholders(before) != placeholders(after):
            raise ValueError(
                f"{path.name}:{name}: format placeholders changed: "
                f"{sorted(placeholders(before).elements())} -> "
                f"{sorted(placeholders(after).elements())}"
            )
        if MYANMAR_DIGITS_RE.search(after):
            raise ValueError(f"{path.name}:{name}: Myanmar numeral found; use 0-9 only")

        if after != before:
            changed_names.append(name)
        return f'{match.group("open")}{after}{match.group("close")}'

    updated_text = STRING_RE.sub(replace, original_text)
    if updated_text != original_text:
        path.write_text(updated_text, encoding="utf-8")
    return len(changed_names), changed_names


def main() -> int:
    if not VALUES_MY.is_dir():
        print(f"ERROR: Myanmar resource directory not found: {VALUES_MY}", file=sys.stderr)
        return 2

    total_changed = 0
    touched_files = 0
    seen_names: set[str] = set()

    for filename in TARGET_FILES:
        path = VALUES_MY / filename
        if not path.exists():
            print(f"ERROR: missing expected file: {path}", file=sys.stderr)
            return 2
        count, names = process_file(path)
        if count:
            touched_files += 1
            total_changed += count
            seen_names.update(names)
            print(f"{filename}: updated {count} strings")
        else:
            print(f"{filename}: no changes needed")

    missing_overrides = sorted(set(EXACT_OVERRIDES) - seen_names)
    # Missing here often means the string already has the exact humanized value.
    # Confirm actual key presence before reporting it as a real miss.
    all_text = "\n".join((VALUES_MY / f).read_text(encoding="utf-8") for f in TARGET_FILES)
    truly_missing = [
        name for name in EXACT_OVERRIDES
        if f'name="{name}"' not in all_text
    ]
    if truly_missing:
        print("ERROR: expected string keys not found:", file=sys.stderr)
        for name in truly_missing:
            print(f"- {name}", file=sys.stderr)
        return 2

    if MYANMAR_DIGITS_RE.search(all_text):
        print("ERROR: Myanmar numerals remain in values-my resources", file=sys.stderr)
        return 2

    print(
        f"Human Myanmar copy pass complete: {total_changed} strings updated "
        f"across {touched_files} files; ASCII digits 0-9 preserved."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
