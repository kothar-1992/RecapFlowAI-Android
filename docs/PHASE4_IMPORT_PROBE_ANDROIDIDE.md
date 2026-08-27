# Phase 4 AndroidIDE verification — local MP4 import and probe

This guide verifies the Phase 4 source on the same ARM64 AndroidIDE environment
that already built RecapFlowAI with NDK `24.0.8215888` and FFmpeg `9.0.1`.

Use the corrected `v4` delivery or later. It includes both the explicit
style-parent fix required by AAPT for `Widget.RecapFlow.MetadataRow` and the
AArch64 FFmpeg static-archive symbol-binding fix required by the probe link. It
also adds an Android 6–9-only media permission request while preserving the
permission-free system picker on Android 10 and newer.

## 1. Preserve the generated FFmpeg prebuilts

The source delivery intentionally does not package generated FFmpeg archives.
Before replacing the old project, keep this directory:

```text
app/src/main/cpp/ffmpeg/prebuilt/arm64-v8a/
├── include/
└── lib/
```

If the Phase 4 project is extracted as `RecapFlowAI_Phase4`, copy the verified
prebuilts from the previous project:

```bash
mkdir -p \
  /storage/emulated/0/AndroidIDEProjects/RecapFlowAI_Phase4/app/src/main/cpp/ffmpeg

cp -R \
  /storage/emulated/0/AndroidIDEProjects/RecapFlowAI_Codex2/app/src/main/cpp/ffmpeg/prebuilt \
  /storage/emulated/0/AndroidIDEProjects/RecapFlowAI_Phase4/app/src/main/cpp/ffmpeg/
```

Confirm that headers and all six archives exist:

```bash
test -f app/src/main/cpp/ffmpeg/prebuilt/arm64-v8a/include/libavformat/avformat.h
ls -lh app/src/main/cpp/ffmpeg/prebuilt/arm64-v8a/lib/*.a
```

## 2. Build with the verified AndroidIDE toolchain

Run from the Phase 4 project root:

```bash
AAPT2_BIN="$HOME/android-sdk/build-tools/35.0.1/aapt2"

bash ./gradlew :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true \
  -Pandroid.aapt2FromMavenOverride="$AAPT2_BIN" \
  --no-daemon \
  --max-workers=2 \
  --stacktrace
```

Expected final task/result:

```text
> Task :app:assembleDebug
BUILD SUCCESSFUL
```

The debug APK is normally written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 3. Verify a real MP4

1. Install and launch the debug APK.
2. Confirm the engine card shows `RecapFlow Native 0.1.0 / FFmpeg 9.0.1`.
3. Tap **Import video** and choose one local MP4 with the system picker.
4. Confirm **Preparing video** shows copy progress without freezing the UI.
5. Confirm **Analyzing video** transitions to the review screen.
6. Compare duration, resolution, orientation, frame rate, codecs, audio,
   bitrate, container, and file size against a known source.
7. Play or seek the native preview.
8. Expand technical details and confirm no private source path is displayed.
9. Tap **Choose another video**, cancel the picker, and confirm the previous
   ready video remains intact.
10. Rotate the device and repeat on a phone-width and `sw600dp` tablet-width
    layout if both are available.

The **Continue to 720p test** action must remain disabled in Phase 4. Rendering,
AI, ATS, timeline, and subtitle work are intentionally outside this gate.

## 4. Evidence to retain

- Full Gradle build output.
- Empty, preparing, probing, ready, and recoverable-error screenshots.
- Selected MP4's expected metadata and the values shown by RecapFlowAI.
- Device model, Android version, orientation, theme, and font-scale used.

If the build fails, preserve the first `Caused by:` section and the first CMake,
Kotlin, AAPT2, or linker error rather than only the final Gradle stack frames.
