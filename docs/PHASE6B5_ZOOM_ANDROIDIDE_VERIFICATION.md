# Phase 6B.5 Zoom — AndroidIDE and Device Verification

Source-only checks are not a substitute for the following target-device gate.

## 1. Confirm the source package

```bash
bash scripts/verify_phase6b5_source.sh
```

Confirm the project title is `RecapFlowAI_Phase6B5` and the installed version is
`1.0-phase6b5`.

## 2. Compile and test Kotlin

```bash
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
```

Expected: both tasks finish successfully, including `ZoomCompilerTest`.

## 3. Build the FFmpeg-enabled APK

Use the same AndroidIDE/NDK/FFmpeg setup that passed Phase 6B.4:

```bash
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

Install the debug APK and confirm the Home and Settings toolbar subtitles show
`Phase 6B.5`.

## 4. Live preview matrix

Import one video with visible edges and movement, open Editor → Transform, and
turn the master Transform switch on.

- Zoom Off: preview must match the source and Zoom must be absent from export.
- Zoom In: the center stays fixed and outer edges crop by the expected amount.
- Zoom Out: the full image gets smaller and background appears around it.
- Alternate: scale moves smoothly from neutral to in, neutral, out, and back.
- Change each mode while playing; playback time must not reset.
- Pause on a clear frame and change each mode; that frame must redraw immediately.
- Turn Zoom off, then on; the previously selected mode must return.
- Turn master Transform off, then on; the Zoom selection must remain remembered.
- Rotate/recreate the Activity; Zoom switch and mode must restore.

## 5. Composition and parity matrix

Test Zoom with Crop, Mirror, Color, and each supported Aspect/Fit/Fill choice.
The visible effect order must remain Crop → Mirror → Color → Zoom → Presentation.

Render at 720p, play the completed output, then render the same plan at 1080p.
Compare representative frames and Alternate-cycle movement against the source
preview. Rendered-output playback must not apply Zoom a second time.

## 6. Regression and safety

- Confirm duration tolerance: `max(100 ms, 3 frames)`.
- Confirm A/V sync remains within 100 ms.
- Cancel during a Zoom render and confirm only the incomplete output is deleted.
- Confirm the source working file remains readable.
- If live effects fail, confirm the source-preview fallback message appears and
  render settings stay intact.

Record device model, Android/API, mode, aspect, preset, encoder, elapsed time,
output size, duration delta, A/V observation, and pass/fail.
