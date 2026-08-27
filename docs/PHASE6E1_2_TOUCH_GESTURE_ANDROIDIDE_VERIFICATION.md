# Phase 6E.1.2 Touch Gesture Isolation — AndroidIDE / LogWire Verification

## Build and identity

From the project root:

```bash
bash scripts/verify_phase6e1_source.sh
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

Before installing, confirm:

```text
root project: RecapFlowAI_Phase6E1_2
version:      1.0-phase6e1.2
```

Uninstall the older test APK first if AndroidIDE presents stale resources, then install and
open the new build.

## LogWire preparation

1. Open Android Code Studio → App Logs and clear previous output.
2. Filter for `RecapFlowBlur`, `RecapFlowPreview`, `AndroidRuntime`, `FATAL EXCEPTION`,
   `Fatal signal`, `VideoFrameProcessingException`, and `GlException`.
3. Keep App Logs visible, then reproduce with the same source used in the report.

LogWire remains a development-environment receiver and is not bundled in RecapFlow.

## Direct-touch matrix

1. Open Editor → Overlay and enable Overlay + Source subtitle blur.
2. While playing, drag the complete marked guide to each corner at least 20 times.
3. Resize from the bottom corner between minimum and maximum at least 20 times.
4. Pause and repeat both sets.
5. During a drag, verify the outline follows the pointer. The actual blur pixels are allowed
   to stay at the last committed position until the finger is released.
6. On release, verify the blur and Horizontal/Vertical/Width/Height values jump once to the
   exact released rectangle.
7. Immediately use every geometry slider and strength/time controls; they must remain stable.

Expected LogWire records include `Blur guide drag committed on release` or
`Blur guide resize committed on release`. They must not contain filenames, content URIs,
source paths, or raw pointer coordinates.

## Lifecycle and export regression

- Begin a gesture, release it, rotate/recreate, and confirm the guide has no stale translation
  or scale.
- Collapse/expand Overlay controls and switch to Clips/Transform/Audio and back.
- Replace the source and confirm the default blur state is safe for the new media.
- Render and play 720p, unlock/render/play 1080p, and compare the released rectangle, strength,
  time range, outside-region sharpness, duration, and A/V sync.
- Cancel an intermediate render and confirm incomplete output is removed while source remains.

## Failure capture

If the app still terminates, do not change code before collecting one complete block:

- `FATAL EXCEPTION` through the last `Caused by`, or
- `Fatal signal` plus `Abort message`, or
- the full `RecapFlowBlur` / `RecapFlowPreview` error block.

Also record: drag or resize, playing or paused, whether the outline moved before termination,
and whether the crash occurred before or after finger release.
