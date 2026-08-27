# Phase 6B.7.1 Collapsible Transform — AndroidIDE and Device Verification

Date: 2026-08-22

## 1. Source preflight

```bash
bash scripts/verify_phase6b7_1_source.sh
```

Confirm root project `RecapFlowAI_Phase6B7_1` and version `1.0-phase6b7.1`.

## 2. Compile and install

```bash
./gradlew --stop
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true
```

Install the debug APK and open one portrait video.

## 3. UI behavior

1. Open Editor → Transform and confirm `Hide controls ▲` is visible.
2. Enable Transform plus any two features and note their values.
3. Tap Hide; confirm the detailed controls disappear while badge, master switch,
   summary, and `Show controls ▼` remain visible.
4. Confirm the summary still names the enabled operations/presets.
5. Tap Show and confirm every switch, slider, and preset is unchanged.
6. Repeat with master Transform Off; collapse must not change remembered values.
7. Collapse, rotate/recreate the Activity, and confirm it remains collapsed.
8. Expand, rotate/recreate again, and confirm it remains expanded.

## 4. Preview/render regression

- Start playback with several transforms enabled, then Show/Hide repeatedly;
  playback position and live effects must remain unchanged.
- Begin a 720p render, collapse/expand during rendering, and confirm the render
  continues without restart or cancellation.
- Play the output and confirm the selected transforms remain present.
- Run the Phase 6B.7 Freeze preview/export checks separately because this UI
  refinement does not replace that media verification gate.
