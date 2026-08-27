# Phase 6E.3B — Realtime preview session stability

## Goal

Keep one source-preview player alive while the user changes reviewed settings, show live
blur/logo values without recreating the GL graph, and leave an indefinite loading state through a
bounded recovery path. Encoded output remains compiled only from the immutable EditPlan.

## Included

- `RealtimePreviewSession` owns source generation, applied graph identity, latest pending request,
  and the one permitted fallback claim.
- Transform and Trim graph requests are coalesced for 140 ms; the latest request wins.
- Source blur and logo geometry/opacity/time changes use thread-safe preview-only state read once
  per frame by retained custom shaders.
- Paused preview requests a same-position redraw after a state-only change.
- Normal controls call `setVideoEffects`; they do not replace the source `MediaItem` or call
  `prepare()`.
- Player buffering gets a 10-second deadline. The first source failure retries at the last valid
  position without live effects; another failure stops loading and exposes Preview unavailable.
- `PlayerView` retains the last rendered frame while Media3 replaces a graph/session.
- Adaptive candidate/sequence and replacement/mix audio continue to share the source-preview
  lifecycle.
- Unit and source-preflight coverage is added.

## Excluded

- Any change to `TransformVideoEffects.forRender` or output semantics.
- Public Gallery/MediaStore output (Phase 6F.1).
- Saved editor presets (Phase 6F.2).
- Direct source-blur/logo screen-touch editing.
- Gemini, Telegram, image animation loops, and video overlay assets.

## Acceptance

- `bash scripts/verify_phase6e3b_source.sh` passes.
- `:app:testDebugUnitTest` and `:app:assembleDebug` pass in AndroidIDE.
- Rapid consecutive controls do not reload the source, remain responsive, and show the latest
  paused/playing frame.
- A forced player failure performs no more than one fallback and never spins indefinitely.
- H.264/HEVC preview and 720p/1080p export parity pass on device.
