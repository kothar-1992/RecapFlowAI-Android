# Recap Flow AI Android — Active Implementation Plan

- **Project:** RecapFlowAI Android
- **Last updated:** 2026-08-29
- **Verified main baseline:** `6b937d32519eb6bc3342e1e17850754dd6732928`
- **Rollback branch:** `stable/phase-6f2.8.1`
- **Primary target:** ARM64 Android phones/tablets
- **UI:** Native Kotlin + XML + ViewBinding
- **Media:** Media3 Composition / CompositionPlayer / Transformer, with FFmpeg/JNI retained for native media support where required
- **Core invariant:** non-destructive editing; one reviewed `EditPlan`; no intermediate MP4 render per feature; exactly one authoritative final `Transformer.start(...)`
- **Core processing:** local/on-device; no VPS dependency for normal editing/export

---

## 1. Verified baseline — Phase 6F.2.8.1 DONE

Phase 6F.2.8.1 is the current stable implementation baseline.

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
- Issues #17 and #13 are closed completed.
- Draft PR #18 was administratively replaced by #19 without changing tested source.
- `stable/phase-6f2.8.1` is the rollback checkpoint.

Do not regress this baseline while adding creative features.

---

## 2. Product objective for the next implementation track

The local editor now exports usable final-quality video. The next goal is to remove the remaining creative-workflow round trips and make editing decisions previewable before export.

Current external round trip:

```text
RecapFlowAI
  -> Clips / Transform / Blur / Logo
  -> Export
  -> CapCut
  -> add SRT
  -> add Narrator voice
  -> re-align video/audio
  -> publish
```

Target workflow:

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

Recent publishing evidence also shows the strongest audience loss occurs around the opening 0:02 region, so rapid first-seconds preview is a product requirement rather than only an editing convenience.

---

## 3. Architecture contract for all next phases

All new features must compile through the existing shared project model rather than creating independent render pipelines.

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

1. **No feature toggle may start an intermediate MP4 render.**
2. **Preview and export must interpret the same timing, geometry, easing, opacity and audio semantics.** Preview may reduce quality for performance, but may not change meaning.
3. **Off means true no-op.** Disabled optional features must not alter output.
4. **Source media is never overwritten.** Failure/cancel/unsupported preview must preserve source and reviewed `EditPlan`.
5. **One final encode remains authoritative.** Combined edits must still end in one final `Transformer.start(...)`.
6. **FFmpeg remains bounded.** Use JNI/FFmpeg for probe/native utilities/specialist media tasks; do not introduce a second independent preview/export semantics path.
7. **Device capability fallback must be explicit.** Never silently clear an effect because realtime preview is unsupported.
8. **Every implementation PR updates this PLAN and adds regression coverage.**

---

# ACTIVE FAST TRACK

## Phase 6H.1 — Realtime Clip Transitions — Issue #20

**Status: NEXT IMPLEMENTATION GATE**

### Goal
Add clip-boundary transitions that can be tested in realtime before export.

### Initial transition set
- None
- Crossfade
- Fade through black
- Slide left/right
- Zoom
- Blur dissolve

### Required model
Introduce a semantic transition definition rather than backend filter strings, conceptually:

```text
TransitionSettings
- boundary identity / adjacent clip IDs
- type
- durationMs
- easing
- enabled
```

### Required behavior
- transition is attached to a clip boundary, not baked into either source clip;
- duration is validated against participating clip lengths;
- projection remains correct after Trim, reviewed Adaptive Cuts and Speed;
- realtime boundary preview reuses shared effect/composition semantics;
- unsupported realtime path falls back cleanly without changing the edit;
- final export stays one pass.

### Exit gate
- [ ] unit tests for boundary timing, short clips, speed, disabled state;
- [ ] `:app:testDebugUnitTest` PASS;
- [ ] FFmpeg-enabled `:app:assembleDebug` PASS;
- [ ] owner-device realtime preview PASS;
- [ ] 720p + 1080p transition export PASS;
- [ ] combined current features + transition still show exactly one final Transformer start;
- [ ] merge scoped PR and mark #20 completed.

**Implementation branch should start from `main` / `stable/phase-6f2.8.1`, not from the old Phase 6G.1 draft branch.**

---

## Phase 6H.2 — Animated Logo Overlay + Loop — Issue #21

Starts after Phase 6H.1 semantics are stable.

### Goal
Upgrade the existing static image/logo overlay into a motion overlay while preserving current geometry and timing behavior.

### Initial presets
- Static / None
- Fade
- Fade + scale
- Pop
- Slide
- Pulse
- Float
- Rotate
- Bounce

### Required model
Conceptually extend overlay settings with:

```text
OverlayMotionSettings
- preset
- enabled
- loopEnabled
- animationDurationMs
- loopPeriodMs
- easing
```

Position, scale, opacity and source-time window remain authoritative existing overlay properties.

### Rules
- no temporary animated-logo video;
- animation phase is deterministic from timeline time;
- loop is bounded by configured overlay start/end;
- aspect conversion may not move the overlay outside the output frame;
- preview/export must share animation phase/easing.

### Exit gate
- [ ] static behavior is unchanged when motion is disabled;
- [ ] realtime preset preview PASS;
- [ ] loop ON/OFF behavior PASS;
- [ ] Trim/Adaptive Cuts/Speed timing tests PASS;
- [ ] transition + animated logo combined export remains one final render;
- [ ] owner-device 720p/1080p PASS;
- [ ] merge scoped PR and close #21.

---

## Phase 6G.2 + Phase 6H.3 — SRT and Narrator Timeline Integration — Issues #4 + #22

These two issues form one product milestone: remove the mandatory CapCut round trip for subtitles and narration.

### Subtitle/SRT — #4
- import SRT;
- Unicode Burmese shaping/wrapping;
- position/alignment/safe margins;
- outline/shadow;
- source/EditPlan timing projection;
- preview/final burn-in parity;
- source-subtitle Blur remains independent.

### Narrator audio — #22
- import one narrator audio asset;
- enable/disable;
- start offset + trim;
- gain/mute/fade;
- waveform/timeline visualization where practical;
- explicit source-audio policy;
- deterministic duration/A-V reconciliation.

### Shared timing rule
SRT and narrator must use the same authoritative source/presentation mapping used by the reviewed `EditPlan`. Do not create a second independent "CapCut-style" timeline model inside the app.

### Exit gate
- [ ] SRT remains aligned through Trim/Adaptive Cuts/Speed;
- [ ] Burmese text renders correctly;
- [ ] narrator remains synchronized after video edits;
- [ ] preview contains the same subtitle/narrator timing as final export;
- [ ] source audio behavior is explicit and testable;
- [ ] transition + logo + blur + SRT + narrator maximal combination still uses one final Transformer export;
- [ ] owner-device A/V sync + 720p/1080p PASS;
- [ ] complete #4 and #22 only after full combined workflow evidence.

---

## Phase 6H.4 — Hook 0–3 Second Preview — Issue #23

### Goal
Make opening optimization fast enough to repeat many times without full-video seeking or rendering.

### Required behavior
- one action enters `Preview Hook 0–3s`;
- loop the hook window;
- immediate reflection of first clip, transition, logo, subtitles and narrator changes;
- quick seek to zero;
- normal preview position/play intent restores predictably on exit;
- uses current `EditPlan`, not a separate project state;
- no intermediate media file.

### Exit gate
- [ ] short projects handled safely;
- [ ] loop/seek restoration tests PASS;
- [ ] opening effects/audio update immediately;
- [ ] owner-device interaction PASS;
- [ ] merge scoped PR and close #23.

---

# CONSOLIDATION

## Phase 6G.3 — Unified Multi-Stage Edit Graph — Issue #5

After the fast-track features are individually proven, consolidate them into one deterministic stage order and regression suite.

Target conceptual order:

```text
Source
 -> Trim / Adaptive Cuts
 -> Clip Boundary Transitions
 -> Aspect / Crop / Mirror / Color
 -> Speed / Freeze
 -> Source Subtitle Blur
 -> Static + Animated Logo / Image Overlay
 -> Subtitle / Text
 -> Narrator + Source Audio Mix
 -> Output Presentation
 -> Final Encode
```

The exact implementation may fuse GPU stages, but semantic ordering must be documented and tested.

Exit gate:
- [ ] maximal combined `EditPlan` compiles deterministically;
- [ ] no tab owns a private conflicting interpretation of settings;
- [ ] preview/export do not silently diverge;
- [ ] one final render invariant verified by source test;
- [ ] regression matrix covers feature combinations.

---

# PHASE 7 — Persistent Render Job Engine — Issue #6

Only after the creative composition is stable, separate render lifetime from Activity/UI lifetime.

State model:

```text
QUEUED
 -> PREPARING
 -> RENDERING
 -> FINALIZING
 -> COMPLETED / FAILED / CANCELLED
```

Required:
- stable RenderJob ID;
- monotonic progress via Flow/state;
- safe cancellation;
- partial-output cleanup;
- Activity recreation survival;
- foreground media-processing service when required;
- notification progress/result surface;
- persistent job/error metadata;
- current CBR/geometry/codec/duration output validation retained.

---

# DEFERRED / BACKLOG

## Timed Video Overlay — Issue #3

The old Phase 6G.1 draft PR #16 was closed without merge. It was based on the pre-6F.2.8.1 hotfix baseline and did not pass the AndroidIDE unit-test gate.

Useful foundation is preserved at:

```text
6e097fc47d0c1098ea436d08e794765b540eb631
```

When this work resumes:
- reconstruct/rebase from current verified main;
- cherry-pick only isolated model/test pieces after review;
- do not restore the old stale PLAN state;
- preserve muted overlay audio for the first gate;
- preserve one final render architecture.

## FFmpegAndroid Reference Research — Issue #7

Keep as research/backlog. It must not replace the proven Media3 preview/export architecture wholesale.

---

## 4. Development / PR discipline

For each implementation gate:

1. branch from current verified `main`;
2. one scoped issue -> one scoped implementation PR;
3. update `PLAN.md` in the same PR;
4. add source verifier/regression tests where the one-render invariant can regress;
5. run unit tests + debug build before owner-device test;
6. do not mark device behavior PASS from static/source inspection alone;
7. owner-device preview/export evidence is required before merge for media-runtime changes;
8. after a stable media milestone, create/update a rollback branch/tag before beginning the next risky gate.

Recommended branch names:

```text
feature/phase-6h1-transitions
feature/phase-6h2-logo-motion
feature/phase-6g2-srt
feature/phase-6h3-narrator-audio
feature/phase-6h4-hook-preview
feature/phase-6g3-unified-edit-graph
feature/phase-7-render-jobs
```

---

## 5. Immediate next action

**Start Issue #20 — Phase 6H.1 Realtime Clip Transitions.**

Do not begin by adding many transition presets. First vertical slice:

```text
Transition model
 -> Crossfade only
 -> shared timeline projection
 -> realtime boundary preview
 -> one-pass final export
 -> tests/build/device PASS
```

After that single path is stable, add Fade/Slide/Zoom/Blur presets on the same contract.
