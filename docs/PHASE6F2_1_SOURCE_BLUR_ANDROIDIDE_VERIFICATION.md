# Phase 6F.2.1 AndroidIDE and device verification

## Source preflight

From the extracted project root run:

```bash
bash scripts/verify_phase6f2_1_source.sh
```

## Build

1. Open `RecapFlowAI_Phase6F2_1` in AndroidIDE.
2. Confirm version `1.0-phase6f2.1`, minSdk 28, Media3 1.10.0, and ARM64.
3. Run:

```bash
./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true
```

4. Install the APK and confirm JNI/FFmpeg diagnostics still load.

## Blur quality matrix

Use an original portrait source containing one crisp burned-in subtitle line. Pause on that line
and use the same rectangle for every check.

1. Test strength 4, 14, 30, and 32. At 30/32, text must be unreadable and must not appear as
   repeated rows, columns, mirrored edges, or discrete copies.
2. Play, pause, scrub backward/forward, toggle Blur Off/On, and change the active time range. No
   previous frame or subtitle position may remain as a trail.
3. Move the rectangle with Horizontal/Vertical sliders and resize with Width/Height sliders. Pixels
   outside the guide must remain sharp. Direct guide touch remains disabled.
4. Repeat near the top, bottom, left, and right frame edges; no wrap-around pixels may enter the
   rectangle.
5. Repeat Original, 9:16, 16:9, and 1:1 with Fit and Fill; verify guide/effect alignment and no
   person/text stretching.

## Preview/export parity

1. Export the same paused-source setup at 720p and 1080p.
2. Extract or pause on the matching timestamp and compare rectangle, strength, feather, and outside
   pixels with realtime preview.
3. Confirm logo remains sharp above the source blur, audio is synchronized, actual bitrate is
   reported, Gallery/Open/Share work, and cancellation removes only incomplete output.

Record device model, Android version, source dimensions/codec, strength, output dimensions,
requested/actual bitrate, and pass/fail screenshots for the owner gate.
