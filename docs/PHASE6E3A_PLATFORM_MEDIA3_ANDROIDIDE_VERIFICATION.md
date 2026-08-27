# Phase 6E.3A AndroidIDE verification

## 1. Confirm the source

Open the extracted `RecapFlowAI_Phase6E3A` project and confirm:

- `versionName = "1.0-phase6e3a"`
- `minSdk = 28`
- Media3 is `1.10.0`
- Core KTX is `1.16.0`
- AppCompat is `1.7.1`

Run from the project root:

```bash
bash scripts/verify_phase6e3a_source.sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Then run the FFmpeg-enabled build using the already-verified local SDK configuration:

```bash
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true
```

## 2. Installation compatibility

- Install on Android 9 / API 28 if a test device is available.
- Install on the primary Android 13 tablet.
- Confirm API 28 requests legacy read permission only when the picker path needs it.
- Confirm API 29+ uses the system picker without legacy read permission.
- Record device model, Android version, encoder names, and build duration.

## 3. Media regression matrix

For one H.264 and one HEVC portrait source:

1. Import and probe metadata.
2. Play, pause, seek, and reopen the preview.
3. Apply Trim, 9:16/16:9/1:1 Fit and Fill, mirror, color, zoom, speed, freeze,
   transitions, adaptive cuts, and audio controls one at a time.
4. Confirm source blur remains slider-only and is visible in realtime.
5. Confirm logo preset, position, size, opacity, and active range update while paused
   and playing.
6. Switch among editor sections without rendering; the preview must not remain in an
   indefinite loading state.
7. Render 720p, play the result, then render 1080p.
8. Compare preview/output geometry, blur rectangle, logo geometry, timing, and A/V sync.
9. Cancel a render near 10% and 50%; the source must survive and the partial output must
   be removed.
10. Rotate/recreate the Activity and confirm the current project remains usable.

## 4. Pass criteria

- No crash, ANR, stretched preview, black half-frame, or effect-state leak.
- H.264/AAC output opens and has the expected duration.
- Preview/export use the reviewed EditPlan.
- Direct blur/logo touch gestures remain unavailable.
- Private app storage output is accepted only for this baseline gate; public Gallery output
  is Phase 6F.1.

