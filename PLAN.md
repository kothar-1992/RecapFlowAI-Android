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
- [x] Shared preview/export architecture retained.
- [x] Exactly one final `Transformer.start(...)` remains the render invariant.
- [x] `stable/phase-6f2.8.1` remains the rollback checkpoint.

Do not regress this baseline while changing Clips/timeline semantics.

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
- [x] accepted Random Mirror feature is now also integrated into this active stack

### Remaining Crossfade gate
- [ ] realtime owner-device Crossfade preview on the actual composition path
- [ ] explicit safe behavior if preview path is unsupported
- [ ] 720p Crossfade export PASS
- [ ] 1080p Crossfade export PASS
- [ ] A/V quality/sync PASS
- [ ] one-final-Transformer invariant preserved

Do **not** add fade-through-black, slide, zoom, or blur-dissolve to PR #25. Additional transition families stay deferred until Target-duration Clips (#30) is stable.

---

# Phase 6H.1E — Deterministic Per-Clip Random Mirror — Issue #28 / PR #29

**Status: COMPLETE AND INTEGRATED.**

Final validated/integrated head: `a7cc98cc1507ce2d2c3e8e7e4ea9ca2421fcddde`.

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
- [x] Myanmar localization verifier PASS: **484 strings covered**
- [x] Arabic digits `0-9` policy PASS
- [x] English/Myanmar Random Mirror copy present
- [x] owner-device verification PASS (owner report)
- [x] PR #29 recorded by GitHub as merged into `feature/phase-6h1-transitions`
- [x] Issue #28 closed completed

No further Random Mirror scope expansion before Target-duration Clips.

---

# NEXT CORE WORKFLOW — Target-Duration Clips — Issue #30

**Priority: START NEXT.**

The product value is not manual head/tail trimming. Replace the normal Trim-first Clips UX with an authoritative target-duration workflow.

### Product contract
- remove standalone head/tail Trim from the normal user-facing Clips workflow
- internal `TrimRange` may remain only as a full-source/timeline boundary if required by the IR
- user chooses final desired duration, e.g. `03:00 → 01:00` or `03:00 → 02:00`
- deterministic planner distributes kept ranges across the whole source in chronological order
- do not truncate the first N seconds or simply delete the tail
- generated ranges remain reviewable before export
- Clips works with Transform completely OFF
- Speed remains optional but composes with Clips instead of conflicting with it
- user target represents **final planned output duration**, not raw selected clip sum
- duration reconciliation accounts for Speed, Crossfade overlap and Intro Freeze
- later SRT/Narrator timing must consume the same duration authority
- preview/export share the same resolved ranges/timing
- source is never overwritten
- exactly one final Transformer export

### First implementation slice
1. Add a first-class target-duration field/mode to the canonical Clips model.
2. Port the deterministic target-duration planning principle from `ZeusOwner/recapflow-ai` into Android without server/AI dependency.
3. Generate source-distributed ordered ranges from source duration + user target.
4. Reconcile required kept-source duration against active Speed/Crossfade/Freeze semantics so `EditPlan.plannedDurationMs` stays near the requested target.
5. Replace the normal Trim-first UI with source duration + desired `mm:ss` + compression + estimated final duration + Generate/Review flow.
6. Provide English/Myanmar copy from the same PR; keep numeric values in Arabic digits `0-9`.

### First acceptance scenarios
- [ ] `03:00 → 01:00` distributed source plan
- [ ] `03:00 → 02:00` distributed source plan
- [ ] Clips-only with all Transform features OFF
- [ ] Clips + Speed ON/OFF re-reconciles to the same requested target
- [ ] Crossfade/Freeze included in final-duration tolerance
- [ ] source-order/story sanity
- [ ] deterministic repeatability
- [ ] preview/export duration parity and A/V sync
- [ ] English/Myanmar UI with Arabic digits `0-9`
- [ ] Termux unit + assemble PASS
- [ ] owner-device validation PASS

---

## Deferred until #30 is stable

- **6H.2 Animated Logo / Loop — Issue #21:** timing windows must bind to the new target-duration timeline.
- **6G.2 SRT/Text — Issue #4 + 6H.3 Narrator — Issue #22:** implement together against one authoritative Clips/timing model.
- **6H.4 Hook 0–3s Preview — Issue #23:** wait until opening composition includes stable Clips/SRT/Narrator timing.
- **Timed Video Overlay — Issue #3:** resume only on the new source→presentation projection model.
- **6G.3 Unified Multi-Stage Edit Graph — Issue #5:** consolidation after target-duration Clips and text/narrator timing are proven.
- **Phase 7 Persistent Render Job Engine — Issue #6:** wrap the stable final graph, not a moving architecture.
- **FFmpegAndroid research — Issue #7:** low priority; no architecture replacement during core workflow work.

---

## Immediate next actions

1. Start Issue #30 implementation from the consolidated `feature/phase-6h1-transitions` stack at/after `a7cc98cc1507ce2d2c3e8e7e4ea9ca2421fcddde`.
2. Treat Crossfade PR #25 as a parallel **validation-only** gate; do not expand transition scope while #30 changes the authoritative duration model.
3. Build the target-duration planner first, then replace the user-facing Trim-first Clips controls.
4. Prove `03:00 → 01:00`, `03:00 → 02:00`, Clips-only, and Clips+Speed before starting #21/#4/#22/#23/#3/#5/#6/#7 implementation work.
