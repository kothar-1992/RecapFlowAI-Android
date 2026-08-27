# Phase 6B.6 Speed — AndroidIDE and Device Verification

Date: 2026-08-22

## 1. Source preflight

```bash
bash scripts/verify_phase6b6_source.sh
```

Confirm the project title is `RecapFlowAI_Phase6B6` and the installed version is
`1.0-phase6b6`.

## 2. Compile and test

```bash
./gradlew --stop
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true
```

Install the generated debug APK without clearing app data unless a clean-state
test is intentional.

## 3. Preview matrix

Use one portrait clip with speech/music and one landscape clip:

1. Open Editor → Transform; confirm Speed is Off by default.
2. Turn master Transform on, then Speed on.
3. While playing, select `0.5×`, `0.75×`, `1×`, `1.25×`, `1.5×`, and `2×`.
4. Confirm each selection takes effect immediately without jumping the source
   position; `1×` sounds and looks original.
5. Pause and change presets; resume and confirm the selected speed is used.
6. Turn Speed off and confirm immediate `1×` playback; turn it on and confirm the
   remembered preset returns.
7. Turn master Transform off/on and repeat the remembered-state check.
8. Rotate/recreate the Activity and confirm On/Off plus preset restore.

## 4. Export matrix

For `0.5×`, `1×`, `1.25×`, and `2×`, render 720p, play it to unlock 1080p, then
render 1080p. For each output:

- rendered playback itself runs at `1×` in the preview player;
- expected duration is `selected clip duration / speed`;
- actual duration is within `max(100 ms, 3 frames)` of expected;
- speech/music pitch is natural for the chosen speed and no track stays at the
  original timing;
- audio/video sync is within 100 ms at the beginning, middle, and end;
- Crop, Mirror, Color, Zoom, Aspect, and Speed match the live source preview.

## 5. Lifecycle and safety

- Start a long slow-motion render, cancel it, and confirm incomplete output is
  removed while the source remains playable.
- Background/foreground during preview and render; confirm no crash or duplicate
  render.
- Verify low/balanced device profiles remain responsive across rapid preset
  changes.

Record device model/API, source codec/resolution, preset, expected/actual
duration, A/V result, and encoder diagnostics for any failure.
