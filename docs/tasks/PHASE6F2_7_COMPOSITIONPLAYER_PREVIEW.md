# Phase 6F.2.7 — CompositionPlayer Live Preview With Explicit Fallback

## Goal

Use the already-authoritative `EditPlan -> Media3CompositionPlan` topology for realtime preview while
preserving the proven one-pass Transformer final export and ExoPlayer recovery path.

## Implementation contract

- `BuildConfig.ENABLE_COMPOSITION_PLAYER_PREVIEW` is controlled by
  `-Precapflow.composition.preview.enabled=true|false` and defaults to `true` for this device gate.
- `Media3CompositionCompiler.compileForPreview(...)` supplies CompositionPlayer's required original
  pre-clipping encoded duration for every clipped source item. Transformer export keeps the verified
  duration behavior and does not receive the clipped range as `durationUs`.
- CompositionPlayer uses the same selected Trim/Adaptive ranges, audio topology, transforms,
  blur/logo settings and speed decisions as final composition planning.
- Source positions are mapped to/from the concatenated output timeline so graph rebuilds preserve the
  semantic playhead through cuts and constant speed changes.
- Media3 experimental paired speed effects are used and the video speed effect is placed first, as
  required by CompositionPlayer 1.10.0.
- Timed blur/logo windows are converted from range-local source time to post-speed presentation time.
- Geometry changes wait for the existing preview-surface settle boundary before composition rebuild.
- Intro Freeze remains an ExoPlayer simulation in this gate because CompositionPlayer would require
  a temporary freeze-frame asset lifecycle.
- Adaptive candidate and draft-sequence inspection remain ExoPlayer source-time tools. Returning to
  normal source preview can re-enter CompositionPlayer.
- A CompositionPlayer setup/playback/readiness failure blocks CompositionPlayer only for the current
  source session and restores ExoPlayer live effects. Exo failure can then enter source-only mode.
- Preview code never starts Transformer and never creates an intermediate MP4.

## Merge gate

Do not merge PR #10 until `docs/PHASE6F2_7_COMPOSITIONPLAYER_ANDROIDIDE_VERIFICATION.md` passes on the
owner device and the result is recorded in `PLAN.md`.
