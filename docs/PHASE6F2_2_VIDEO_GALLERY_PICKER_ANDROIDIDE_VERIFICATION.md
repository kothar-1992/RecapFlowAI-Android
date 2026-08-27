# Phase 6F.2.2 AndroidIDE and device verification

## Build identity

1. Open `RecapFlowAI_Phase6F2_2` in AndroidIDE.
2. Confirm version `1.0-phase6f2.2`, minSdk 28, targetSdk 34, Activity KTX 1.10.1,
   Media3 1.10.0, and ARM64.
3. Run:

   ```bash
   bash scripts/verify_phase6f2_2_source.sh
   ./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true
   ```

4. Install the debug APK and confirm Home shows `Phase 6F.2.2`.

## Primary owner-device flow

1. On Home, tap **Import video**.
2. Confirm a system media screen with video thumbnails opens, not the folder-first `Open from`
   DocumentsUI screen shown in the issue screenshot.
3. Confirm images are not selectable.
4. Select a recent MP4 and confirm Preparing → Analyzing → Editor completes.
5. Confirm filename, preview, duration, dimensions, codec, audio, and bitrate are populated.
6. Return Home, tap **Choose another video**, and confirm the same video gallery opens.
7. Close the picker without selecting and confirm no source is replaced.

## Compatibility flow

1. On an API 28/29 device with current Google Play services, confirm the backported Photo Picker is
   offered after its module becomes available.
2. On a device/emulator without a Photo Picker implementation, confirm AndroidX falls back to
   DocumentsUI and still filters for `video/*`.
3. Confirm denying the API 28 compatibility permission still permits the private picker fallback.
4. Confirm Android 13+ does not request whole-library `READ_MEDIA_VIDEO` access.

## Regression smoke matrix

- Replace the active source and confirm the previous source workspace is cleaned safely.
- Import H.264 and HEVC samples and play both.
- Verify Blur strength 30 has no repeated subtitle ghosts.
- Verify saved editor settings restore/reset.
- Run one 720p render, open it, publish it to Gallery, and confirm Share/Open.
- Cancel a render and confirm the source plus completed outputs remain intact.
