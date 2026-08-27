# Phase 6F.2.7 — CompositionPlayer Live Preview With Explicit Fallback

## Goal

Connect the already-authoritative `EditPlan -> Media3CompositionPlan` topology to Media3
`CompositionPlayer` for realtime editor preview without changing the proven one-pass Transformer
export path.

## Source contract

1. `BuildConfig.ENABLE_COMPOSITION_PLAYER_PREVIEW` is controlled by the Gradle property
   `recapflow.composition.preview.enabled` and defaults to `true` for the Phase 6F.2.7 device gate.
2. Source preview uses `Media3CompositionCompiler.compileForPreview`, which reuses the same
   `Media3CompositionPlan` decisions as final export.
3. CompositionPlayer requires explicit `EditedMediaItem` durations. Preview therefore supplies the
   original encoded source duration before clipping. The final Transformer compiler path is left
   unchanged to protect the owner-verified Phase 6F.2.6.2 export baseline.
4. Editor source positions are mapped to/from CompositionPlayer's output timeline so a preview graph
   rebuild preserves the user's semantic playhead across Trim, Adaptive Cuts, and Speed.
5. A CompositionPlayer setup/playback/readiness failure blocks CompositionPlayer only for the current
   source session and first restores the existing ExoPlayer live-effects path. The existing
   source-only preview remains the second fallback if ExoPlayer also fails.
6. Intro Freeze remains on the proven ExoPlayer simulation in this gate because CompositionPlayer
   would require a temporary freeze-frame asset. This is an explicit capability fallback, not a
   dropped EditPlan operation; final export still renders the freeze.
7. Adaptive candidate/sequence inspection explicitly uses ExoPlayer because those controls operate
   on source-time clip previews. Returning to normal source preview can re-enter CompositionPlayer.
8. Composition preview never starts Transformer and never creates an intermediate MP4.
9. Final export remains exactly one `Transformer.start(compiledComposition.composition, ...)` call.

## Expected live-edit behavior

- Trim / applied Adaptive Cuts: rebuild shared preview Composition without rendering.
- Transform / aspect / crop / mirror / color / zoom / speed: rebuild shared preview Composition;
  geometry-changing updates wait for the movable preview surface to consume its new bounds first.
- Audio Keep/Mute/Replace/Mix: CompositionPlayer consumes the compiled audio topology. The separate
  ExoPlayer replacement-audio simulator is disabled while CompositionPlayer owns preview audio.
- Blur / image logo: immutable Composition effects are rebuilt from the current OverlaySettings.
  Slider traffic remains debounced.
- Export quality selection does not force a preview render. Preview uses the 720p geometry budget for
  editor efficiency while retaining the selected aspect/timing operations.

## Non-goals

- No multi-video overlay / picture-in-picture sequence in this gate.
- No CompositionPlayer freeze-frame asset lifecycle yet.
- No removal of ExoPlayer fallback.
- No change to FFmpeg 9.0.1 build or native render runtime.
- No change to final H.264/AAC bitrate/validation policy.
