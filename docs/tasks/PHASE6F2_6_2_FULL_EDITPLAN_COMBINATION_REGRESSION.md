# Phase 6F.2.6.2 Task — Full EditPlan Combination Regression

Date: 2026-08-28

## Problem

The Phase 6F.2.6.1 hotfix line proved that exact portrait output, retained live preview, logo refresh,
aspect-ratio rebuilds, long-timeline overlays, and high-bitrate export can be corrected independently.
Before freezing a stable GitHub baseline, those features must also be proven together. The editor must
not regress to a workflow where enabling one tool forces an intermediate render or invalidates another
tool's state.

## Outcome

Keep one immutable `EditPlan` as the authoritative reviewed state across Clips, Transform, Audio,
Overlay and Export. Strengthen source-level regression coverage for the maximal combined plan and run
one owner-device matrix that ends in a single final `Transformer` export.

## Source work

- [x] Add `FullEditPlanCombinationRegressionTest` covering Adaptive Cuts + Transform + Audio Mix +
  Blur + Image Overlay + exact export preset in the same plan.
- [x] Assert Trim/reviewed range order, Freeze placement, planned duration, audio topology and
  replacement-audio topology together.
- [x] Assert Aspect/Crop/Mirror/Color/Zoom/Speed/Transition compilers remain active together.
- [x] Assert Blur and Logo absolute source windows project correctly into every adaptive clip.
- [x] Assert master switches omit operations without deleting remembered child settings.
- [x] Assert 720p/1080p/2K change final geometry/quality budget without changing planned duration.
- [x] Remove the accidental duplicate `input = input` named argument in `LocalRenderCoordinator`.
- [x] Add a retained source verifier and AndroidIDE/device verification contract.
- [x] Update `PLAN.md`, project identity and package version to `1.0-phase6f2.6.2`.
- [ ] Owner AndroidIDE unit/build verification.
- [ ] Owner device full-combination runtime/export verification.

## Acceptance criteria

1. The user can visit Clips → Transform → Audio → Overlay repeatedly and change enabled settings
   without generating an intermediate output file.
2. The same source preview remains usable, or falls back without deleting the reviewed EditPlan.
3. A maximal valid plan compiles all enabled operations at once and has no `EditPlanValidator` issue.
4. Adaptive Cuts remain in reviewed order and later clips retain correct Blur/Logo timing.
5. Intro Freeze, Speed, Transition and Audio Mix preserve the expected planned duration/audio policy.
6. 720p, 1080p and 2K are final-export choices only; switching the preset does not mutate the edit.
7. Final Export invokes one authoritative Media3 Composition and one `Transformer.start(...)`.
8. 1080p uses the Phase 6F.2.6.1D high-bitrate request policy and output validation reports the
   actual average bitrate rather than merely trusting the requested bitrate.

## Non-goals

- CompositionPlayer activation (Phase 6F.2.7)
- New video overlay or subtitle/text renderer
- Background render service
- External FFmpegAndroid reference integration
- GitHub repository migration before this device gate passes

## Next step after PASS

Freeze this exact source as the first verified GitHub baseline, update `PLAN.md` with retained device
evidence, push the new repository, tag the baseline, and create Issues for Phase 6F.2.7 / 6G / Phase 7.
