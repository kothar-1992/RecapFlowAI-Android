# Recap Flow AI Android — Active Implementation Plan

- **Project:** RecapFlowAI Android
- **Last updated:** 2026-08-30
- **Verified main baseline:** `7baca862cf7ba02677bebfdba1ada146bd71c1d6`
- **Verified media rollback:** `stable/phase-6f2.8.1`
- **Primary target:** ARM64 Android phones/tablets
- **Primary development / Git environment:** **Termux**
- **AndroidIDE status:** legacy/fallback environment for IDE-specific inspection when needed; it is no longer the primary branch/build workflow
- **UI:** Native Kotlin + XML + ViewBinding
- **Media:** Media3 Composition / CompositionPlayer / Transformer, with FFmpeg/JNI retained for bounded native media support where required
- **Core invariant:** non-destructive editing; one reviewed `EditPlan`; no intermediate MP4 render per feature; exactly one authoritative final `Transformer.start(...)`
- **Core processing:** local/on-device; no VPS dependency for normal editing/export

---

## 1. Verified baseline — Phase 6F.2.8.1 DONE

Phase 6F.2.8.1 is the current verified media baseline.

### Verified on owner device
- [x] 720p H.264 CBR export: 720×1280, target/actual 7.50 Mbps, AAC, Gallery publication PASS.
- [x] 1080p H.264 CBR export: 1080×1920, target 10.00 Mbps, actual 9.11 Mbps, AAC, Gallery publication PASS.
- [x] Duration reconciliation accepts bounded MediaCodec/container finalization drift with 350 ms floor while drifts above 250 ms remain visible.
- [x] Reported average bitrate below 80% of requested CBR target fails validation; 80–90% warns.
- [x] Source-aware 24/25/30/48/50/60 fps policy remains intact; no synthetic promotion of normal-fps sources to 60 fps.
- [x] CompositionPlayer / ExoPlayer preview fallback architecture remains intact.
- [x] Source verifier confirms exactly one final `Transformer.start(...)`.
- [x] Synced unit tests + debug build PASS.
- [x] Finalized output completed downstream SRT/narrator editing and successful TikTok playback, providing practical A/V sanity evidence.

### Repository state
- PR #19 merged Phase 6F.2.8.1 to `main`.
- PR #24 refreshed this active roadmap.
- Issues #17 and #13 are closed completed.
- `stable/phase-6f2.8.1` remains the media rollback checkpoint.

Do not regress this baseline while adding creative features.

---

## 2. Development workflow — Termux primary

The active workflow uses **Termux** for Git, source verification, unit tests and Gradle builds.

```text
GitHub issue / plan
    ↓
Termux git fetch / branch / diff / rebase
    ↓
Termux source verifier + unit tests
    ↓
Termux FFmpeg-enabled assembleDebug
    ↓
install/test on physical Android device
    ↓
owner-device preview/media acceptance
    ↓
PR merge
```

Normal wording: **Termux build/test PASS → owner-device runtime PASS**.

---

## 3. Product objective

Target creator workflow:

```text
Import
  -> Clips
  -> realtime transitions
  -> Transform / Blur / Overlay
  -> animated logo
  -> SRT + Narrator tracks
  -> Hook 0–3s preview
  -> full composition preview
  -> one final export
  -> publish
```

---

## 4. Architecture contract

```text
User controls
    ↓
Immutable reviewed EditPlan
    ↓
Timeline/source-time projection
    ↓
Shared Media3 composition/effect semantics
    ├── CompositionPlayer / explicit preview fallback
    └── Transformer final export
```

Rules:
1. No feature toggle may start an intermediate MP4 render.
2. Preview and export interpret the same timing/geometry/easing/audio semantics.
3. Off means true no-op.
4. Source media is never overwritten.
5. One final encode remains authoritative.
6. FFmpeg remains bounded.
7. Unsupported realtime paths fail explicitly; effects are never silently cleared.
8. Every implementation PR updates PLAN and regression coverage.

---

# ACTIVE FAST TRACK

## Phase 6H.1 — Realtime Clip Transitions — Issue #20

**Status: IN PROGRESS — semantic/EditPlan/two-lane runtime and preview timeline mapping build-verified; Activity integration gate active**

### First vertical slice
**Crossfade only.** Later transition presets remain blocked until Crossfade passes the complete runtime/device gate.

### 6H.1A — semantic model + timeline projection
- [x] Backend-independent `ClipTransitionSettings` / `ClipTransitionBoundary`.
- [x] Adjacent source-range boundary identity.
- [x] CROSSFADE only.
- [x] Linear / Ease-in-out semantic easing.
- [x] 150–1000 ms presentation-duration policy.
- [x] Trim/reviewed Adaptive Cuts/Speed projection.
- [x] Presentation duration remains stable under Speed.
- [x] Deterministic accumulated overlap.
- [x] Unit regression coverage.
- [x] Termux `:app:testDebugUnitTest` PASS.
- [x] Termux FFmpeg-enabled `:app:assembleDebug` PASS.

### 6H.1B — EditPlan + validation integration
- [x] `EditPlan.clipTransitions` added as immutable reviewed metadata.
- [x] Planned output duration subtracts valid Crossfade overlap.
- [x] `EditPlanValidator` maps semantic boundary failures.
- [x] Existing Transform fade remains separate.
- [x] Termux gate PASS: **BUILD SUCCESSFUL in 2m 55s, 28 actionable tasks executed**.

### 6H.1C — shared runtime topology + execution spike
- [x] `Media3CompositionPlan` carries compiled transition topology.
- [x] Explicit capability guard prevents silent hard-cut fallback.
- [x] Capability-guard Termux gate PASS: **BUILD SUCCESSFUL in 2m 53s, 28 actionable tasks executed**.
- [x] Deterministic two-lane clip schedule for overlapping adjacent clips.
- [x] Shared lane-alpha/easing calculation.
- [x] Reject adjacent Crossfades that overlap inside the same middle clip.
- [x] Two-lane topology Termux gate PASS: **BUILD SUCCESSFUL in 2m 55s, 28 actionable tasks executed**.
- [x] Feature-gated `VideoCompositorSettings` runtime primitives.
- [x] Matching PCM audio Crossfade envelope primitive after Speed.
- [x] Runtime-primitives unit gate PASS: **BUILD SUCCESSFUL in 2m 58s, 28 actionable tasks executed**.
- [x] Runtime-primitives assemble gate PASS: **BUILD SUCCESSFUL in 3m 7s, 43 actionable tasks executed**.
- [x] Feature-gated two-lane `Composition` execution wiring with explicit gaps and shared compositor.
- [x] Execution-wiring unit gate PASS: **BUILD SUCCESSFUL in 2m 58s, 28 actionable tasks executed**.
- [x] Execution-wiring assemble gate PASS: **BUILD SUCCESSFUL in 3m 3s, 43 actionable tasks executed**.
- [x] `CompositionPreviewPlayerFactory` can select `MultipleInputVideoGraph.Factory` for two video lanes.
- [x] Crossfade-aware source/output preview timeline mapping authored with dominant-visual overlap semantics.
- [x] Crossfade-aware timeline mapping unit gate PASS: **BUILD SUCCESSFUL in 2m 52s, 28 actionable tasks executed**.
- [x] Crossfade-aware timeline mapping assemble gate PASS: **BUILD SUCCESSFUL in 2m 59s, 43 actionable tasks executed**.
- [x] Auditable MainActivity integration patch staged at `scripts/phase6h1_mainactivity_preview_integration.patch`.
- [ ] `MainActivity` CompositionPlayer creation consumes `CompositionPreviewPlayerFactory` and shared Crossfade-aware seek mapping in committed source.
- [ ] Preview-integration unit + assemble gate PASS.
- [ ] owner-device realtime preview proves multiple-input graph behavior.
- [ ] Runtime failure preserves EditPlan and verified hard-cut path.
- [x] One final `Transformer.start(...)`; no temporary Crossfade MP4 remains the architecture invariant.

### Media3 constraint
The project is pinned to Media3 1.10.0. Official Media3 Composition documentation still lists direct video/audio crossfading as unsupported. Media3 does support overlapping sequences plus custom `VideoCompositorSettings`, including presentation-time-dependent alpha. Therefore the custom compositor path is an explicitly feature-gated runtime spike and is not production-supported until physical-device preview/export evidence passes.

### 6H.1D — realtime boundary controls
- [ ] Select clip boundary.
- [ ] Crossfade ON/OFF.
- [ ] Duration 150–1000 ms.
- [ ] Easing selection.
- [ ] Boundary preview.
- [ ] Reset to hard cut.

### Exit gate
- [ ] Feature-gated runtime build/test PASS after preview integration.
- [ ] owner-device realtime boundary preview PASS.
- [ ] 720p + 1080p Crossfade export PASS.
- [ ] A/V Crossfade quality/sync PASS.
- [ ] combined features still use exactly one final Transformer start.
- [ ] PR #25 merged and Issue #20 completed.

Implementation branch: `feature/phase-6h1-transitions`.

---

## Phase 6H.2 — Animated Logo Overlay + Loop — Issue #21
Starts after Phase 6H.1 semantics/runtime are stable.

## Phase 6G.2 + Phase 6H.3 — SRT and Narrator Timeline Integration — Issues #4 + #22
One authoritative source/presentation timeline; no second timeline model.

## Phase 6H.4 — Hook 0–3 Second Preview — Issue #23
One action loops the opening window using the reviewed EditPlan.

## Phase 6G.3 — Unified Multi-Stage Edit Graph — Issue #5
Semantic order remains deterministic and tested.

## Phase 7 — Persistent Render Job Engine — Issue #6
After creative composition is stable.

---

## Immediate next action
Apply the staged MainActivity integration patch to the active branch so CompositionPlayer creation uses `CompositionPreviewPlayerFactory` and all Composition preview source/output seeks use the plan-aware Crossfade mapping. Then rerun `:app:testDebugUnitTest` and FFmpeg-enabled `:app:assembleDebug` before owner-device validation.
