# Recap Flow AI Android — Active Implementation Plan

- **Project:** RecapFlowAI Android
- **Last updated:** 2026-08-31
- **Verified main baseline:** `7baca862cf7ba02677bebfdba1ada146bd71c1d6`
- **Verified media rollback:** `stable/phase-6f2.8.1`
- **Primary development / Git environment:** **Termux**
- **UI:** Native Kotlin + XML + ViewBinding
- **Media:** Media3 Composition / CompositionPlayer / Transformer, with FFmpeg/JNI retained only for bounded native media support.
- **Core invariant:** one immutable reviewed `EditPlan`; no intermediate MP4 per feature; exactly one authoritative final `Transformer.start(...)`.

---

## Development gate wording

**Termux build/test PASS → owner-device runtime validation PASS.**

---

## Verified baseline — Phase 6F.2.8.1 DONE

- [x] 720p and 1080p H.264/AAC Gallery export accepted on owner device.
- [x] Duration reconciliation and bitrate validation accepted.
- [x] Source-aware frame-rate policy retained.
- [x] Shared preview/export architecture retained.
- [x] Exactly one final `Transformer.start(...)` remains the render invariant.
- [x] `stable/phase-6f2.8.1` remains the rollback checkpoint.

Do not regress this baseline while adding creative features.

---

# Phase 6UX.1 — English/Myanmar Language + Human-Readable Copy — Issue #26

**Status: ACCEPTANCE PASS — repository sync complete; Issue #26 closed completed.**

Implementation branch: `feature/phase-6ux1-language-copy`  
Stacked PR: #27 (base `feature/phase-6h1-transitions`)  
Final localization commit: `ada027f846dffe2c8c8d0c8b0976c578e8996d01`

### Implemented
- [x] Settings includes explicit `English` / `မြန်မာ` language controls.
- [x] AppCompat per-app locale switching and persistence are wired.
- [x] Android locale config declares `en` and `my`.
- [x] Language changes do not alter media/EditPlan state and do not start rendering.
- [x] Myanmar resources cover Home, Editor, Settings, Clips, Transform, Audio, Overlay, Adaptive Clips, Export/Render, diagnostics, and Crossfade surfaces.
- [x] Normal UI copy is rewritten into plain conversational Myanmar instead of literal engineering terminology.
- [x] Normal duration copy uses human-readable seconds instead of raw millisecond values.
- [x] Myanmar UI keeps English/Arabic digits `0-9`; Myanmar digit glyphs are prohibited.
- [x] Localization verifier checks coverage, format placeholders, XML safety, and numeral policy.
- [x] Python `__pycache__` / `*.py[cod]` ignored by Git.

### Acceptance evidence
- [x] Human Myanmar final polish PASS.
- [x] XML structure validation PASS.
- [x] Localization verifier PASS: **479 strings covered; English numerals `0-9` policy satisfied**.
- [x] `git diff --check` PASS.
- [x] Fresh Termux `:app:testDebugUnitTest` PASS (owner report).
- [x] Fresh FFmpeg-enabled `:app:assembleDebug` PASS (owner report).
- [x] Owner-device English ↔ မြန်မာ switch PASS.
- [x] Language persists after restart.
- [x] Owner-device wording/layout/numeric-format review PASS.

### Repository completion gate
- [x] Final approved Human Myanmar/resource-duration edits committed and pushed to PR #27 head.
- [x] PR #27 head confirmed at `ada027f846dffe2c8c8d0c8b0976c578e8996d01`.
- [x] Issue #26 closed completed.
- [ ] GitHub PR #27 draft flag still needs to be switched to Ready for review; connector mutation failed after source validation. The PR is otherwise mergeable and acceptance-complete.

PR #27 remains stacked on `feature/phase-6h1-transitions`; preserve stack ordering while PR #25 remains the active Crossfade branch.

---

# Phase 6H.1 — Realtime Clip Transitions — Issue #20

**Status: IN PROGRESS — implementation/build gates passed; owner-device Crossfade runtime remains unverified.**

First vertical slice: **Crossfade only**.

### 6H.1A/B/C
- [x] Semantic `ClipTransitionSettings` / boundaries.
- [x] Source-boundary identity and speed-aware presentation projection.
- [x] `EditPlan.clipTransitions` and duration/validation integration.
- [x] Deterministic two-lane overlapping schedule.
- [x] Shared compositor alpha/easing primitives.
- [x] PCM Crossfade envelope after Speed.
- [x] Feature-gated Media3 Composition execution path.
- [x] Multiple-input preview graph selection authored.
- [x] Crossfade-aware source/output preview mapping authored.
- [x] MainActivity preview factory/timeline integration committed on the transition branch.
- [x] Unit/assemble gates for these slices PASS.

### 6H.1D — boundary controls
- [x] Boundary selector and source-identity editor policy.
- [x] Crossfade ON/OFF.
- [x] 150–1000 ms semantic duration policy; normal UI presents human-readable seconds.
- [x] Linear / ease-in-out choice.
- [x] Focused boundary preview action; no intermediate encode.
- [x] Reset-to-direct-change action.
- [x] MainActivity integration exists remotely.
- [x] 6H.1D Termux unit gate PASS (owner report).
- [x] 6H.1D Termux assemble gate PASS (owner report).

### Runtime status
Owner-device screenshot previously reached the transition UI but reported that realtime Crossfade preview was unavailable on the current preview fallback. Therefore Crossfade runtime acceptance is **not** inferred from the Phase 6UX.1 language/device PASS.

### Exit gate
- [ ] Owner-device realtime Crossfade boundary preview PASS.
- [ ] Runtime failure preserves EditPlan and hard-cut/direct-change fallback state.
- [ ] 720p Crossfade export PASS.
- [ ] 1080p Crossfade export PASS.
- [ ] A/V Crossfade quality/sync PASS.
- [ ] Combined features still use exactly one final `Transformer.start(...)`.
- [ ] PR #25 merged and Issue #20 completed.

Implementation branch: `feature/phase-6h1-transitions`.

---

## Next phases

- **6H.2 — Animated Logo Overlay + Loop — Issue #21:** starts after Crossfade runtime semantics are stable.
- **6G.2 + 6H.3 — SRT + Narrator Timeline — Issues #4 + #22:** use the same authoritative source/presentation timeline.
- **6H.4 — Hook 0–3 Second Preview — Issue #23.**
- **6G.3 — Unified Multi-Stage Edit Graph — Issue #5.**
- **Phase 7 — Persistent Render Job Engine — Issue #6.**

---

## Immediate next action

1. Resume Phase 6H.1 owner-device Crossfade runtime validation; do not treat Phase 6UX.1 language/device validation as Crossfade runtime proof.
2. Prove realtime boundary preview or capture the precise fallback/runtime failure path without altering the reviewed EditPlan.
3. After preview PASS, validate 720p + 1080p Crossfade export and A/V sync while preserving the one-final-Transformer invariant.
