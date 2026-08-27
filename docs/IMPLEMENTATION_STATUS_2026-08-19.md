# RecapFlowAI Android — implementation status

- **Status date:** 2026-08-19
- **Target device:** ARM64 Mi Pad / AndroidIDE-RV2
- **Verification level:** Phase 4 build/runtime/media probe verified on the target Mi Pad
- **Current gate:** Phase 5 local-render source complete; AndroidIDE/device verification pending

## Completed work

### Native Android baseline

- Native Kotlin/XML/ViewBinding application preserved; no WebView migration.
- Application ID and namespace remain `com.recapflow.ai`.
- Initial delivery remains restricted to `arm64-v8a`.
- The native shared-library contract remains `System.loadLibrary("flowai")`.
- The project builds with Java 17, Gradle 9.0.0, AGP 8.13.0, Kotlin 2.1.0,
  NDK `24.0.8215888`, and CMake `3.18.1` on the Android-hosted development
  environment.

### Clean JNI bridge

- Direct media JNI access was moved out of `MainActivity` into
  `NativeMediaBridge`.
- The generic native entry was replaced by `recapflow_jni.cpp`.
- `MediaEngine`, structured native result/error types, and Android native
  logging helpers were added.
- `NativeMediaBridge → recapflow_jni.cpp → MediaEngine` was verified on-device.

### FFmpeg ARM64 foundation

- FFmpeg `9.0.1` was built locally from source with NDK `24.0.8215888`.
- The following static archives were produced for `arm64-v8a`:
  `libavcodec.a`, `libavfilter.a`, `libavformat.a`, `libavutil.a`,
  `libswresample.a`, and `libswscale.a`.
- The build is static/PIC, enables JNI and MediaCodec, disables programs,
  documentation, networking, GPL, nonfree components, and automatic external
  library detection.
- FFmpeg headers and archives are validated by CMake before linking.
- Static FFmpeg archives are linked into `libflowai.so` as one linker group.
- The native linker is configured for 16 KB maximum/common page alignment;
  formal package-level page-size validation remains a release-hardening task.
- FFmpeg configuration and licensing notes are recorded in the repository.

### AndroidIDE build compatibility fixes

- NDK r29 was rejected because its installed host compiler was x86-64 and could
  not execute on the ARM64 Android development host.
- NDK r24 is the verified project baseline.
- CMake native staging was moved to internal app storage to avoid shared-storage
  timestamp/rebuild problems.
- FFmpeg source helper permissions are normalized by the build script.
- The script handles FFmpeg releases that no longer expose `libpostproc`.
- AndroidIDE's ARM64 Build Tools `35.0.1` AAPT2 is selected with
  `android.aapt2FromMavenOverride`; this avoids launching Maven's incompatible
  Linux host AAPT2 binary.

### Phase 4 UI, import, and probe source

- The diagnostics-only screen was replaced with a Material 3 product flow for
  engine checking, empty, picking, preparing, probing, ready, and error states.
- Compact and `sw600dp` two-pane layouts share the same ViewBinding contract.
- Android's `OpenDocument` picker is restricted to `video/*`; the app does not
  request broad storage access.
- Selected content is copied on a worker thread to an app-owned cache workspace
  with UUID filenames, a partial-file commit, free-space checks, progress, and
  cancellation cleanup.
- `MediaImportCoordinator` owns async state, retry behavior, activity-state
  restoration, and replacement of old working copies only after a new probe
  succeeds.
- The JNI/media-engine boundary now exposes a typed FFmpeg probe instead of
  console text. It extracts duration, dimensions, rotation, frame rate, video
  and audio codecs, sample rate, channels, bitrate, and container format.
- Ready UI includes a native preview, essential metadata, expandable technical
  details, safe diagnostics copy, and a disabled Phase 5 render action.
- The metadata-row style explicitly opts out of dotted-name parent inference,
  fixing the AndroidIDE AAPT error for the nonexistent `Widget.RecapFlow`
  resource.
- FFmpeg static archives are linked into `libflowai.so` with `-Wl,-Bsymbolic`.
  This locally binds AArch64 assembly lookup-table references that become live
  when the Phase 4 probe pulls codec/DSP objects from the archives, preventing
  LLD's `R_AARCH64_ADR_PREL_PG_HI21` interposition/PIC failure.
- A real 47-second portrait MP4 was selected on the target device, copied into
  the app workspace, previewed, and probed successfully. The ready UI reported
  720×1280, 30 fps, MP4, H.264, and the linked FFmpeg 9.0.1 runtime.
- `READ_EXTERNAL_STORAGE` is now limited to `maxSdkVersion=28`. Android 6–9
  requests it immediately before import when absent; denial still falls back to
  `ACTION_OPEN_DOCUMENT`. Android 10+ keeps the permission-free picker flow and
  the app does not request `READ_MEDIA_VIDEO`.

### Phase 5 local render source

- Added Media3 Transformer `1.8.0` as the MediaCodec/OpenGL render backend while
  preserving FFmpeg 9.0.1 as the native probe foundation.
- The dependency is intentionally pinned below Media3 `1.9`, which raised the
  library minimum SDK to 23; the app remains installable at `minSdk 21`, while
  render is explicitly unavailable on API 21–22.
- Runtime capability detection requires H.264 and AAC encoders before enabling
  the test action.
- The 720p and 1080p presets target a 720/1080-pixel short side and cap output
  at 30 fps through an explicit frame-processing graph, ensuring the 720p test
  exercises decode, OpenGL processing, and encode rather than merely remuxing.
- Output is H.264 + AAC MP4 in the app-specific Movies directory; no broad
  storage permission or VPS is used.
- Typed preparing, rendering, finalizing, completed, failed, and cancelled
  states expose progress, elapsed time, source duration, realtime factor,
  output size, encoder name, and destination.
- Cancellation requires confirmation and deletes only the unique incomplete
  output. The imported source and previously completed exports are preserved.
- The 1080p action remains locked until the completed 720p file opens in the
  in-app player.

## Verification evidence

The following command completed successfully on the target device:

```bash
AAPT2_BIN="$HOME/android-sdk/build-tools/35.0.1/aapt2"

bash ./gradlew :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true \
  -Pandroid.aapt2FromMavenOverride="$AAPT2_BIN" \
  --no-daemon \
  --max-workers=2 \
  --stacktrace
```

Observed result:

```text
BUILD SUCCESSFUL in 1m 52s
42 actionable tasks: 28 executed, 14 up-to-date
```

The installed app launched and displayed:

```text
Native bridge READY
RecapFlow Native 0.1.0 / FFmpeg 9.0.1
```

This proves APK compilation/packaging, ARM64 native linking, `libflowai.so`
loading, JNI resolution, and an FFmpeg `av_version_info()` runtime call.

## Non-blocking warnings

- `android.aapt2FromMavenOverride` is reported by AGP as experimental.
- AndroidIDE reports its command-line tools package in a nonstandard location.
- NDK r24 reports limited compiler-attribute detection when used with CMake
  3.18.1.

These warnings did not stop compilation, packaging, installation, launch, JNI,
or the FFmpeg version smoke test. Keep the verified NDK/CMake baseline until a
separate toolchain migration is explicitly tested.

## Not yet verified

- APK/ELF contents have not yet received a separate package inspection pass.
- FFmpeg configuration and encoder enumeration are not yet displayed by the UI.
- Android 6–9 permission grant, denial, and OEM document-provider behavior need
  compatibility-device verification.
- Phone portrait/landscape, light mode, large text, picker
  cancellation, low-storage recovery, and process recreation need device QA.
- The Phase 5 source has not yet been compiled in AndroidIDE or media-verified
  on the Mi Pad; 720p and 1080p output remain acceptance-test items.
- Audio/video synchronization, cancellation, lifecycle recovery, and thermal
  behavior remain untested.

## Current verification handoff

Build and exercise the implemented vertical slice without expanding into the
full AI or ATS workflow:

```text
Engine check / Home
    ↓
Select one MP4 with Android's document picker
    ↓
Copy safely into app working storage for the proof of concept
    ↓
Probe locally with FFmpeg
    ↓
Show preview and structured metadata
    ↓
Run the 720p local render and open its output
    ↓
Verify A/V sync, then run the unlocked 1080p test
```

The detailed interaction and responsive-layout rules are in
[`UI_UX_PLAN.md`](UI_UX_PLAN.md).

Exact AndroidIDE build, prebuilt-library transfer, and device test steps are in
[`PHASE4_IMPORT_PROBE_ANDROIDIDE.md`](PHASE4_IMPORT_PROBE_ANDROIDIDE.md).

Phase 5 build and media acceptance steps are in
[`PHASE5_LOCAL_RENDER_ANDROIDIDE.md`](PHASE5_LOCAL_RENDER_ANDROIDIDE.md).
