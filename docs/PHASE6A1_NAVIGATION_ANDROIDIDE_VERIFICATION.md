# Phase 6A.1 Navigation AndroidIDE Verification

Use the same AndroidIDE, NDK/CMake, and FFmpeg prebuilt setup that passed Phase
5 and the Phase 6A Trim smoke test.

## 1. Build and install

```bash
./gradlew :app:testDebugUnitTest \
  :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true
```

Install the ARM64 debug APK and confirm the app launches on `Home` with the
bottom navigation labels `Home`, `Editor`, and `Settings`.

## 2. Destination behavior

1. On Home, tap **Import video** and choose a source.
2. Confirm the app opens Editor and shows preparation/probe progress there.
3. After probe completes, return Home and confirm the active project card shows
   the filename, duration, resolution, and codec.
4. Tap **Continue editing** and confirm the same source, preview, metadata, and
   Trim selection remain available in Editor.
5. Open Settings and confirm it shows only current on-device processing and
   FFmpeg/native engine diagnostics—no unimplemented editing switches.
6. Press Android Back from Settings and from Editor; each must return to Home.
7. Press Back from Home and confirm normal app exit behavior.

## 3. Lifecycle and render safety

- Select each destination, rotate/recreate the Activity, and confirm the selected
  destination is restored.
- Start preview playback, leave Editor, and confirm playback pauses.
- Start a 720p render, switch Home → Settings → Editor, and confirm render
  progress continues and no incomplete-output cleanup is triggered by tab
  switching.
- While rendering, confirm choosing another source is blocked until the render
  finishes or is explicitly cancelled.

## 4. Phase 6A regression

1. Select a non-full Trim range of at least one second.
2. Render 720p and play the output once.
3. Confirm the 1080p action unlocks, then render it without changing Trim.
4. Change Trim and confirm the prior 1080p unlock is invalidated.
5. Run the duration, A/V sync, and cancellation checks in
   `docs/PHASE6A_ANDROIDIDE_VERIFICATION.md`.

Do not begin exposing Phase 6B controls until the navigation and Phase 6A
regression checks pass on the target device.
