# Recap Flow AI Android — Implementation Plan

- **Project:** RecapFlowAI Android
- **Baseline:** `main` @ `1db468aae2fc2934f6b80bac1b6f0f36b2813565`
- **Last updated:** 2026-08-29
- **Primary target:** ARM64 Android phones/tablets; Android-hosted development remains supported
- **UI:** Native Kotlin + XML + ViewBinding
- **Media:** Media3 Composition/Transformer + JNI/FFmpeg where required
- **Core invariant:** editing remains non-destructive; one immutable `EditPlan` feeds one final `Transformer.start(...)`; no intermediate MP4 render while editing
- **Historical plan:** [`docs/archives/PLAN_2026-08-28_PHASE6F2_8.md`](docs/archives/PLAN_2026-08-28_PHASE6F2_8.md)

## Verified baseline

### Phase 6F.2.7 — CompositionPlayer preview
- [x] Owner-device PASS
- [x] Shared Composition preview architecture merged
- [x] Explicit ExoPlayer fallback preserved
- [x] One-final-render contract preserved

### Phase 6F.2.8 — Social export quality + source-aware FPS
- [x] Owner-device PASS reported 2026-08-29
- [x] PR #12 merged to `main` as `1db468aae2fc2934f6b80bac1b6f0f36b2813565`
- [x] Issue #11 closed completed
- [x] Stable rollback branch `stable/phase-6f2.8` created
- [x] 720p: 5 / 7.5 Mbps normal/high-FPS targets
- [x] 1080p: 8 / 12 Mbps normal/high-FPS targets
- [x] 1440p: 16 / 24 Mbps normal/high-FPS targets
- [x] H.264 MediaCodec request uses VBR
- [x] Final export preserves source FPS class when practical and caps at 60fps
- [x] Preview remains independently budgeted at 30fps
- [x] Finalized MP4 FPS/codec/duration validation retained
- [ ] Optional extended true-60fps device matrix when a representative source is available; non-blocking

## Repository housekeeping

### Issue #13 — stale Phase 6F.2.6 / CBR presentation copy
- [ ] Update legacy phase labels in product UI
- [ ] Replace stale CBR wording with VBR wording
- [ ] Update displayed preset bitrate descriptions to Phase 6F.2.8 policy
- [x] PLAN current-gate/status cleanup moved into the Phase 6G.1 foundation branch
- [ ] Merge presentation-only cleanup without touching verified render logic

Issue #13 is a presentation cleanup. It must not modify `EditPlan`, CompositionPlayer, Media3 composition compilation, encoder policy, or the one-final-Transformer export path.

## Current gate — Phase 6G.1 Timed Video Overlay (#3)

### Goal
Add one real muted picture-in-picture video overlay while preserving the verified shared-Composition architecture.

### Architecture contract
- Store one app-private video overlay asset.
- Store normalized center position, width fraction, opacity, and an absolute source-timeline window.
- Overlay audio is muted in Phase 6G.1; no implicit mix.
- Project the absolute overlay window through Trim and reviewed Adaptive Cuts.
- Apply Speed only when mapping surviving source intervals into presentation time.
- A short overlay ends naturally; it does not loop implicitly.
- A long overlay is clipped to the configured window.
- Preview and final export must use the same geometry/timing contract where the device path supports multi-input Composition.
- Unsupported multi-input preview must fail cleanly and preserve the current `EditPlan`.
- Final export remains one shared Composition -> one `Transformer.start(...)`.

### Phase 6G.1 implementation status

#### 6G.1A — model + timeline foundation
- [x] Add `VideoOverlayAsset` and `VideoOverlaySettings` as standalone foundation models.
- [x] Keep overlay audio out of the Phase 6G.1 model contract.
- [x] Add structural/project validation for geometry, asset metadata and source window.
- [x] Add `VideoOverlayTimelinePolicy` for Trim/Adaptive Cuts projection.
- [x] Preserve overlay-media source offsets across removed Adaptive Cut gaps.
- [x] Map surviving overlay segments into post-Speed presentation time.
- [x] Add unit coverage for cuts, 2x speed, short-overlay exhaustion, off-selection windows and model validation.
- [ ] AndroidIDE `:app:testDebugUnitTest` verification.

#### 6G.1B — EditPlan + import integration
- [ ] Add video-overlay state to `OverlaySettings` / immutable `EditPlan`.
- [ ] Add app-private video-overlay import coordinator using the system picker.
- [ ] Persist settings safely across editor tab changes without persisting source permissions/paths outside the existing project lifetime rules.
- [ ] Extend `EditPlanValidator` with video-overlay validation.

#### 6G.1C — shared Composition rendering
- [ ] Compile projected overlay segments into the shared Media3 Composition graph.
- [ ] Use multi-input video composition/PIP placement without starting an intermediate Transformer.
- [ ] Keep overlay audio muted.
- [ ] Define device/API fallback for unsupported multi-input composition.
- [ ] Verify exact one-final-Transformer start remains true.

#### 6G.1D — realtime editor controls
- [ ] Add Import/Replace/Remove video overlay controls.
- [ ] Add Move/Resize/Opacity controls.
- [ ] Add absolute Start/End timing controls.
- [ ] Keep preview geometry consistent with export geometry.
- [ ] Preserve source preview when multi-input preview is unavailable.

#### 6G.1 owner-device acceptance gate
- [ ] Import one overlay video successfully.
- [ ] Position/scale/opacity/timing survive tab changes.
- [ ] Overlay audio is muted.
- [ ] Preview and export geometry match.
- [ ] Short overlay ends cleanly; long overlay clips to configured window.
- [ ] Adaptive Cuts + Speed preserve overlay timing.
- [ ] Unsupported compositor/preview path fails cleanly without damaging source/EditPlan.
- [ ] 720p/1080p final H.264/AAC export remains one pass with no intermediate MP4 render.
- [ ] Owner reports Phase 6G.1 PASS before merge to stable baseline.

## Next planned gates

### Phase 6G.2 — Subtitle / text rendering pipeline (#4)
Starts only after Phase 6G.1 owner-device PASS.

### Phase 6G.3 — Unified multi-stage edit graph (#5)
Starts only after 6G.1/6G.2 contracts are stable.

## Build baseline
- Application ID: `com.recapflow.ai`
- minSdk: 28
- targetSdk: 34
- compileSdk: 36
- ABI: `arm64-v8a`
- Java/JVM: 17
- AndroidX Media3: 1.10.0
- NDK: 24.0.8215888
- CMake: 3.18.1
- FFmpeg: 9.0.1 static ARM64 integration

## Release / safety invariants
- Do not introduce VPS dependency for core video processing.
- Do not add hidden intermediate renders to make preview work.
- Do not silently mix overlay audio.
- Do not promote normal-FPS sources to synthetic 60fps.
- Do not regress H.264/AAC geometry, duration tolerance or Gallery export.
- Do not clear or mutate the reviewed edit when preview/compositor capability fails.
