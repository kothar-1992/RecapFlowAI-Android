# Phase 6A AndroidIDE Verification

Use the same Android-hosted NDK/CMake and FFmpeg prebuilt setup that passed
Phase 5. The source-only archive does not duplicate large prebuilt libraries.

## 1. Build and unit test

```bash
./gradlew :app:testDebugUnitTest \
  :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true
```

Expected:

- `EditPlanValidatorTest` passes.
- ViewBinding generates for compact and `sw600dp` layouts.
- The ARM64 APK packages `libflowai.so` with the Phase 5 FFmpeg runtime.

## 2. Trim smoke test

1. Import a source with audio that is at least 10 seconds long.
2. In Review Editor → Clips/Trim, select `00:02..00:07`.
3. Confirm the UI shows a 5-second selected duration.
4. Render trimmed 720p.
5. Play the output once; confirm playback starts near source second 2 and ends
   near source second 7.
6. Confirm the 1080p action unlocks, then render it without changing Trim.

## 3. Validate output with FFmpeg

Probe each private output path shown in the app:

```bash
ffprobe -v error \
  -show_entries format=duration \
  -show_entries stream=index,codec_type,codec_name,start_time,duration \
  -of json \
  /path/to/RecapFlow_720p_YYYYMMDD_HHMMSS.mp4
```

Pass conditions:

- Output has H.264 video and AAC audio.
- Duration is within `max(100 ms, 3 frames)` of 5 seconds.
- Audio/video start timestamps differ by at most 100 ms.
- Output opens in Android and audio remains synchronized through the end.

## 4. State and cleanup checks

- Rotate/recreate the Activity and confirm the selected Trim remains.
- After a completed 720p render, change Trim and confirm the 1080p unlock is
  invalidated until the new 720p output is played.
- Start a render, cancel it, and confirm the partial MP4 is removed while the
  imported source remains playable.
- Reset Trim and confirm the selection returns to the full source duration.
