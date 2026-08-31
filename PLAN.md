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
- [x] PR #27 merged into `feature/phase-6h1-transitions` as merge commit `3537b7451644177486e27344959c2213896c4f3c`.

Localization is no longer a separate blocker. New UI work must add both English and Myanmar copy from the start.

---

# Phase 6H.1 — Realtime Clip Transitions — Issue #20 / PR #25

**Status: SOURCE + TERMUX GATES PASS; OWNER-DEVICE CROSSFADE RUNTIME NOT YET ACCEPTED.**

Current PR scope is **Crossfade only**.

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

### Remaining gate
- [ ] realtime owner-device Crossfade preview on the actual composition path
- [ ] explicit safe behavior if preview path is unsupported
- [ ] 720p Crossfade export PASS
- [ ] 1080p Crossfade export PASS
- [ ] A/V quality/sync PASS
- [ ] one-final-Transformer invariant preserved

Do **not** add fade-through-black, slide, zoom, or blur-dissolve to PR #25. Additional transition families are deferred until Target-duration Clips (#30) is stable.

---

# Phase 6H.1E — Deterministic Per-Clip Random Mirror — Issue #28 / PR #29

**Status: IMPLEMENTED; TERMUX BUILD/TEST PASS REPORTED; OWNER-DEVICE VALIDATION PENDING.**

### Implemented
- [x] global Mirror retained
- [x] separate Random mirror each clip mode
- [x] deterministic clip-identity decision; no runtime `Random`
- [x] global/random mutual exclusion
- [x] single-clip no-op
- [x] Crossfade source-index preservation
- [x] Intro Freeze matches first moving clip orientation
- [x] preference/state persistence
- [x] ViewBinding parameter overflow fixed by extracting Mirror controls to a child layout
- [x] `testDebugUnitTest` PASS (owner report)
- [x] `assembleDebug` PASS (owner report)
- [x] `git diff --check` PASS

### Remaining gate
PR #27 localization is now in PR #29's base branch. Refresh PR #29 from the updated base before final device acceptance.

- [ ] 3+ clips show mixed stable mirror choices
- [ ] preview rebuild preserves the same pattern
- [ ] global/random mutual exclusion on device
- [ ] single clip stays normal
- [ ] Intro Freeze parity
- [ ] Crossfade + Random Mirror A/V/identity sanity
- [ ] 720p/1080p preview/export pattern parity
- [ ] Random Mirror English/Myanmar copy verified together

Keep PR #29 draft until these pass. Do not expand Random Mirror scope.

---

# NEXT CORE WORKFLOW — Target-Duration Clips — Issue #30

**Priority: IMMEDIATE AFTER CURRENT VALIDATION CLEANUP.**

The product value is not manual head/tail trimming. Replace the normal Trim-first Clips UX with an authoritative target-duration workflow.

### Required contract
- remove standalone head/tail Trim from the normal user-facing Clips workflow
- internal `TrimRange` may remain as a full-source/timeline boundary only
- user chooses final desired duration, e.g. `03:00 → 01:00` or `03:00 → 02:00`
- deterministic planner distributes kept ranges across the source in chronological order
- do not truncate the first N seconds or simply delete the tail
- generated ranges remain reviewable
- Clips works with Transform completely OFF
- Speed remains optional but composes with Clips instead of conflicting with it
- user target represents **final planned output duration**, not raw selected clip sum
- duration reconciliation accounts for Speed, Crossfade overlap and Intro Freeze
- later SRT/Narrator timing must consume the same duration authority
- preview/export share the same resolved ranges/timing
- source is never overwritten
- exactly one final Transformer export

### First acceptance scenarios
- [ ] `03:00 → 01:00` distributed source plan
- [ ] `03:00 → 02:00` distributed source plan
- [ ] Clips-only with all Transform features OFF
- [ ] Clips + Speed ON/OFF re-reconciles to the same requested target
- [ ] Crossfade/Freeze included in final-duration tolerance
- [ ] source-order/story sanity
- [ ] preview/export duration parity and A/V sync
- [ ] English/Myanmar UI with Arabic digits `0-9`

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

1. Keep PR #25 draft; capture/resolve the owner-device realtime Crossfade fallback path and finish 720p/1080p + A/V validation.
2. Refresh PR #29 from the now-localized `feature/phase-6h1-transitions` base and run its remaining owner-device Random Mirror checks; do not expand its feature scope.
3. After those validation-only gates, start Issue #30 as the next implementation phase: **User Target Duration → distributed reviewed Clips → Speed/Crossfade/Freeze reconciliation → shared preview → one final export**.
4. Do not start #21/#4/#22/#23/#3/#5/#6/#7 implementation PRs before #30 establishes the authoritative duration model.
