# Phase 6A.2 Sticky Editor AndroidIDE Verification

Use the same AndroidIDE/NDK/CMake/FFmpeg environment that built the three-tab
Phase 6A.1 package.

## 1. Build and launch

```bash
./gradlew :app:testDebugUnitTest \
  :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true
```

Install the ARM64 debug APK, import a portrait video, and open Editor.

## 2. Layout and interaction

1. Confirm the preview is centered at the top rather than placed beside Video
   details.
2. Confirm the first metadata card shows a one-line filename and compact
   duration/resolution/codec summary.
3. Swipe upward. Metadata, Trim, and render cards must move underneath the
   pinned preview while the preview remains in place.
4. Tap **Video details** and confirm duration, resolution, orientation, frame
   rate, container, video codec, audio, bitrate, and file size appear.
5. Tap **Hide video details** and confirm the sheet returns to its compact state.
6. Confirm preview playback controls remain usable and leaving Editor pauses
   playback.

## 3. Responsive checks

- Repeat on the Mi Pad/tablet layout and one compact phone-sized device or
  emulator.
- Rotate/recreate the Activity and confirm the selected destination, imported
  source, and Trim range restore without duplicate preview surfaces.
- Check dark mode and large text for clipped filename, controls, or inaccessible
  sheet content.

## 4. Media regression

1. Select a non-full Trim range and render 720p.
2. Switch Home → Settings → Editor while rendering; the job must continue.
3. Play the completed output and confirm the 1080p action unlocks.
4. Complete the remaining duration, A/V sync, and cancellation checks in
   `docs/PHASE6A_ANDROIDIDE_VERIFICATION.md`.

The gate passes only when the new layout behavior and the existing Trim/render
workflow both pass on the target device.
