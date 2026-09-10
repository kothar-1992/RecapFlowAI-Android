# Recap Flow AI Android — Active Implementation Plan

- **Project:** RecapFlowAI Android
- **Last updated:** 2026-09-10
- **Verified media rollback:** `stable/phase-6f2.8.1`
- **Active transition baseline:** `feature/phase-6h1-transitions`
- **Target-duration Clips merge:** `5cdf300e12e62bd1cdb32dc3cc4e90ec5270fd3f`
- **Primary development / Git environment:** **Termux**
- **Additional verified environment:** Windows / VS Code; `scripts/build_debug.bat` avoids PowerShell execution-policy and Java socket-directory failures.
- **Accepted side-menu merge:** `45d65e4e9cb374b6e361f6e8807fc64271abbda3` — PR #34 into the transition stack.
- **Next priority:** Gemini public-app BYOK foundation and reviewed AI recap planning — Issue #36, before SRT/Narrator.
- **UI:** Native Kotlin + XML + ViewBinding
- **Media:** Media3 Composition / CompositionPlayer / Transformer, with FFmpeg/JNI retained only for bounded native media support.
- **Core invariant:** one immutable reviewed `EditPlan`; no intermediate MP4 per feature; exactly one authoritative final `Transformer.start(...)`.

---

## Development gate wording

**Termux build/test PASS → owner-device runtime validation PASS.**

Canonical Termux/AndroidIDE gate uses Gradle 9.0.0, `$PREFIX/bin/aapt2`, FFmpeg enabled, Crossfade runtime enabled, `--rerun-tasks`, and `--stacktrace`.

---

## Verified baseline — Phase 6F.2.8.1 DONE

- [x] 720p and 1080p H.264/AAC Gallery export accepted on owner device.
- [x] Duration reconciliation and bitrate validation accepted.
- [x] Shared preview/export architecture retained.
- [x] Exactly one final `Transformer.start(...)` remains the render invariant.
- [x] `stable/phase-6f2.8.1` remains the rollback checkpoint.

Do not regress this baseline while changing timeline semantics.

---

# Phase 6UX.1 — English/Myanmar Language + Human-Readable Copy — Issue #26 / PR #27

**Status: COMPLETE AND MERGED INTO THE ACTIVE TRANSITION STACK.**

- [x] English / မြန်မာ switch.
- [x] AppCompat per-app locale persistence.
- [x] `en` / `my` locale config.
- [x] Human-readable Myanmar UI coverage.
- [x] Arabic digits `0-9` policy; Myanmar digit glyphs prohibited.
- [x] Localization verifier / XML / placeholder checks PASS.
- [x] Termux unit + assemble PASS (owner report).
- [x] Owner-device language switch/persistence/layout review PASS.
- [x] Issue #26 closed.
- [x] PR #27 merged into `feature/phase-6h1-transitions`.

Localization is no longer a separate blocker. New UI work must add both English and Myanmar copy from the start.

---

# Phase 6H.1 — Realtime Clip Transitions — Issue #20 / PR #25

**Status: SOURCE + TERMUX GATES PASS; OWNER-DEVICE CROSSFADE RUNTIME STILL NOT ACCEPTED.**

Current PR scope remains **Crossfade only**.

### Implemented
- [x] semantic per-boundary Crossfade model
- [x] 150–1000 ms duration + easing policy
- [x] immutable `EditPlan.clipTransitions`
- [x] deterministic two-lane overlap topology
- [x] shared compositor/audio envelope semantics
- [x] source↔output preview mapping
- [x] realtime boundary controls integrated
- [x] Termux unit + assemble gates reported PASS
- [x] localization merged into the active branch
- [x] accepted Random Mirror feature is integrated into this active stack

### Remaining Crossfade gate
- [ ] realtime owner-device Crossfade preview on the actual composition path
- [ ] explicit safe behavior if preview path is unsupported
- [ ] 720p Crossfade export PASS
- [ ] 1080p Crossfade export PASS
- [ ] A/V quality/sync PASS
- [ ] one-final-Transformer invariant preserved

Do not expand transition families while Phase 6H.2 is landing.

---

# Phase 6H.1E — Deterministic Per-Clip Random Mirror — Issue #28 / PR #29

**Status: COMPLETE AND INTEGRATED.**

- [x] global Mirror retained
- [x] separate Random mirror each clip mode
- [x] deterministic clip-identity decision; no runtime `Random`
- [x] global/random mutual exclusion
- [x] single-clip no-op
- [x] Crossfade source-index preservation
- [x] Intro Freeze matches first moving clip orientation
- [x] preference/state persistence
- [x] ViewBinding budget fix via child Mirror-controls layout
- [x] refreshed `testDebugUnitTest` PASS (owner report)
- [x] refreshed `assembleDebug` PASS (owner report)
- [x] `git diff --check` PASS
- [x] Myanmar localization verifier PASS
- [x] Arabic digits `0-9` policy PASS
- [x] owner-device verification PASS (owner report)
- [x] PR #29 merged into `feature/phase-6h1-transitions`
- [x] Issue #28 closed completed

---

# Phase 6H.1F / 6H.1F.2 — Target-Duration Clips — Issue #30 / PR #31

**Status: COMPLETE, VALIDATED, MERGED.**

- [x] standalone head/tail Trim removed from the normal user-facing Clips workflow
- [x] full-source `TrimRange` retained only as an internal timeline boundary
- [x] Target Duration is the primary Clips authority
- [x] deterministic whole-source range planning
- [x] `03:00 → 01:00` and `03:00 → 02:00` planner coverage
- [x] generated clips remain chronological and reviewable
- [x] duplicate Gentle/Balanced/Compact + Generate draft + Apply controls removed from normal Target mode
- [x] Speed + Crossfade + Intro Freeze reconciliation preserves requested target within tolerance
- [x] canonical `MainActivity.kt`/localization source committed; no local-only patch state required
- [x] owner-device Target Duration → Review → Export validation PASS
- [x] owner explicitly confirmed exported video correct
- [x] canonical Termux `testDebugUnitTest` PASS
- [x] canonical Termux `assembleDebug` PASS
- [x] PR #31 merged as `5cdf300e12e62bd1cdb32dc3cc4e90ec5270fd3f`
- [x] Issue #30 closed completed

Architecture contract now established:

`Import → Target-duration Clips planning → Review → optional effects → Preview → one final export`

---

# Phase 6H.2 Animated Logo / Loop — Issue #21 / PR #32

**Status: COMPLETE, OWNER-ACCEPTED, MERGED INTO THE ACTIVE TRANSITION STACK.**

GitHub PR #32 is merged as `599eb91b82bdef80888e8d0356653f00d5543930`.
The PR's 2026-09-02 owner-device evidence records realtime animation and final
1080p export acceptance. This is historical device evidence, not a new device run.

The static image/logo overlay is extended with semantic animation metadata rather than temporary rendered logo clips.

### Product contract
- keep current PNG/JPEG/WebP logo import, normalized position, size, opacity and source-time start/end window
- presets: None/static, Fade, Fade + scale, Pop, Slide, Pulse, Float, Rotate, Bounce
- loop ON/OFF
- animation duration
- loop period/interval
- deterministic bounded looping inside the configured overlay window
- animation timing survives Target-duration Clips boundaries and Speed without preview/export phase drift
- same Media3/OpenGL effect path for preview and final Transformer export
- no temporary animated-logo MP4
- exactly one final Transformer export

### Foundation — PASS
- [x] first-class `ImageOverlayAnimationPreset` semantics in canonical `EditPlan`
- [x] loop/duration/period settings with static `NONE` default for backward compatibility
- [x] deterministic pure `ImageOverlayAnimationPolicy`
- [x] compiler-owned phase offset so a later reviewed clip resumes source-anchored loop phase instead of restarting
- [x] CompositionPlayer Speed projection scales window + animation duration + period + phase offset together
- [x] validation contract for animation duration/period
- [x] JVM timing/phase tests
- [x] owner-reported canonical `testDebugUnitTest` BUILD SUCCESSFUL
- [x] owner-reported canonical `assembleDebug` BUILD SUCCESSFUL

### Shared OpenGL animation slice
- [x] deterministic visual curves for Fade, Fade+Scale, Pop, Slide, Pulse, Float, Rotate and Bounce
- [x] shared `StaticImageOverlayEffect` consumes canonical phase/visual semantics for preview/export
- [x] `NONE` preserves dedicated legacy static shader behavior
- [x] inverse texture mapping supports translation, scale and rotation
- [x] transformed logo geometry remains frame-safe after aspect conversion
- [x] JVM visual-curve / frame-safety / Speed parity tests added
- [x] first refreshed gate exposed a Float exact-equality-only failure in `identityKeepsResolvedCenterAndScale`
- [x] test fixed to use epsilon comparisons for normalized Float geometry; production geometry policy unchanged
- [x] refreshed Termux `testDebugUnitTest` PASS after float-safe fix
- [x] refreshed Termux `assembleDebug` PASS after float-safe fix

### Completed UI and runtime acceptance
- [x] realtime Animation Preset / Loop / Duration / Period controls
- [x] English + Myanmar copy
- [x] preferences and Activity state restore with static defaults
- [x] shared preview/export animation semantics and source/build verifiers
- [x] owner-device realtime controls and rendered-output acceptance, including documented 1080p validation

---

# Phase 6UX.2A/B Side Menu — Issue #33 / PR #34

**Status: A/B OWNER-ACCEPTED AND MERGED; #33 CLOSED. ACCOUNT MODEL SPLIT INTO #35.**

- [x] 6UX.2A drawer shell, Guest header, runtime version, EN/MY copy and scrim accepted on owner tablet (PR #34 discussion, 2026-09-02).
- [x] 6UX.2B configured email/Telegram/Facebook intents and native legal dialogs implemented.
- [x] MainActivity controller binding and Back-before-bottom-navigation integration included in canonical source; no apply-script step required after checkout.
- [x] Windows/VS Code `.bat` and PowerShell build helpers; local Java socket directory and quoted Gradle properties.
- [x] side-menu source verifier and localization verifier (553 strings) PASS on 2026-09-10.
- [x] Windows unit tests and FFmpeg/Crossfade-enabled assemble PASS on 2026-09-10.
- [x] 6UX.2B owner-device email/Telegram/Facebook actions (owner report, 2026-09-10).
- [x] 6UX.2B all four legal dialogs, EN/MY text and Back behavior (owner report, 2026-09-10).
- [x] PR #34 merged at the owner's request with current Windows build/tests and device evidence.
- [ ] 6UX.2C account/user-level model boundary: tracked separately in #35; auth remains deferred.

Missing-handler fallback is source-verified; no separate no-handler device test
is claimed. Historical Termux and current Windows gates remain distinct evidence.
Public-store policies/licenses, authentication and Ads are still future release
work. See `docs/PHASE6UX2_WINDOWS_REVIEW_2026-09-10.md` for the dated audit.

---

# Next work — Gemini AI / BYOK — Issue #36

**Status: SMART CUTS RESEARCH COMPLETE; DETERMINISTIC PLANNER IMPLEMENTED; ANALYSIS AND UI INTEGRATION PENDING.**

- Product: public app, each user supplies their own Gemini key.
- Research: `docs/research/GEMINI_BYOK_RECAPFLOWAI.md` (37 cited sources and 9 GitHub references).
- First useful AI scope (owner update): CapCut-style AI Clips — remove evidenced pauses, redundant dialogue and idle scene portions; review and apply the edited footage. Script generation is no longer the first deliverable.
- CapCut feature mapping and acceptance: `docs/research/CAPCUT_AI_CLIPS_SMART_CUTS.md`.
- SmartCutPlanner now validates evidence-bearing removals into canonical chronological keep ranges. This is not yet a media analyzer or a connected UI feature.
- API/eligibility: use current auth-key/API guidance; official region list does not list Myanmar. Resolve eligible distribution before cloud rollout.
- Transport: decide a BYOK backend relay or separately reviewed direct-client exception; Firebase app-project auth is not arbitrary user-key BYOK.
- Secrets: session-only default, optional Keystore protection with backup/export exclusions, no keys in APK/logs.
- Data: explicit media consent, provider disclosure, owned-upload cleanup, cloud policy copy before enabling public AI features.
- Invariant: AI proposes; local validation and user Apply produce EditPlan; one final Transformer remains authoritative.
- No live Gemini key/account/quota/media validation is claimed yet.

---

## Deferred after the Gemini connection and recap foundation

- **Voiceover/TTS + 6G.2 SRT/Text — Issue #4 + 6H.3 Narrator — Issue #22:** explicitly postponed behind AI Clips by owner request; later implement together against the authoritative Clips/timing model. Internal speech timing for cuts does not enable narration or subtitle export.
- **6H.4 Hook 0–3s Preview — Issue #23:** wait until opening composition includes stable Clips/SRT/Narrator timing.
- **Timed Video Overlay — Issue #3:** resume on the established source→presentation projection model.
- **6G.3 Unified Multi-Stage Edit Graph — Issue #5:** consolidation after animated overlay and text/narrator timing are proven.
- **Phase 7 Persistent Render Job Engine — Issue #6:** wrap the stable final graph, not a moving architecture.
- **FFmpegAndroid research — Issue #7:** low priority; no architecture replacement during core workflow work.

---

## Immediate next actions

1. Complete #36 AI Clips: actual media evidence extraction, Gemini BYOK connection/secret handling, progress/cancel and distinct error states, then review/apply/undo integration and real-video validation. Do not label the existing periodic duration planner as AI.
2. Keep unsupported-region/offline editing functional; do not describe free quota as unlimited or automatically billable requests as free.
3. Resolve the remaining PR #25 owner-device Crossfade preview, 720p/1080p export and A/V acceptance before merging the transition stack to `main`.
4. Resume #4/#22 only after Gemini recap foundation; preserve #35 account-model follow-up without adding fake sign-in.
