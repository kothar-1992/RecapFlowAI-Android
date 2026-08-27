# Phase 6E.1.5 Fit Preview Aspect Parity — AndroidIDE Verification

## Build

```bash
bash scripts/verify_phase6e1_source.sh
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

Confirm the fresh project is `RecapFlowAI_Phase6E1_5` and the installed version is
`1.0-phase6e1.5`.

## Portrait source matrix

Use the same portrait source that exposed the stretch regression.

1. Transform Off / Original: confirm the portrait source is proportional.
2. Transform On → 9:16 → Fit: confirm the whole source remains proportional.
3. Select 16:9 → Fit: the card becomes landscape, but people, circles, and text keep their
   original proportions. The whole portrait image remains centered with black side bars.
4. Select 16:9 → Fill: the card remains landscape and source edges are center-cropped. No
   horizontal or vertical stretch is allowed.
5. Repeat 1:1 Fit/Fill and switch rapidly through Original → 9:16 → 16:9 → 1:1 while playing
   and paused.
6. Move and resize the floating preview, then Reset it and repeat 16:9 Fit/Fill.

## Export parity

1. Render/play 720p for 16:9 Fit; compare framing and source proportions with live preview.
2. Unlock and render/play 1080p; compare again.
3. Repeat one 16:9 Fill render and confirm center crop without stretch.
4. Check duration, source/replacement/mixed audio, A/V sync, cancellation, and output cleanup.

## Regression checks

- Repeat with a landscape source converting to 9:16 Fit and Fill.
- Enable custom Crop, Mirror, Color, Zoom, Speed, and one Visual Fade combination.
- Open Overlay and confirm slider-only source blur remains stable and direct guide touch is
  still disabled.
- If live effects fall back, confirm the card returns to undistorted source-aspect playback.

Capture `RecapFlowPreview`, Player, `AndroidRuntime`, `FATAL EXCEPTION`, and `GlException`
logs if any frame stretches, leaves the card, or the preview fails to redraw.
