# Recap Flow AI Android — Active Implementation Plan

- **Project:** RecapFlowAI Android
- **Last updated:** 2026-09-01
- **Verified media rollback:** `stable/phase-6f2.8.1`
- **Active transition baseline:** `feature/phase-6h1-transitions`
- **Target-duration Clips merge:** `5cdf300e12e62bd1cdb32dc3cc4e90ec5270fd3f`
- **Primary development / Git environment:** **Termux**
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

# NEXT CORE WORKFLOW — Phase 6H.2 Animated Logo / Loop — Issue #21

**Status: IMPLEMENTATION STARTED on `feature/phase-6h2-animated-logo`.**

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

### Phase 6H.2 first implementation slice
- [x] first-class `ImageOverlayAnimationPreset` semantics in canonical `EditPlan`
- [x] loop/duration/period settings with static `NONE` default for backward compatibility
- [x] deterministic pure `ImageOverlayAnimationPolicy`
- [x] compiler-owned phase offset so a later reviewed clip resumes source-anchored loop phase instead of restarting
- [x] CompositionPlayer Speed projection scales window + animation duration + period + phase offset together
- [x] validation contract for animation duration/period
- [x] JVM tests added for non-loop, loop repetition, settled interval, clip-boundary phase continuity and Speed projection
- [ ] Termux unit gate for first slice
- [ ] Termux assemble gate for first slice

### Next implementation slices
1. Extend the shared `StaticImageOverlayEffect` shader path into a time-varying logo effect using the canonical phase policy.
2. Implement preset visual curves while keeping `NONE` pixel-identical to current static behavior.
3. Add realtime UI controls and English/Myanmar copy for preset, loop, duration and period.
4. Persist/restore user settings without altering static-overlay defaults.
5. Validate Target-duration Clips + Speed + Crossfade + animation in CompositionPlayer preview and one final Transformer export.
6. Owner-device 720p/1080p preview/export phase and geometry validation.

---

## Deferred after Phase 6H.2 foundation

- **6G.2 SRT/Text — Issue #4 + 6H.3 Narrator — Issue #22:** implement together against the authoritative Clips/timing model.
- **6H.4 Hook 0–3s Preview — Issue #23:** wait until opening composition includes stable Clips/SRT/Narrator timing.
- **Timed Video Overlay — Issue #3:** resume on the established source→presentation projection model.
- **6G.3 Unified Multi-Stage Edit Graph — Issue #5:** consolidation after animated overlay and text/narrator timing are proven.
- **Phase 7 Persistent Render Job Engine — Issue #6:** wrap the stable final graph, not a moving architecture.
- **FFmpegAndroid research — Issue #7:** low priority; no architecture replacement during core workflow work.

---

## Immediate next actions

1. Run the Phase 6H.2 first-slice Termux unit + assemble gates on `feature/phase-6h2-animated-logo`.
2. Keep static `NONE` behavior unchanged while wiring animation into the shared OpenGL overlay effect.
3. Add UI only after timing/phase semantics pass JVM and build gates.
4. Keep Crossfade PR #25 as a parallel validation-only gate; do not broaden transition scope.
