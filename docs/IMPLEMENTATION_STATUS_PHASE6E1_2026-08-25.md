# Phase 6E.1 Implementation Status — 2026-08-25

## Phase 6E.1.5 Fit preview aspect parity — 2026-08-26

- Owner testing found that 9:16 → 16:9 Fit resized the floating card correctly but stretched
  portrait pixels horizontally instead of showing the whole frame with side bars.
- The shared Media3 `Presentation.LAYOUT_SCALE_TO_FIT` preview/export operation is retained;
  it remains responsible for pillarbox/letterbox composition.
- A typed `PreviewAspectPolicy` now assigns aspect ownership to PlayerView for ordinary or
  fallback source playback and to Media3 video effects for Presentation/custom Crop output.
- PlayerView uses Fill only when placing an already-composed effect frame in the matching
  preview card. This removes the second source-aspect correction; it does not select Media3's
  Stretch layout and does not alter source pixel proportions.
- Policy tests cover ordinary source, portrait-to-landscape Fit, custom Crop, and fallback.
- Project identity is now `RecapFlowAI_Phase6E1_5` / `1.0-phase6e1.5`.

## Phase 6E.1.4 touch rollback + preview bounds recovery — 2026-08-26

- Owner testing confirms Phase 6E.1.3 still terminates after releasing a direct blur
  drag, while Horizontal/Vertical/Width/Height slider editing remains stable.
- Direct guide drag and corner resize are therefore gated off by the explicit
  `SOURCE_BLUR_DIRECT_TOUCH_ENABLED = false` safety flag; the dormant implementation is
  retained for later crash-trace work, not registered as an active touch target.
- The blur guide remains visible as a non-interactive realtime outline, its resize handle is
  hidden, and the UI directs the user to the working position and size sliders.
- The movable/resizable PlayerView now requests `texture_view`, and the preview card hierarchy
  clips children so dynamic 9:16/16:9 Presentation output stays inside the card.
- Aspect/crop-driven geometry changes request a bounded PlayerView/TextureView relayout on the
  next animation frame.
- Project identity is now `RecapFlowAI_Phase6E1_4` / `1.0-phase6e1.4`.

## Phase 6E.1.3 release-commit deferral hotfix — 2026-08-26

- Owner testing confirms the Phase 6E.1.2 outline now moves under direct touch without
  terminating the app; termination remains specifically when the finger is released.
- `ACTION_UP`/`ACTION_CANCEL` now performs no geometry layout, slider synchronization,
  typed rectangle mutation, immediate effect update, or `performClick()` side effect.
- Release only schedules a guarded `postOnAnimation` commit so Android completes the
  pointer dispatch before the guide layout and slider values are synchronized.
- The actual Media3 effect update uses the existing 140 ms coalesced preview queue after
  the deferred geometry commit; it is not rebuilt in either the input callback or layout frame.
- The visually translated/scaled guide remains at the released location until the deferred
  commit restores identity transforms and installs the normalized layout.
- New gestures, source replacement, player error, stop, and destruction cancel the pending
  view callback and restore the guide safely.
- Project identity is now `RecapFlowAI_Phase6E1_3` / `1.0-phase6e1.3`.

## Phase 6E.1.2 direct-touch isolation hotfix

- Owner testing proved that the geometry/strength sliders are stable and that only direct
  touch movement or resize of the preview guide terminates the app.
- The active pointer stream no longer changes the touched view's layout parameters, writes
  slider values, mutates typed blur settings, or asks Media3 to rebuild an effect graph.
- Drag uses compositor translation and resize uses top-left compositor scale so the marked
  guide remains responsive without requesting layout while it owns the touch target.
- One normalized rectangle is validated and committed after release; only then are the
  sliders/layout synchronized and one immediate preview effect update requested.
- Drag/resize pending values are isolated from the committed `OverlaySettings` and export plan.
- Runtime gesture failures restore identity transforms, release parent interception, and emit
  `RecapFlowBlur` error records for Android Code Studio's LogWire App Logs.
- Slider behavior, localized shader pixels, source-time mapping, and 720p/1080p export remain
  unchanged.
- Project identity is now `RecapFlowAI_Phase6E1_2` / `1.0-phase6e1.2`.

## Phase 6E.1.1 drag-stability hotfix

- Owner device testing found that moving the blur guide could terminate the app before a
  useful crash block was captured.
- Direct rectangle movement now updates only the guide and geometry controls on each touch
  frame; Media3 effect graphs are coalesced to one update per 140 ms.
- Drag and resize release flushes the latest rectangle immediately.
- Guide layout is clamped and its FrameLayout parameters are checked safely.
- Synchronous effect failures restore the existing source-only preview fallback.
- Asynchronous Player errors and bounded graph applications emit `RecapFlowPreview` logs;
  layout failures emit `RecapFlowBlur` for Android Code Studio's LogWire App Logs.
- Pending callbacks are cancelled on source replacement, Player failure, stop, and destroy.
- Project identity is now `RecapFlowAI_Phase6E1_1` / `1.0-phase6e1.1`.

## Implemented in source

- Overlay is the fourth Review Editor tab and remains inside Editor navigation.
- Overlay master and Source subtitle blur item switches preserve remembered Off/On state.
- One typed normalized blur rectangle, source time range, and 4–32 strength are compiled.
- The user can drag/resize the preview guide or use precise sliders and Reset.
- A custom localized Media3 OpenGL shader blurs only the selected rectangle with feathered edges.
- ExoPlayer preview and Transformer export share the same effect construction and order.
- Absolute source-time mapping covers Trim, Adaptive candidate/sequence, concatenated ranges,
  and the Intro Freeze source frame.
- Transform, Audio, lifecycle restoration, render locking, stale-output invalidation,
  cancellation, and local-only processing are retained.
- Compiler/validator tests and source-preflight markers cover disabled-state omission and
  valid/invalid geometry, strength, and time.
- The Media3 1.8-compatible shader loader uses packaged `assets/shaders/*.glsl` paths;
  it does not pass unsupported raw resource IDs to `GlProgram`.

## Verification completed in this workspace

- XML parsing, duplicate-ID checks, resource-reference checks, ViewBinding ID checks,
  Kotlin delimiter scan, GLSL markers, shell syntax, identity markers, and secret scan: PASS via
  `bash scripts/verify_phase6e1_source.sh`.
- GitHub task created: [`ZeusOwner/recapflow-ai#21`](https://github.com/ZeusOwner/recapflow-ai/issues/21).
- Owner AndroidIDE evidence found the first actionable compile error at
  `SourceSubtitleBlurEffect.kt:56-57`; the unsupported `R.raw` arguments are corrected
  to packaged shader asset paths and guarded by source preflight.
- Gradle compilation/testing/assembly: NOT RUN. The wrapper attempted to download Gradle 9.0.0,
  but this workspace has no complete cached distribution and outbound access to the distribution
  endpoint is unavailable. This is an environment blocker, not build evidence.

## Required next evidence

Run `PHASE6E1_5_FIT_PREVIEW_ANDROIDIDE_VERIFICATION.md` on AndroidIDE and the target
device. Static checks cannot prove the device GPU shader, paused-frame refresh, exact preview/
export geometry, hardware encoder compatibility, audio sync, or final playback quality.
