# Phase 6E.1.1 Blur Drag Stability — AndroidIDE / LogWire Verification

## Build

From the project root:

```bash
bash scripts/verify_phase6e1_source.sh
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

Confirm project identity `RecapFlowAI_Phase6E1_1` and version
`1.0-phase6e1.1` before installation.

## Prepare App Logs

1. Open Android Code Studio → App Logs and clear the previous output.
2. Keep the log view open once to confirm `Powered by LogWire` is visible.
3. Install and launch the new RecapFlow build.
4. Filter/search for `RecapFlowPreview`, `RecapFlowBlur`, `AndroidRuntime`,
   `FATAL EXCEPTION`, `Fatal signal`, `VideoFrameProcessingException`, and `GlException`.

LogWire is supplied by the development environment. Do not add its repository as a release
dependency for this test.

## Reproduce the former crash path

1. Import the same video that previously crashed.
2. Open Editor → Overlay, turn Overlay and Source subtitle blur On.
3. Play the source and drag the complete blur guide continuously for 20 seconds.
4. Pause on a detailed frame and repeat the drag for 20 seconds.
5. Resize from the bottom corner to minimum/maximum at least ten times.
6. Release near all four preview corners and confirm the final blur catches up immediately.
7. Sweep Horizontal, Vertical, Width, Height, Strength, and time-range controls.

Expected:

- The guide follows each touch frame and never leaves the preview.
- The GPU blur may trail the guide by no more than the 140 ms coalescing interval.
- The app remains alive and playback controls remain usable.
- App Logs contain `Applied live effects` records under `RecapFlowPreview`; raw touch
  coordinates, filenames, and source paths are not logged.

## Failure evidence

If the app still closes, do not rebuild immediately. In App Logs copy:

- the first `FATAL EXCEPTION` through the final `Caused by`, or
- `Fatal signal` plus `Abort message`, or
- the complete `RecapFlowPreview` / `VideoFrameProcessingException` block.

Also record whether the crash happened during guide drag, resize, a slider, playing, or paused.
If App Logs remain empty, use an ADB shell after reproduction:

```bash
adb logcat -b crash -d -v threadtime
adb logcat -d -v threadtime | grep -E "RecapFlowPreview|RecapFlowBlur|AndroidRuntime|Fatal signal|VideoFrameProcessingException|GlException"
```

## Regression

- Verify Overlay Off and Source blur Off are still no-ops.
- Verify localized inside-region blur and sharp outside-region pixels.
- Rotate and background/foreground during an edit.
- Replace the source and confirm the previous queued preview is not applied.
- Render/play 720p, then render/play 1080p and compare the final released rectangle.
- Repeat with Transform, Adaptive Cuts, Freeze, and each Audio policy as practical.
