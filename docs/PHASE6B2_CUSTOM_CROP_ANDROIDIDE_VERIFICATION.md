# Phase 6B.2 Custom Crop — AndroidIDE Verification

## Identity

- Project: `RecapFlowAI_Phase6B2`
- Version: `1.0-phase6b2`
- Preflight: `bash scripts/verify_phase6b2_source.sh`

## Build

```bash
bash scripts/verify_phase6b2_source.sh
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

## Device matrix

1. Import a known video and open Editor → Transform.
2. With Transform Off, render 720p and confirm the verified source-aspect path.
3. Turn Transform On, leave Custom crop Off, and verify Phase 6B.1 Fit/Fill.
4. Turn Custom crop On. Confirm the four edge controls appear.
5. Set an unmistakably asymmetric rectangle, for example L 5%, T 20%, R 30%,
   B 10%, then render 720p.
6. Confirm the correct edges were removed, the picture is not stretched, audio
   is present, and visible A/V sync remains within the Phase 6 tolerance.
7. Open the 720p result, unlock 1080p, and render the same plan at 1080p.
8. Rotate/recreate the Activity and confirm Transform, Crop, aspect, scale, and
   all four edge values are restored.
9. Start another render and confirm every crop control is disabled until the
   render completes or is cancelled.
10. Cancel once during rendering; confirm only the incomplete output is removed
    and the source remains playable.
11. Turn Crop Off and verify the remembered edge values remain but the next
    output contains no custom crop.

## Pass evidence

Record the source and output duration, dimensions, orientation, codecs, audio
presence, device encoder, crop values, output path, and a short playback check.

Static source checks do not replace this Android device matrix.
