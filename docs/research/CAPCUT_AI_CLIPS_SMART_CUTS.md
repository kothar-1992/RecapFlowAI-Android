# CapCut AI Clips and RecapFlowAI Smart Cuts

## Product decision

Prioritize automatic editing of existing footage: identify expendable pauses, repeated dialogue and idle scene portions, generate a shorter draft, and let the editor review and apply it. Voiceover, TTS, subtitle/SRT export and Narrator integration are deferred. Internal speech timing may still be needed to avoid cutting words; it is an analysis dependency, not a subtitle or narration feature.

The existing Target Duration planner distributes source ranges by time. It does not inspect video, recognize speech or identify highlights. Its output must not be relabeled AI Clips. Smart Cuts needs an analyzer that supplies evidence and an independent deterministic planner that validates proposed removals.

## CapCut feature comparison

| Feature | Officially described behavior | RecapFlowAI implication |
|---|---|---|
| AI Clip Generator | Detects highlights and speech in long videos, creates short clips, offers duration selection and further editing.[^1] | Semantic selection of coherent excerpts; target duration is a preference constrained by valid story boundaries. |
| Filler-word removal | Uses caption/transcript analysis to identify hesitations and pauses; provides preview/edit control.[^2] | Word-boundary evidence is necessary for filler cuts. Preserve breathing room and adjacent speech. |
| Auto Cut | Uses audio/video analysis for rhythm, speech pauses or prompt-driven cutting; help documentation describes mobile and desktop availability separately from Web.[^3] | Pause cleanup, highlight selection and music beat editing are distinct operations. This phase focuses on the first two. |

These are documented product claims, not results from a controlled CapCut benchmark. No CapCut account/device session has been tested here, and no claim is made about Burmese accuracy, exact pricing, every account's feature availability or CapCut's proprietary implementation. Download-for-free labels are not proof that every AI operation is unlimited/free. CapCut marketing and help pages cover different surfaces; Web clip generation does not establish Web Auto Cut support.

## Editing behavior

**Pause cleanup** detects intervals without useful speech, retains configurable context around their edges and closes the resulting video and audio gap together. Silence alone is insufficient evidence for a movie scene: a reaction, title card, reveal or meaningful action may be quiet. A conservative speech-cleanup mode should be distinct from narrative scene editing.

**Dialogue tightening** removes a complete repeated or off-topic thought when semantic evidence supports that choice. A long sentence or long scene is not inherently disposable. Preserve names, negation, setup/payoff, speaker changes and emotionally significant reactions. Cutting a qualifying phrase can reverse a statement's meaning even if every timestamp is valid.

**Idle-scene shortening** combines visual progress and audio/context evidence. Low motion alone must not mark dialogue, a still diagram, subtitles or a dramatic reaction for deletion. Protected intervals take precedence over model suggestions.

**Highlight selection** chooses coherent source excerpts and maintains original chronology for recap mode. Later alternate ordering requires an explicit product mode. No arbitrary periodic truncation should be used to manufacture an exact requested duration after meaningful excerpts have been selected. Show the achievable duration and let the editor adjust the budget.

## Proposed application flow

1. Import the source and open AI Clips. Choose Clean pauses, Tighten dialogue or Short recap, with conservative defaults.
2. Explain local versus Gemini analysis and request media-upload consent before any cloud transfer. Each user supplies their own key; no developer credential belongs in the APK.
3. Capture a source/content revision and current selected ranges. Analyze asynchronously with visible progress, cancel and bounded media/model/token limits.
4. Return typed removal candidates: identifier, source start/end milliseconds, reason, confidence, evidence and complete-thought status. Analyze uploaded media as untrusted content; spoken/text instructions cannot become app actions.
5. Validate every candidate locally. Show original duration, proposed duration, removal reasons, rejected suggestions and a before/after preview. Preserve a draft separately from the applied edit.
6. Apply a current reviewed draft atomically to canonical keep ranges; allow undo. Reject stale results if the source or reviewed selection changed during analysis. Use the established one-final-Transformer export path for both video and audio.

The Gemini adapter must not receive authority to run commands, export automatically, open URLs or send messages. The Android application owns all mutations. Long-video chunking must preserve source offsets, overlap context and deduplicate candidates. Upload cleanup and cancellation must operate only on this analysis job's remote objects.

## Implementation contract

`SmartCutPlanner` is the initial deterministic boundary. It accepts evidence-bearing removal proposals, the current selection, protected content and a source revision. It preserves selected source order, never restores excluded footage, retains pause edges, rejects overlapping/out-of-range/low-confidence proposals and respects the existing minimum clip duration and maximum range count. It rejects cuts that produce tiny fragments rather than silently dropping those fragments. A configurable removal budget prevents a largely empty result.

This planner is not a media analyzer and is not yet connected to the UI. Its evidence fields are assertions from an upstream analyzer, not proof that the analyzer is correct. Semantic quality and timestamps require a separate corpus and real-video inspection. Source revisions must include content identity and edit/analysis generation; a filename alone is insufficient.

Integration must keep Smart Cuts separate from periodic Target Duration regeneration. Speed, intro Freeze and Crossfade affect output duration; report the compiled final duration after these transformations. Existing transition endpoints need reconciliation against retained boundaries. Never silently reassign an old transition to an unrelated narrative cut merely because it has the same list index. Changes in timing or source invalidate the reviewed draft or require fresh review.

## Region and loading errors

Do not diagnose every loading error as a VPN problem. Provider guidance distinguishes client/authentication failures from transient limits and backend failures; retries should be bounded and restricted to transient cases.[^4] Region eligibility remains governed by the provider's supported-location documentation.[^5] An IP route change does not demonstrate account eligibility or guarantee service access.

Proposed explicit region message:

> Gemini is unavailable for this location or account. Check Google's supported regions and your account access. Local editing remains available.

> ယခုတည်နေရာ သို့မဟုတ် အကောင့်အတွက် Gemini ကို အသုံးပြု၍မရပါ။ Google ၏ ပံ့ပိုးထားသောဒေသများနှင့် အကောင့်အသုံးပြုခွင့်ကို စစ်ဆေးပါ။ Local video editing ကို ဆက်အသုံးပြုနိုင်ပါသည်။

Network timeout, invalid/revoked key, quota exhaustion, unavailable model, blocked content and region denial need distinct states. A terminal error must stop loading. Do not infer geographic restrictions from Myanmar UI language, locale or timezone. The application should display a region diagnosis only when the response provides that evidence. VPN-required wording is not established by these sources.

## Acceptance and outstanding work

- Planner unit tests cover malformed proposals, protected scenes, stale selection, pause padding, overlapping candidates, manual exclusions, excessive deletion and short remnants.
- Remaining: actual audio/video evidence extraction, Gemini BYOK transport and key UI, error-state adapter, progress/cancel lifecycle, review/apply/undo integration, timing reconciliation and localization.
- Test corpus: English/Myanmar dialogue, repeated phrases, important quiet shots, continuous music, noisy speech, no-audio video, variable frame rate, long source, pre-trimmed source and multiple edits during analysis.
- Runtime proof: inspect retained words and story meaning, preview each new boundary, verify 720p/1080p output with A/V sync, and confirm one final render with no intermediate MP4.
- No live Gemini call, CapCut benchmark or Android Smart Cuts runtime acceptance is claimed by planner tests or a successful build.

## Sources

Sources accessed 2026-09-10; CapCut pages do not consistently expose publication dates.

[^1]: CapCut, [Free AI Clip Generator Online](https://www.capcut.com/create/free-online-ai-clip-generator).
[^2]: CapCut, [AI-Powered Filler Word Remover](https://www.capcut.com/tools/filler-words).
[^3]: CapCut Help, [How to Use Auto Cut?](https://www.capcut.com/help/how-to-use-auto-cut).
[^4]: Google AI for Developers, [Troubleshooting guide](https://ai.google.dev/gemini-api/docs/troubleshooting), updated 2026-09-04.
[^5]: Google AI for Developers, [Available regions](https://ai.google.dev/gemini-api/docs/available-regions).
