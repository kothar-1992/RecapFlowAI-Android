# Phase 6E.2.1 AndroidIDE Verification

## Build

From the project root in AndroidIDE:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=false --no-daemon
```

Confirm the installed app reports version `1.0-phase6e2.1`.

## Realtime control matrix

Use a transparent PNG with an obvious asymmetric shape and a portrait source video.

1. Enable Overlay and Image/logo, then select the PNG.
2. While paused, test TL, TR, Center, BL, and BR; each must redraw at the selected location.
3. Move Horizontal center and Vertical center to both extremes and a middle value.
4. Move Logo width through 8%, 35%, and 80%; the image must preserve its own aspect ratio.
5. Move Opacity through 10%, 50%, and 100%.
6. Restrict Active source time, seek before/inside/after the range, and confirm exact omission.
7. Repeat steps 2–6 while the preview is playing.
8. Turn the item Off, turn the Overlay master Off, Remove, and Replace; no previous logo may leak.
9. Repeat on Original, 9:16, 16:9, and 1:1 with Fit and Fill.
10. Verify adaptive candidate/sequence previews use the same logo state.

## Export parity

Render 720p, then 1080p. Compare position, width, opacity, active time, aspect ratio, and edge
clamping with the reviewed preview. Confirm audio/video duration and playback remain valid.

## Safety check

Logo editing must remain preset/slider based. Do not expect direct screen-touch drag/resize in this
gate. Source-subtitle-blur direct touch also remains disabled.

## Failure evidence

If a mismatch remains, record the control name/value, paused or playing state, aspect + Fit/Fill,
source codec/resolution, and LogWire output. Keep the exported test file for comparison.
