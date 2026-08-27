# Phase 6B.4 Color — AndroidIDE Verification

## Identity

- Project: `RecapFlowAI_Phase6B4`
- Version: `1.0-phase6b4`
- Preflight: `bash scripts/verify_phase6b4_source.sh`

## Build

```bash
bash scripts/verify_phase6b4_source.sh
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

## Device matrix

Use a well-lit source containing skin tones, saturated colors, white/gray
objects, dark shadows, and an obvious left/right cue.

1. Import the source and leave Transform Off. Confirm ordinary playback color.
2. Open Editor → Transform, turn Transform On, then Color On. Confirm all four
   values begin at zero and the preview remains neutral.
3. Pause on a recognizable frame. Move Brightness through `-50`, `0`, and
   `+50`; confirm the same timestamp redraws after every change.
4. Repeat for Contrast `-50/0/+50` and Saturation `-100/0/+100`. At `-100`,
   confirm the image approaches grayscale without changing framing.
5. Move Temperature from `-50` cool through `0` to `+50` warm. Confirm blue/red
   balance changes without a size or orientation change.
6. Set non-zero values on all four controls and tap Reset Color. Confirm all
   labels return to zero and the preview returns to neutral immediately.
7. While playback runs, move each control once. Confirm playback does not
   restart and the new appearance arrives on following frames.
8. Combine asymmetric Custom crop, Mirror On, 9:16 Fit/Fill, and non-zero Color.
   Confirm every operation remains visible and no stretching occurs.
9. Turn Transform Off while Color remains remembered On. Confirm source color
   returns to ordinary playback and the summary says Color is omitted.
10. Turn Transform back On and confirm the saved Color values return.
11. Rotate/recreate the Activity and confirm Transform, Color, four values,
    Mirror, Crop, aspect, and scale selections are restored.
12. Render/play 720p and compare the paused source frame with output. Confirm
    color, orientation, and framing match and no effect is applied twice.
13. Unlock, render, and play 1080p. Confirm audio remains present and visible
    A/V sync stays within the Phase 6 tolerance.
14. Start another render, confirm Color controls are locked, then cancel.
    Confirm incomplete output cleanup and source preservation.
15. Repeat a moderate adjustment on the lowest-capability supported device. If
    live effects fail, confirm the explained source-preview fallback and that
    the selected Color render plan remains intact.

## Pass evidence

Record device/API, input metadata, slider values, paused/playing behavior,
Reset, combined-effect results, recreation, 720p/1080p comparison, A/V check,
cancellation, and any preview fallback.

Static source checks do not replace this Android device matrix.
