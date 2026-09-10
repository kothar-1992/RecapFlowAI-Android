# RecapFlowAI — Human Myanmar Copy Style

This file defines the user-facing Myanmar language style for RecapFlowAI.

## Goal

Write Myanmar copy the way a helpful person would explain the action in everyday speech: short, clear, direct, and easy to scan on a phone screen.

## Fixed rules

1. Use everyday Myanmar words instead of developer or bureaucratic wording.
2. Prefer short action labels for buttons and toggles.
3. Prefer active sentences: say what the user can do and what will happen next.
4. Keep technical jargon out of normal UI. Put codec, bitrate, runtime, fallback, graph, API, and similar implementation details under technical/details UI only when they are genuinely useful.
5. Do not show milliseconds in normal controls when seconds are easier to understand. Example: `0.3 စက္ကန့်`, not `300 ms`.
6. Use ASCII/English digits `0-9` everywhere. Never use Myanmar numeral glyphs `၀၁၂၃၄၅၆၇၈၉`.
7. Keep required standards and abbreviations such as `720p`, `1080p`, `H.264`, `AAC`, `CPU`, and `RAM` when they are needed for technical details.
8. Preserve Android format placeholders exactly (`%1$s`, `%1$d`, `%1$.0f`, etc.).
9. Avoid literal word-for-word English translation when natural Myanmar wording is clearer.
10. Error text should explain the problem and the next useful action without developer terminology.

## Tone examples

Prefer:
- `ဗီဒီယို ရွေးမယ်`
- `ဒီဖုန်းထဲမှာပဲ လုပ်မယ်`
- `တိုက်ရိုက်ကြိုကြည့်တာ ထပ်စမ်းမယ်`
- `ကလစ် 2 ခုကြား ဘယ်လိုကူးမလဲ`
- `အခုထုတ်နေတဲ့ ဗီဒီယိုကို ရပ်မလား?`

Avoid in normal UI:
- `ဗီဒီယို ထည့်သွင်းခြင်း`
- `လုပ်ဆောင်ချက်ကို အသုံးပြုမည်`
- `runtime fallback`
- `boundary easing`
- `300 ms`

## Translation review checklist

Before accepting a Myanmar copy change:

- Can a first-time user understand it without knowing video-editing jargon?
- Is the button label clear without reading the paragraph below it?
- Is the sentence shorter than the English implementation description when possible?
- Are all numbers still `0-9`?
- Are all format placeholders unchanged?
- Does `python3 scripts/verify_myanmar_localization.py` pass?
