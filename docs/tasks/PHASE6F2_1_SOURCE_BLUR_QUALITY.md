# Phase 6F.2.1 — Source-subtitle blur quality hotfix

## Goal

Make a burned-in source subtitle unreadable without drawing repeated, mirrored, or stale copies
inside the selected blur rectangle.

## Confirmed cause

The previous fragment shader used only 13 samples. At high strength it sampled at full-radius and
double-radius offsets, so each sample remained recognizable as a separate displaced subtitle.
That sparse kernel explains the owner screenshot; it was not equivalent to the GitHub server's
continuous multi-pass box blur.

## Included

- A bounded `strength / 2` radius policy matching the server-side blur control contract.
- A dense normalized 9×9 GPU kernel shared by realtime Media3 preview and Transformer export.
- Rectangle-clamped sampling so texture edge modes cannot tile or mirror subtitle pixels.
- Region-size capping, 720-short-side scaling, and independent horizontal/vertical feather widths.
- Pure policy tests and a source preflight that rejects the old sparse shader.
- Phase 6F.2 preference, Transform, Audio, Overlay, Export, bitrate, and cancellation preservation.

## Non-goals

- Automatic subtitle detection, OCR, tracking, keyframes, or additional blur regions.
- Direct blur drag/resize touch re-enablement.
- Pixel-identical FFmpeg and GPU kernels; the typed radius/geometry/time behavior is aligned.
- Changes to logo compositing, output dimensions, audio, encoding bitrate, or public publication.

## Acceptance

- Strength 30 no longer creates readable lines at vertical/horizontal offsets.
- Strength 30 makes ordinary burned-in subtitle text unreadable in preview and export.
- Pixels outside the selected rectangle remain unchanged and the feathered border has no hard seam.
- Scrubbing, pause/play, On/Off, time-range changes, and repeated source frames leave no stale trails.
- Original, 9:16, 16:9, and 1:1 with Fit/Fill keep the rectangle aligned.
- 720p and 1080p renders preserve the reviewed rectangle, time range, and quality behavior.
