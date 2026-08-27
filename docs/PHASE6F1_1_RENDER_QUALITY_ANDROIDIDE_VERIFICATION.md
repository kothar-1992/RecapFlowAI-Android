# Phase 6F.1.1 AndroidIDE and device verification

## 1. Source and build

Open `RecapFlowAI_Phase6F1_1_1`, then run:

```bash
bash scripts/verify_phase6f1_1_source.sh
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

Confirm `versionName = "1.0-phase6f1.1.1"`, Media3 `1.10.0`, minSdk 28, and no new permission.
The source verifier must confirm `setEnableFallback(true)` and reject the unavailable
`setEnableFormatFallback` method before Gradle starts.

## 2. Use an original source

Choose an original H.264 or HEVC source, not a file beginning with `RecapFlow_`. Record source
resolution, duration, codec, frame rate, and bitrate from Video details.

Render 720p, play it, then render 1080p. For each result record:

- output dimensions and codec;
- displayed H.264 target and actual average bitrate;
- output size and duration;
- visible detail in a still frame and a high-motion scene;
- lip-sync/audio sync.

Expected floors are 25 Mbps for 720p and 30 Mbps for 1080p. Some hardware encoders may apply a
supported fallback; if so, the actual value must be visible rather than silently hidden.

## 3. Source-limit checks

- With a 576 × 1024 source, confirm the upscale warning appears for both presets. Higher bitrate
  should reduce added blockiness but cannot reconstruct detail absent from the source.
- Import a completed `RecapFlow_720p_...mp4` once and confirm the generation-loss warning appears.
  Do not use that file for the final quality comparison.

## 4. Regression checks

- Trim and Adaptive Cuts preserve duration/order.
- Fit/Fill, Crop, Mirror, Color, Zoom, Speed, Freeze, Fade, Blur, and Logo match preview.
- Keep/Mute/Replace/Mix audio remains synchronized.
- Cancel at roughly 10%, 50%, and 90%; incomplete private/public files disappear and source stays.
- The finalized Movies/RecapFlowAI item has the same byte count as the completed private output and
  opens/shares from the Export tab.

## Pass evidence

Attach one original-source metadata screenshot plus 720p and 1080p completion screenshots showing
target/actual bitrate. Record any fallback value and encoder name.
