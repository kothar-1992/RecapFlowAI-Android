# Phase 6B.7 Intro Freeze — AndroidIDE and Device Verification

Date: 2026-08-22

## 1. Source preflight

```bash
bash scripts/verify_phase6b7_source.sh
```

Confirm the project title is `RecapFlowAI_Phase6B7` and installed version is
`1.0-phase6b7`.

## 2. Compile and test

```bash
./gradlew --stop
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true
```

Install the debug APK without clearing app data unless a clean-state test is
intentional.

## 3. Preview matrix

Use one portrait H.264/AAC clip and one HEVC/AAC clip with a clearly audible
first word or beat:

1. Select a Trim start away from `00:00` and open Transform.
2. Confirm Intro Freeze is Off by default and preview remains unchanged.
3. Turn master Transform and Intro Freeze on; choose `1 sec`.
4. Tap `Preview intro freeze`; confirm the selected Trim-start frame stays still
   for about one second and source playback then begins from that frame.
5. Repeat for `2 sec` and `3 sec` while paused and while previously playing.
6. Confirm the button shows `Holding frame…` and duration controls are locked
   only during the hold.
7. Change Trim start and confirm the preview uses the new selected frame.
8. Turn Freeze off/on and master Transform off/on; confirm the preset is remembered.
9. Recreate/rotate the Activity and confirm On/Off plus duration restore.

## 4. Export matrix

Test Off and 1/2/3 seconds at 720p, then play 720p to unlock and repeat at
1080p. For at least one run, combine Crop, Mirror, Color, Zoom, and `2×` Speed.

- Freeze Off output matches the Phase 6B.6 path.
- Freeze On output begins with the selected Trim-start image.
- The intro contains silence; original audio begins with moving source content.
- No black frame, green frame, image rotation error, or discontinuity appears at
  the still-to-video boundary.
- Existing visual transforms match between freeze frame and moving content.
- Expected duration is `(selected clip duration / speed) + freeze duration`.
- Actual duration is within `max(100 ms, 3 frames)` of expected.
- A/V sync after the freeze is within 100 ms at the beginning, middle, and end.
- Rendered playback itself runs at `1×` because Speed is already baked in.

## 5. Lifecycle and cleanup

- Start a long 1080p freeze render, cancel during `Preparing`, and confirm no
  output is left and the source remains playable.
- Cancel during rendering and repeat the same checks.
- Background/foreground during frame preparation and rendering; confirm no
  crash, duplicate render, or stale completion.
- Run two Freeze exports consecutively and confirm both succeed without cache
  filename collision.

Record device model/API, source codec/resolution/rotation, selected Trim start,
Speed, Freeze duration, expected/actual duration, boundary quality, A/V result,
and encoder diagnostics for any failure.
