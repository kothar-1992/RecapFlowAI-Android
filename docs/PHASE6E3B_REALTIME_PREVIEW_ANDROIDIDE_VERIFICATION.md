# Phase 6E.3B AndroidIDE and device verification

## 1. Build gate

Open `RecapFlowAI_Phase6E3B` and run:

```bash
bash scripts/verify_phase6e3b_source.sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Confirm `versionName = "1.0-phase6e3b"`, Media3 `1.10.0`, minSdk 28, and no new permission.

## 2. Retained preview matrix

Run once with a portrait H.264 source and once with a portrait HEVC source:

1. Pause on a frame with source subtitles.
2. Change blur X/Y/Width/Height/Strength and active time rapidly for at least 15 seconds.
3. Confirm every latest value appears on that paused frame; resume playback and repeat.
4. Enable a logo, change all presets, X/Y/Width/Opacity/time, replace it, disable it, and remove it.
5. Alternate 9:16, 16:9, 1:1, Original, Fit/Fill, crop, mirror, color, zoom, speed, and transition.
6. Change Trim repeatedly, then use Adaptive candidate and full-sequence previews.
7. Switch Keep/Mute/Replace/Mix and gains without rendering between operations.
8. Confirm the preview does not jump back to 00:00, lose its last frame, remain on Loading, or
   show a stale blur/logo value.

## 3. Recovery matrix

- Background/foreground the app while preview is paused and while playing.
- Temporarily make a test source unavailable or reproduce a decoder error.
- Confirm one no-effects fallback resumes near the last valid position.
- If the fallback also fails, wait at least 10 seconds and confirm the spinner stops and
  Preview unavailable is visible.
- Change a control after a successful fallback and confirm a new live session can be attempted.

## 4. Export regression

Render the reviewed plan at 720p, play it, then render 1080p. Verify duration, A/V sync, aspect,
blur rectangle/strength/time, logo geometry/opacity/time, and adaptive/audio behavior match the
reviewed settings. Cancel separate renders around 10% and 50%; source files must remain intact and
partial outputs must be removed.

## Pass criteria

- No crash, ANR, black/half frame, stretched frame, stale effect, or indefinite loading.
- Direct blur/logo screen-touch editing remains unavailable.
- No render/export behavior changed from Phase 6E.3A.
