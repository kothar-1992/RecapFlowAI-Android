# Phase 5 AndroidIDE verification — first local MP4 render

This guide verifies the Phase 5 720p/1080p MediaCodec render slice on the ARM64
Mi Pad without starting Phase 6 editing features.

## What this gate adds

- FFmpeg 9.0.1 still probes the app-owned source copy.
- Media3 Transformer 1.8.0 uses Android MediaCodec and OpenGL to decode,
  scale/process, and encode locally.
- The first output targets a 720-pixel short side, at most 30 fps, H.264 video,
  and AAC audio in MP4.
- Opening the completed 720p output unlocks the 1080p test.

Media3 1.8.0 is deliberate: Media3 1.9+ requires minSdk 23, while this project
keeps its existing minSdk 21 installation contract. Rendering is runtime-gated
to Android 6.0/API 23 and newer.

## 1. Preserve FFmpeg prebuilts

As with Phase 4, a source-only delivery does not package generated archives.
Copy the verified directory into the Phase 5 project if necessary:

```bash
mkdir -p app/src/main/cpp/ffmpeg

cp -R \
  /storage/emulated/0/AndroidIDEProjects/RecapFlowAI_Phase4/app/src/main/cpp/ffmpeg/prebuilt \
  app/src/main/cpp/ffmpeg/
```

Verify the header and all six archives:

```bash
test -f app/src/main/cpp/ffmpeg/prebuilt/arm64-v8a/include/libavformat/avformat.h
ls -lh app/src/main/cpp/ffmpeg/prebuilt/arm64-v8a/lib/*.a
```

## 2. Resolve the new AndroidX dependencies

The first Phase 5 build needs Google Maven access to download these matching
artifacts once:

```text
androidx.media3:media3-common:1.8.0
androidx.media3:media3-effect:1.8.0
androidx.media3:media3-transformer:1.8.0
```

After Gradle caches them, later builds can reuse the local cache.

## 3. Build with the verified AndroidIDE settings

```bash
AAPT2_BIN="$HOME/android-sdk/build-tools/35.0.1/aapt2"

bash ./gradlew :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true \
  -Pandroid.aapt2FromMavenOverride="$AAPT2_BIN" \
  --no-daemon \
  --max-workers=2 \
  --stacktrace
```

Do not enable Gradle offline mode for the first Phase 5 build unless Media3
1.8.0 is already cached.

## 4. Run the 720p acceptance test

1. Install and launch the debug APK.
2. Import an MP4 with both video and audio.
3. Confirm FFmpeg probe completion and source preview.
4. Tap **720p test render**.
5. Confirm the UI moves through preparing, rendering, and finalizing without
   freezing.
6. Confirm progress, elapsed time, and the private output path update.
7. Tap **Play rendered video** after completion.
8. Confirm playback starts, image orientation is correct, audio is present,
   and lip sync/drift is acceptable through the end.
9. Confirm the 1080p action unlocks only after the 720p output opens.

Record the completed card's elapsed time, source duration, realtime factor,
output size, and actual encoder name.

## 5. Verify cancellation safety

1. Start a render from a sufficiently long source.
2. Tap **Cancel render**.
3. In the confirmation dialog, first choose **Keep rendering** and confirm the
   job continues.
4. Open the dialog again and choose **Cancel and delete**.
5. Confirm the state becomes cancelled, the source preview remains usable, and
   only the incomplete output path has been removed.

## 6. Run the 1080p test

After 720p playback has unlocked the action:

1. Tap **1080p test render**.
2. Repeat output playback, orientation, audio, and end-of-file sync checks.
3. Observe device temperature and stability.
4. If a codec error occurs, copy the visible codec/error diagnostics before
   retrying with another source.

Phase 6 must remain blocked until the Mi Pad produces and plays a valid 720p
MP4 with acceptable A/V sync. The 1080p result determines the safe default
export ceiling for this device.
