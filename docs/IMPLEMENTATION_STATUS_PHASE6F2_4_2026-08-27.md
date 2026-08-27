# Phase 6F.2.4 implementation status — 2026-08-27

## Status

Source implementation is complete. AndroidIDE build/install and physical-device verification are
still required.

Workspace verification completed:

- `bash scripts/verify_phase6f2_4_source.sh` — PASS, including all retained source gates.
- XML parse of all 27 app resource files — PASS.
- `bash -n scripts/verify_phase6f2_4_source.sh` — PASS.
- `./gradlew :app:testDebugUnitTest --stacktrace` — BLOCKED before Gradle started because the
  Gradle 9.0.0 distribution is not cached and `services.gradle.org` is unreachable here. The test
  and build commands remain required in AndroidIDE.

## Source evidence

- `PreviewUiState` separates live effects, playable source-only fallback, and terminal
  unavailability from the edit/render plan.
- `RealtimePreviewSession` bounds automatic recovery to one attempt per generation, tracks an
  applying graph, and confirms it only after a rendered frame.
- `MainActivity` recreates ExoPlayer before fallback, preserves position/play intent, stops
  automatic retries on edit changes, and exposes one explicit retry action.
- `PreviewGeometryPolicy` keeps preview at the source short side up to 720 pixels while
  `RenderPreset` remains authoritative for 720p/1080p/2K final output.
- User-facing fallback text no longer implies that FFmpeg metadata is equivalent to a playable
  preview.
- Unit-test source covers recovery bounds, applying/confirmed graph state, low-resolution
  no-upscale behavior, high-resolution preview caps, and even dimensions.

## Preserved baseline

- Gallery-first video import and document fallback.
- Typed Trim/Adaptive/Transform/Audio/Overlay settings.
- Exact user-selected 720p/1080p/2K render path and post-render validation.
- Private-first output, cancellation cleanup, MediaStore publication, Open/Share, and optional
  rendered-output playback.
- FFmpeg JNI probe/version path; FFmpeg is not presented as the ExoPlayer preview renderer.

## Verification boundary

The source verifier and JVM tests are automation evidence only. The owner-device matrix must prove
decoder recovery, surface behavior, GPU effect compatibility, 540×960 playback, and final media
properties before this gate is marked complete.
