# Phase 6C.2 Continuous Preview — AndroidIDE and Device Verification

## 1. Source identity and build

Confirm root project `RecapFlowAI_Phase6C2` and version `1.0-phase6c2`, then run:

```bash
bash scripts/verify_phase6c2_source.sh
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true
```

Install the new APK over a disposable test build or uninstall the old test build
first if AndroidIDE reports a signature mismatch.

## 2. Continuous review preview

1. Import a 30–120 second video, set a non-full Trim, and generate each Adaptive preset.
2. Leave Apply Off and tap **Preview full cut sequence**.
3. Confirm every reviewed range plays once in chronological order and the candidate
   counter advances at each item boundary.
4. Tap **Stop sequence preview** mid-item; confirm the ordinary source preview returns
   at the corresponding source position and remains paused.
5. Let the sequence end; confirm it restores the ordinary source preview and the button
   returns to **Preview full cut sequence**.
6. Repeat after background/foreground, tab navigation, rotation, and a source change.

## 3. Realtime transform and fade parity

With Transform On, test Original/9:16/16:9/1:1, Fit/Fill, Custom Crop, Mirror,
non-neutral Color, each Zoom mode, and Speed 0.5×/1×/2× during sequence preview.

For each Fade duration (0.5/1/1.5 seconds), test:

- Fade In: each range starts black and becomes visible.
- Fade Out: each range ends at black.
- In + Out: adjacent ranges meet through black without an overlapping dissolve.
- Transition Off: no edge fade is visible.

Choose a draft containing a range shorter than the required wall-clock fade span;
confirm render validation blocks it instead of starting an invalid export.

## 4. Export matrix

Render and play 720p, then 1080p, for Apply Off and Apply On with: Fade Off; all
three fade modes; Speed 0.5×/2×; combined Crop/Mirror/Color/Zoom; nonzero Trim;
Intro Freeze; H.264 input; and HEVC input.

Verify sequence order, first/last kept frames, preview/export visual parity, expected
duration, H.264/AAC playback, source-audio continuity, and A/V sync at every boundary.
Transition must change pixels only; it must not add duration or fade audio.

## 5. Failure and device matrix

- Trigger preview fallback with a demanding HEVC file; confirm the app reports fallback
  and restores a usable source preview rather than changing render settings.
- Cancel an active multi-range render, render again, and confirm only the incomplete
  output was removed.
- Test portrait/landscape, phone/tablet, compact/large display, balanced/limited device
  profile, low free storage, and Wi-Fi/offline states.
- Confirm Home/Editor/Settings, Trim, overlay player, 720p playback verification, and
  1080p unlock still work.

Record device/API, codec/audio, preset, reviewed ranges, transition/speed, expected and
actual duration, output path/size, elapsed time, playback result, and boundary A/V notes.
