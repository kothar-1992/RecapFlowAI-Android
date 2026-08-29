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

The project originally used AndroidIDE as the primary Android-hosted development environment. The active workflow has now moved to **Termux** because branch inspection, Git operations, PR review, source verification, unit tests and Gradle builds are faster and easier to repeat there.

### Standard development loop

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

### Responsibilities
- **Termux:** primary Git branch management, diffs, rebases/cherry-picks, PR preparation, shell verification scripts, Gradle unit tests and debug builds.
- **Physical Android device:** installation, UI interaction, realtime preview, encoder behavior, visual quality, A/V sync and 720p/1080p media acceptance.
- **AndroidIDE:** optional fallback only when an IDE-specific inspection or debugging workflow is useful.

Do not describe an ordinary future verification gate as an “AndroidIDE build gate” unless AndroidIDE itself is specifically required. The normal wording is now **Termux build/test PASS → owner-device runtime PASS**.

---

## 3. Product objective for the next implementation track

The local editor now exports usable final-quality video. The next goal is to remove creative-workflow round trips and make editing decisions previewable before export.

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

Recent publishing evidence shows the strongest audience loss around the opening 0:02 region, so rapid first-seconds preview remains a product requirement.

---

## 4. Architecture contract for all next phases

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

**Status: IN PROGRESS — 6H.1A/B verified in Termux; 6H.1C capability guard verified; deterministic two-lane Crossfade topology authored and awaiting the next Termux gate**

### Goal
Add clip-boundary transitions that can be tested in realtime before export.

### Initial transition set
The first vertical slice implements **Crossfade only**. Additional presets remain blocked until the crossfade contract proves stable.

Later presets:
- Fade through black
- Slide left/right
- Zoom
- Blur dissolve

### 6H.1A — semantic model + timeline projection
- [x] Add backend-independent `ClipTransitionSettings` / `ClipTransitionBoundary` model.
- [x] Identify a transition by adjacent surviving source-range boundary rather than backend filter syntax.
- [x] Add `CROSSFADE` as the only first-slice transition type.
- [x] Add Linear / Ease-in-out semantic easing values.
- [x] Enforce 150–1000 ms user-facing transition duration policy.
- [x] Project transition timing after Trim/reviewed Adaptive Cuts and Speed.
- [x] Keep transition duration in presentation time while scaling required source overlap under Speed.
- [x] Accumulate overlap deterministically across multiple boundaries.
- [x] Add unit coverage for disabled no-op, adjacent boundary projection, Speed, multiple boundaries, duplicate/missing/too-long boundaries.
- [x] Termux `:app:testDebugUnitTest` PASS.
- [x] Termux FFmpeg-enabled `:app:assembleDebug` PASS.

### 6H.1B — EditPlan + validation integration
- [x] Attach clip-transition settings to the immutable reviewed `EditPlan`.
- [x] Subtract compiled crossfade overlap from planned output duration.
- [x] Surface transition validation through `EditPlanValidator`.
- [x] Preserve the existing Transform fade operation as a separate legacy/global visual effect until intentionally migrated.
- [x] Termux unit/build gate PASS in 2m 55s; 28 actionable tasks executed.

### 6H.1C — shared runtime topology and capability-safe execution
- [x] Carry compiled clip-boundary transitions into `Media3CompositionPlan`.
- [x] Add an explicit runtime capability guard so an enabled Crossfade cannot silently degrade to a hard cut.
- [x] Capability-guard Termux unit/build gate PASS in 2m 53s; 28 actionable tasks executed.
- [x] Define a deterministic two-lane overlap schedule for future Media3 sequence/compositor execution.
- [x] Define one shared easing function for visual/audio Crossfade envelopes.
- [x] Reject adjacent Crossfades that overlap inside the same middle clip; a two-lane runtime must never create triple-overlap ambiguity.
- [x] Add unit coverage for lane alternation, Freeze/Speed offsets, easing alpha and overlapping-boundary rejection.
- [ ] Termux unit/build gate PASS for the new two-lane topology commits.
- [ ] Wire the verified topology to a reviewed Media3/custom compositor execution path for both preview and export.
- [ ] Define and verify matching audio Crossfade envelopes; do not accept a video-only blend with doubled audio during overlap.
- [ ] Keep exactly one final `Transformer.start(...)`.
- [ ] No temporary crossfade MP4 or per-feature render.

### Media3 limitation decision
The project remains pinned to Media3 1.10.0. Multiple `EditedMediaItemSequence` tracks can overlap and a custom `VideoCompositorSettings` can vary overlay alpha by presentation time, but Android's current Composition documentation still lists direct video/audio crossfading as unsupported. Therefore the branch treats the two-lane/compositor route as a capability-gated implementation spike, not as a proven runtime contract. Until owner-device preview/export validates a shared path, reviewed Crossfade state must fail explicitly rather than silently render as a hard cut.

### 6H.1D — realtime boundary controls
- [ ] Select a clip boundary.
- [ ] Crossfade ON/OFF.
- [ ] Duration 150–1000 ms.
- [ ] Easing selection.
- [ ] One-tap boundary preview around the transition.
- [ ] Reset to None / hard cut.

### Phase 6H.1 exit gate
- [ ] Termux unit tests + FFmpeg-enabled debug build PASS for final runtime implementation.
- [ ] owner-device realtime boundary preview PASS.
- [ ] 720p + 1080p crossfade export PASS.
- [ ] combined current features + crossfade still show exactly one final Transformer start.
- [ ] merge scoped PR and mark #20 completed.

Implementation branch: `feature/phase-6h1-transitions` from current verified `main`, not the old Phase 6G.1 draft branch.

---

## Phase 6H.2 — Animated Logo Overlay + Loop — Issue #21

Starts only after Phase 6H.1 semantics are stable.

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

### Rules
- no temporary animated-logo video;
- animation phase is deterministic from timeline time;
- loop is bounded by configured overlay start/end;
- aspect conversion may not move the overlay outside the output frame;
- preview/export must share animation phase/easing;
- transition + animated logo must still finish in one final render.

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
SRT and narrator must use the same authoritative source/presentation mapping used by the reviewed `EditPlan`. Do not create a second independent timeline model.

---

## Phase 6H.4 — Hook 0–3 Second Preview — Issue #23

Required behavior:
- one action enters `Preview Hook 0–3s`;
- loop the hook window;
- immediately reflect first clip, transition, logo, subtitles and narrator changes;
- quick seek to zero;
- restore normal preview position/play intent predictably;
- use current `EditPlan`, not a separate project state;
- create no intermediate media file.

---

# CONSOLIDATION

## Phase 6G.3 — Unified Multi-Stage Edit Graph — Issue #5

Target semantic order:

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

The implementation may fuse GPU stages, but semantic ordering must remain deterministic and tested.

---

# PHASE 7 — Persistent Render Job Engine — Issue #6

After creative composition is stable, separate render lifetime from Activity/UI lifetime.

```text
QUEUED
 -> PREPARING
 -> RENDERING
 -> FINALIZING
 -> COMPLETED / FAILED / CANCELLED
```

Required later: stable RenderJob ID, monotonic progress, safe cancellation, partial-output cleanup, Activity recreation survival, foreground media-processing service where required, notifications and persisted job/error metadata.

---

# DEFERRED / BACKLOG

## Timed Video Overlay — Issue #3

Old draft PR #16 remains closed without merge. Useful foundation is preserved at:

```text
6e097fc47d0c1098ea436d08e794765b540eb631
```

When resumed, reconstruct from current verified main and selectively reuse only reviewed isolated pieces.

## FFmpegAndroid Reference Research — Issue #7

Research/backlog only. It must not replace the proven Media3 preview/export architecture wholesale.

---

## 5. Development / PR discipline

For each implementation gate:

1. branch from current verified `main`;
2. one scoped issue -> one scoped implementation PR;
3. update `PLAN.md` in the same PR;
4. add source verifier/regression tests where the one-render invariant can regress;
5. **run verification in Termux first**;
6. normal source gate: `:app:testDebugUnitTest` + FFmpeg-enabled `:app:assembleDebug`;
7. do not mark device behavior PASS from static/source/build inspection alone;
8. owner-device preview/export evidence is required before merge for media-runtime changes;
9. use AndroidIDE only as an optional fallback, not the default build/branch gate;
10. after a stable media milestone, create/update a rollback branch/tag before the next risky gate.

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

## 6. Immediate next action

Verify the new 6H.1C two-lane topology and adjacent-overlap validation in **Termux**:

```bash
cd /storage/emulated/0/AndroidIDEProjects/RecapFlowAI-Android/
git fetch origin
git switch feature/phase-6h1-transitions
git pull --ff-only origin feature/phase-6h1-transitions

AAPT2_BIN="$PREFIX/bin/aapt2"
GRADLE="$HOME/.local/opt/gradle-9.0.0/bin/gradle"

"$GRADLE" :app:testDebugUnitTest \
  -Precapflow.ffmpeg.enabled=true \
  -Pandroid.aapt2FromMavenOverride="$AAPT2_BIN" \
  --no-daemon --max-workers=2 --rerun-tasks --stacktrace

"$GRADLE" :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true \
  -Pandroid.aapt2FromMavenOverride="$AAPT2_BIN" \
  --no-daemon --max-workers=2 --rerun-tasks --stacktrace
```

If both pass, continue to the capability-gated Media3/custom compositor spike. Do not add more transition presets before Crossfade passes shared preview/export and owner-device validation.
