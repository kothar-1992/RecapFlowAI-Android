# Phase 6C.2.1 Movable Preview — AndroidIDE and Device Verification

## 1. Identity and build

Confirm root project `RecapFlowAI_Phase6C2_1` and version `1.0-phase6c2.1`, then run:

```bash
bash scripts/verify_phase6c2_1_source.sh
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true
```

## 2. Move and boundary behavior

1. Import portrait and landscape sources.
2. Drag only the **Move** handle to every corner and screen edge.
3. Confirm the full card remains inside the Editor viewport and does not enter the
   bottom navigation area.
4. Confirm dragging does not scroll the editor sheet until the gesture ends.
5. Play, pause, seek, skip, and open/hide PlayerView controls after moving the card.

## 3. Resize and reset

1. Drag the bottom-right resize handle inward to the minimum and outward to the
   largest allowed size.
2. Confirm aspect ratio remains stable and the video is never stretched.
3. Resize near each boundary; confirm the card clamps without disappearing.
4. Tap **Reset**; confirm one-third adaptive size and centered-top placement return.
5. Confirm reset does not change playback position, Transform values, Adaptive
   draft/apply state, or a running render.

## 4. Underlay and lifecycle

- Scroll the sheet beneath previews at several positions/sizes. Only the exact
  preview rectangle should dim; all uncovered text must remain normal.
- Rotate/recreate after moving and resizing. Confirm normalized position/scale restore
  and clamp safely to the new dimensions.
- Switch Home/Editor/Settings, background/foreground the app, and import another video.
- Test compact phone, `sw600dp` tablet, portrait, landscape, and large-text settings.

## 5. Media regressions

- Preview one Adaptive candidate and the full cut sequence.
- Exercise Crop/Mirror/Color/Zoom/Aspect/Speed/Fade realtime preview.
- Render/play 720p, unlock/render/play 1080p, and confirm preview customization does
  not alter output dimensions, duration, codecs, audio, or A/V sync.
- Cancel at mid-render and confirm incomplete-output cleanup/source preservation.

Record device/API, screen dp/px, orientation, source aspect, preview start/end
position and scale, boundary behavior, playback controls, underlay locality, and
720p/1080p regression result.
