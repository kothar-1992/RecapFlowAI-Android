# Task: Phase 6B.1 Aspect Ratio and Fit/Fill

## Outcome

Give the Android user an explicit, reversible transform that can convert a
trimmed source to Original, 9:16, 16:9, or 1:1 without stretching the image.

## Acceptance

- Transform defaults to Off.
- Clips and Transform occupy separate Review Editor tabs; only one panel is
  visible at a time.
- Off compiles no optional transform and uses the Phase 6A render path.
- On + Original is an explicit no-op.
- Fit preserves the whole source frame and may letterbox/pillarbox.
- Fill center-crops frame edges and produces no aspect-ratio bars.
- 720p and 1080p output dimensions match the selected aspect.
- Last aspect and Fit/Fill selections survive master Off and Activity recreation.
- The selected Clips/Transform tab survives Activity recreation.
- Editing a transform after a completed render invalidates the old output state.
- Output playback preview uses the rendered aspect.
- Trim, cancellation, playback unlock, source preservation, and device profile
  remain unchanged.

## Non-goals

- Custom crop rectangle
- Zoom, mirror, color, freeze, speed, or transitions
- Adaptive edit/ATS
- Audio, overlay, subtitle, or Gemini controls

## Verification commands

```bash
bash scripts/verify_phase6b1_source.sh
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

The Gradle commands must be executed in AndroidIDE when the Gradle 9 wrapper
distribution and Android toolchain are unavailable in the delivery workspace.
