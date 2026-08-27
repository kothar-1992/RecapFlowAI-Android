# Phase 6E.1.4 Touch Rollback + Preview Bounds Recovery — AndroidIDE Verification

## Build

```bash
bash scripts/verify_phase6e1_source.sh
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

Confirm the fresh project is `RecapFlowAI_Phase6E1_4` and the installed version is
`1.0-phase6e1.4`.

## Slider-only blur safety

1. Import the source that reproduced the release crash.
2. Open Editor → Overlay and enable Overlay + Source subtitle blur.
3. Confirm the guide has no resize handle and dragging/tapping the guide does not move it or
   close the Activity. Player controls beneath it must remain usable where applicable.
4. Sweep Horizontal, Vertical, Width, and Height repeatedly while playing and paused.
5. Sweep strength and source-time range, then use Reset.
6. Confirm the guide and localized blur follow every committed slider value in realtime.
7. Render/play 720p, then render/play 1080p; compare rectangle, timing, outside-region
   sharpness, duration, and A/V sync.

## Preview aspect and bounds recovery

Test at least one portrait and one landscape source; include H.264 and HEVC if available.

1. Start from Transform Off / Original and verify the whole source is visible.
2. Enable Transform and switch repeatedly through Original → 9:16 → 16:9 → 1:1.
3. Test both Fit and Fill. Fit may letterbox and Fill may crop, but neither may stretch,
   leave only a narrow video sliver, or draw pixels outside the preview card.
4. Move and resize the preview card, repeat the aspect sequence, then use Reset.
5. Scroll the sheet under the preview and confirm the controller and video pixels stay
   aligned and the localized underlay stays bounded to the card.
6. Repeat while playing and paused, rotate/recreate the Activity, and switch Editor tabs.
7. Compare at least one 720p and 1080p output with the live preview.

## Logs and failure capture

Filter Android Code Studio App Logs for `RecapFlowBlur`, `RecapFlowPreview`,
`AndroidRuntime`, `FATAL EXCEPTION`, `Fatal signal`, and `GlException`.

Expected safety marker:

```text
Direct-touch blur geometry is temporarily disabled
```

If the video surface is still offset, capture a screenshot plus the complete
`RecapFlowPreview`/Player error block and record source codec, rotation, selected aspect,
Fit/Fill, whether playback was paused, and whether the card had been moved/resized.
