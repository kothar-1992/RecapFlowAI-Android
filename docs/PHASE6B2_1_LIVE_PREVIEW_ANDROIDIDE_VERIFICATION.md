# Phase 6B.2.1 Live Transform Preview — AndroidIDE Verification

## Identity

- Project: `RecapFlowAI_Phase6B2_1`
- Version: `1.0-phase6b2.1`
- Preflight: `bash scripts/verify_phase6b2_1_source.sh`

## Build

```bash
bash scripts/verify_phase6b2_1_source.sh
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

## Live-preview matrix

1. Import a known video, open Editor → Transform, and leave Transform Off.
   Play/pause/seek must behave as ordinary source playback.
2. Pause on a recognizable frame and turn Transform On. The frame must remain
   at the same time position.
3. Select 9:16, 16:9, then 1:1. Confirm the floating preview changes shape and
   framing immediately without starting a render.
4. Compare Fit and Fill. Fit must preserve the whole source; Fill must center
   crop without stretching.
5. Enable Custom crop and move each edge asymmetrically. Confirm the paused
   frame redraws after each adjustment and the correct edge is removed.
6. Resume playback while changing one control. Confirm playback continues and
   the new effect appears on following frames.
7. Render 720p, play the output, and compare it with the live preview. Confirm
   Crop/Aspect/Fit/Fill framing matches and no effect is applied twice.
8. Render and play 1080p. Confirm audio remains present and visible A/V sync is
   within the Phase 6 tolerance.
9. Turn Transform Off and confirm both preview and the next render return to the
   verified source-aspect path.
10. Repeat one test on the lowest-capability supported device. If live effects
    fail, confirm the app reports the fallback, restores source preview, keeps
    the selected settings, and can still render.

## Pass evidence

Record device/API, source codec, chosen settings, whether paused redraw and
playing updates passed, 720p/1080p comparison, A/V check, and any fallback.

Static source checks do not replace this Android device matrix.
