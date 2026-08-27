# Phase 6B.3 Horizontal Mirror — AndroidIDE Verification

## Identity

- Project: `RecapFlowAI_Phase6B3`
- Version: `1.0-phase6b3`
- Preflight: `bash scripts/verify_phase6b3_source.sh`

## Build

```bash
bash scripts/verify_phase6b3_source.sh
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

## Device matrix

Use a source with an obvious left/right cue such as readable text, a raised
hand, or an object positioned on one side.

1. Import the source and open Editor → Transform. Leave Transform Off and
   confirm source playback orientation is unchanged.
2. Turn Transform On, leave Mirror Off, pause on the recognizable frame, and
   note the current timestamp.
3. Turn Mirror On. Confirm the same paused timestamp redraws immediately with
   left and right reversed.
4. Resume playback and toggle Mirror Off/On. Confirm playback does not restart
   and the new orientation appears on following frames.
5. Enable asymmetric Custom crop. Confirm Crop operates on the selected source
   edges and Mirror flips the cropped result.
6. Test Original, 9:16 Fit, 9:16 Fill, 16:9, and 1:1. Confirm aspect framing and
   Mirror both update in the live preview without stretching.
7. Turn the master Transform switch Off while Mirror remains remembered On.
   Confirm the preview returns to ordinary source orientation and the Mirror
   summary reports that it is remembered but omitted.
8. Turn Transform back On and confirm Mirror is restored without reselecting it.
9. Rotate/recreate the Activity and confirm Transform, Mirror, Crop, aspect, and
   scale selections remain intact.
10. Render 720p, play it, and compare the recognizable frame with the live
    preview. Confirm orientation/framing match and Mirror is not applied twice.
11. Unlock, render, and play 1080p. Confirm audio is present and visible A/V
    sync remains within the Phase 6 tolerance.
12. Start another render, confirm Mirror is locked, then cancel. Confirm the
    incomplete output is removed and the source remains available.
13. Repeat Off/On preview on the lowest-capability supported device. If the live
    effect fails, confirm source preview is restored, the fallback is explained,
    and the Mirror render plan remains selected.

## Pass evidence

Record device/API, source codec, recognizable left/right cue, paused timestamp,
playing and paused toggle results, Crop/Aspect combinations, 720p/1080p parity,
A/V observation, restoration, cancellation, and any fallback.

Static source checks do not replace this Android device matrix.
