# Recap Flow AI Android — Implementation Plan

- **Project:** RecapFlowAI Android
- **Current source:** `RecapFlowAI_Phase6F2_7` (CompositionPlayer live-preview feature gate on the verified Phase 6F.2.6.2 baseline)
- **Planning date:** 2026-08-18
- **Last updated:** 2026-08-28
- **Primary target:** Modern ARM64 Android tablets/phones, beginning with Mi Pad
- **Development environment:** Android Code Studio on Android
- **UI strategy:** Native Kotlin + XML + ViewBinding
- **Media strategy:** Native FFmpeg through JNI/CMake, with Android MediaCodec capability where appropriate
- **Server strategy:** No VPS dependency for core video processing
- **Current gate:** Phase 6F.2.7 SOURCE IMPLEMENTED — AndroidIDE/device verification pending before merge
- **GitHub repository:** [`kothar-1992/RecapFlowAI-Android`](https://github.com/kothar-1992/RecapFlowAI-Android)
- **Current GitHub task:** [#2 Phase 6F.2.7 CompositionPlayer live preview with explicit fallback](https://github.com/kothar-1992/RecapFlowAI-Android/issues/2)

---

## 1. Baseline Assessment

The uploaded project is a valid Native C++ Android baseline and already contains the correct insertion points for the media engine.

### Current build configuration

| Item | Current value |
|---|---|
| Application ID | `com.recapflow.ai` |
| Namespace | `com.recapflow.ai` |
| AGP | `8.13.0` |
| Gradle | `9.0.0` |
| Kotlin | `2.1.0` |
| AndroidX Core KTX | `1.16.0` |
| AndroidX AppCompat | `1.7.1` |
| AndroidX Activity KTX | `1.10.1` |
| Material Components | `1.13.0` |
| ConstraintLayout | `2.1.4` |
| AndroidX Media3 | `1.10.0` |
| Java/JVM target | `17` |
| compileSdk | `36` |
| targetSdk | `34` |
| minSdk | `28` (Android 9) |
| NDK | `24.0.8215888` (verified Android-hosted toolchain) |
| CMake | `3.18.1` (verified; emits a non-blocking compiler-detection warning) |
| ViewBinding | Enabled |
| Native library | `flowai` |
| C++ entry file | `recapflow_jni.cpp` |
| Native build | CMake |
| Current ABI restriction | `arm64-v8a` |
| Current UI | XML + ConstraintLayout |

### Verified device baseline — updated 2026-08-19

The Mi Pad / Android Code Studio build log and runtime screenshot prove:

- `:app:assembleDebug` completed successfully in 25 seconds.
- NDK `24.0.8215888` compiled the `arm64-v8a` C++ target.
- CMake `3.18.1` configured and built `libflowai.so`.
- The APK packaged, installed, and launched on the target Android device.
- `System.loadLibrary("flowai")` and `nativeVersionFromJni()` resolved at runtime.
- The UI displayed `Native bridge READY` and `RecapFlow Native 0.1.0`.

The FFmpeg-enabled device build subsequently proved:

- FFmpeg `9.0.1` produced all six required ARM64 static archives.
- `:app:assembleDebug -Precapflow.ffmpeg.enabled=true` completed successfully
  in 1 minute 52 seconds.
- AndroidIDE Build Tools `35.0.1` supplied the working ARM64 AAPT2 through
  `android.aapt2FromMavenOverride`.
- `libflowai.so` linked the static FFmpeg libraries and loaded in the installed
  APK without `UnsatisfiedLinkError`.
- The UI displayed `RecapFlow Native 0.1.0 / FFmpeg 9.0.1`, proving the native
  `av_version_info()` call resolved at runtime.

The detailed evidence and remaining verification gaps are recorded in
[`docs/IMPLEMENTATION_STATUS_2026-08-19.md`](docs/IMPLEMENTATION_STATUS_2026-08-19.md).

NDK r29 is not the current baseline because the installed compiler was an
x86-64 host executable and could not run on the ARM64 Android development host.

### JNI path already proven

Current runtime path:

```text
MainActivity.kt
    ↓
NativeMediaBridge.nativeVersion()
    ↓
System.loadLibrary("flowai")
    ↓
nativeVersionFromJni()
    ↓
recapflow_jni.cpp → MediaEngine
    ↓
"RecapFlow Native 0.1.0"
```

The successful app launch confirms that the Kotlin → JNI → C++ → Android runtime path is already operational.

---

## 2. Architecture Decision

Do **not** rebuild the app as WebView.

The Android version will be a native application:

```text
Recap Flow AI Android
│
├── Native UI
│   ├── Kotlin
│   ├── XML
│   └── ViewBinding
│
├── Application Layer
│   ├── Project state
│   ├── Timeline/Edit plan
│   ├── Export presets
│   ├── Job state
│   └── AI orchestration
│
├── Native Media Bridge
│   ├── Kotlin NativeMediaBridge
│   ├── JNI
│   └── C/C++
│
├── FFmpeg Engine
│   ├── libavformat
│   ├── libavcodec
│   ├── libavfilter
│   ├── libavutil
│   ├── libswscale
│   └── libswresample
│
├── Android Hardware Media
│   └── MediaCodec when supported
│
├── AI Services
│   ├── Transcription
│   ├── Gemini/script planning
│   └── TTS
│
└── Local Storage
    ├── Projects
    ├── Working/cache files
    └── Final exports
```

### Core rule

**Video processing must remain local.**

Cloud/API access is reserved for tasks that actually require AI.

---

## 3. Scope Control

The project must be developed in gates. Do not attempt the full Recap Flow AI workflow before the media engine proves stable.

### Initial non-goals

Do not add these during the FFmpeg proof-of-concept:

- Full production UI
- Authentication
- Account system
- Database synchronization
- VPS processing
- Multi-user features
- Thumbnail generation
- Full ATS timeline
- Full narrator workflow
- Complex subtitle engine
- Background queue system
- Multiple ABI support

These come only after local rendering succeeds.

### UI/UX delivery rule

UI work begins now as a narrow vertical slice supporting the current media
gate, not as a placeholder implementation of the whole product. The first
experience is limited to Home → Import → Preparing/Probe → Video Review. Render
progress and result UI follow only when the render engine is ready.

The responsive screen specifications, state models, accessibility rules, and
acceptance checklist are maintained in
[`docs/UI_UX_PLAN.md`](docs/UI_UX_PLAN.md).

---

# PHASE 0 — Freeze the Working Baseline

## Goal

Preserve the currently working project before FFmpeg integration.

## Tasks

- [x] Keep the current launchable APK as the baseline.
- [ ] Create a Git commit before modifying native code.
- [x] Record current Gradle/AGP/Kotlin/NDK/CMake versions.
- [x] Keep `MainActivity → JNI → flowai` smoke test working.
- [x] Add `local.properties` and IDE-local files to `.gitignore` if needed.
- [x] Do not include API keys, signing secrets, or credentials in source.

## Recommended baseline commit

```text
chore: initialize RecapFlowAI native Android baseline
```

## Definition of done

The unmodified baseline can always be restored and rebuilt in Android Code Studio.

---

# PHASE 1 — Clean Native Bridge

## Goal

Separate UI code from native media code before FFmpeg is introduced.

## Proposed structure

```text
app/src/main/
├── kotlin/com/recapflow/ai/
│   ├── MainActivity.kt
│   └── media/
│       └── NativeMediaBridge.kt
│
└── cpp/
    ├── CMakeLists.txt
    ├── recapflow_jni.cpp
    ├── media/
    │   ├── media_engine.cpp
    │   └── media_engine.h
    └── utils/
        ├── native_log.cpp
        └── native_log.h
```

## Tasks

- [x] Move direct JNI calls out of `MainActivity`.
- [x] Create `NativeMediaBridge`.
- [x] Rename the generic native entry file to a RecapFlow-specific name.
- [x] Add native logging helpers.
- [x] Add structured native result/error codes.
- [x] Add `nativeVersion()` smoke test.
- [x] Preserve the existing `flowai` shared-library name initially.

## Definition of done

UI code no longer contains media-engine implementation details.

---

# PHASE 2 — FFmpeg ARM64 Build

## Goal

Produce FFmpeg libraries compatible with this Android project and the verified
Android-hosted NDK r24 toolchain.

## Initial ABI

```text
arm64-v8a
```

Only ARM64 is required for the first working version.

## Initial FFmpeg components

Required:

```text
libavutil
libavcodec
libavformat
libavfilter
libswscale
libswresample
```

Optional later:

```text
libavdevice
postproc
```

## Build principles

- [x] Build FFmpeg against the same Android NDK family used by the app.
- [x] Start with ARM64 only.
- [x] Prefer the smallest feature set needed by Recap Flow AI for this gate.
- [x] Enable Android/JNI support.
- [x] Enable MediaCodec integration where supported.
- [x] Avoid unnecessary network protocols in the media library.
- [x] Keep licensing configuration documented.
- [ ] Verify every packaged `.so` is compatible with modern Android page-size requirements.
- [x] Record FFmpeg configure flags in source control.

### Current Phase 2 implementation status

- The Gradle flag `recapflow.ffmpeg.enabled` controls the FFmpeg link gate.
- `scripts/build_ffmpeg_android_arm64.sh` builds the six required static
  libraries with NDK `24.0.8215888` and installs them into the expected layout.
- CMake validates every required header/archive before attempting the link.
- FFmpeg is disabled by default until those generated artifacts exist, preserving
  the verified Phase 1 build.
- First device attempt stopped before FFmpeg configuration because GNU Make was
  not installed. The handoff now records `pkg install make` as a prerequisite.
- The CMake module now resolves generated artifacts relative to the function
  definition with `CMAKE_CURRENT_FUNCTION_LIST_DIR`, preventing the former
  one-directory-short `cpp/prebuilt` lookup.
- FFmpeg configuration is version-adaptive for the removal of `libpostproc`;
  releases without that component no longer receive `--disable-postproc`.
- The build script normalizes executable permission for FFmpeg shell helpers
  after Android shared-storage copies and no longer passes the invalid
  `--pkg-config=false` placeholder.
- FFmpeg `9.0.1` successfully generated the six required ARM64 archives on the
  target development device.
- The enabled FFmpeg link build, APK installation, launch, loader resolution,
  and runtime version call are verified.
- Phase 4 probe symbols pull additional AArch64 FFmpeg assembly into the final
  shared object. The CMake integration now links `libflowai.so` with
  `-Wl,-Bsymbolic` so those internal table references bind locally instead of
  failing LLD's PIC/interposition check. The corrected FFmpeg-enabled build and
  Phase 4 probe were verified on the target device.
- The remaining Phase 2 hardening item is formal APK/ELF page-size inspection
  before release; linker alignment flags are already present.

## Implemented packaging layout

```text
app/src/main/
└── cpp/ffmpeg/prebuilt/arm64-v8a/
    ├── include/...
    └── lib/
        ├── libavutil.a
        ├── libavcodec.a
        ├── libavformat.a
        ├── libavfilter.a
        ├── libswscale.a
        └── libswresample.a
```

Only `libflowai.so` is packaged as the application native library. The FFmpeg
archives are linked statically into it. Public distribution requires the
documented LGPL/static-link compliance review.

## Definition of done

`libflowai.so` links successfully against FFmpeg and the APK launches without native-loader errors.

---

# PHASE 3 — FFmpeg JNI Smoke Test

## Goal

Prove that FFmpeg is callable from Kotlin.

## Required JNI calls

```text
getFfmpegVersion()
getFfmpegConfiguration()
getAvailableVideoEncoders()
```

## Current Phase 3 status

- [x] Return the linked FFmpeg runtime version through JNI.
- [x] Display `FFmpeg 9.0.1` on the temporary diagnostics UI.
- [x] Verify the version call after installation on the target device.
- [ ] Expose the complete FFmpeg configuration through a typed diagnostic call.
- [ ] Enumerate available video encoders and distinguish software from
  MediaCodec-backed capability.

The core smoke-test gate is complete. Configuration and encoder capability
diagnostics remain prerequisites for the first real render rather than blockers
for local import/probe.

## UI test

Temporary diagnostics page:

```text
FFmpeg Status: READY
FFmpeg Version: ...
ABI: arm64-v8a
Hardware Encoder: ...
```

## Definition of done

The app launches and displays the FFmpeg runtime version returned by native code.

---

# PHASE 4 — UI/UX Foundation + Local Video Import + Probe

## Goal

Replace the centered diagnostics screen with the first useful product flow,
select a video from Android storage, and inspect it locally.

## UX scope

```text
Home / Empty state
    ↓
System video picker
    ↓
Preparing working copy
    ↓
FFmpeg probe
    ↓
Video review + structured metadata
```

- [x] Add Material 3 design tokens and reusable status styles.
- [x] Add compact and tablet (`sw600dp`) layout variants.
- [x] Keep native/FFmpeg diagnostics available as secondary information.
- [x] Model empty, picking, preparing, probing, ready, and error states
  explicitly.
- [x] Keep unsupported AI, ATS, timeline, and subtitle destinations out of this
  gate.
- [ ] Verify the accessibility and responsive-layout acceptance criteria on
  phone/tablet, light/dark mode, and large text as described in
  [`docs/UI_UX_PLAN.md`](docs/UI_UX_PLAN.md).

## Android layer

Use Android's system Photo Picker for visual video selection. Keep the AndroidX automatic
`ACTION_OPEN_DOCUMENT` fallback instead of legacy raw-storage assumptions when Photo Picker is
unavailable.

## Initial safe workflow

```text
User selects video from the system gallery
    ↓
content:// URI
    ↓
RecapFlow file resolver
    ↓
Working project file / native-readable handle
    ↓
FFmpeg probe
```

For the first POC, copying the selected source into a project working directory is acceptable.

A later optimization can replace large file copies with file-descriptor/custom-I/O handling.

## Metadata to extract

- [x] Duration
- [x] Width
- [x] Height
- [x] Rotation
- [x] Frame rate
- [x] Video codec
- [x] Audio codec
- [x] Audio sample rate
- [x] Channel count
- [x] Bit rate
- [x] Container format

## Proposed model

```text
MediaInfo
├── uri
├── localPath/workHandle
├── durationMs
├── width
├── height
├── fps
├── rotation
├── videoCodec
├── audioCodec
├── audioSampleRate
├── audioChannels
├── bitrate
└── containerFormat
```

## Definition of done

A selected MP4 can be probed entirely on-device with no VPS.

The user can distinguish selection, preparation, probe success, and recoverable
failure without the app freezing or exposing raw FFmpeg command text.

**Implementation status (2026-08-19):** Source complete. Static resource/JNI
checks and the FFmpeg-disabled C++ fallback compile pass in the delivery
workspace. An FFmpeg-enabled AndroidIDE build selected and probed a real MP4 on
the target Mi Pad, displaying its preview and structured metadata in the
`sw600dp` two-pane UI. Legacy Android 6–9 permission behavior still needs a
separate compatibility-device check.

---

# PHASE 5 — First Real Render

## Goal

Generate a new MP4 locally.

Do not begin with complex RecapFlow processing.

**Implementation status (2026-08-19):** Source complete for the first render
vertical slice. FFmpeg remains the verified local probe/demux foundation while
Media3 Transformer `1.10.0` drives the Android MediaCodec/OpenGL render path.
The implementation runtime-detects H.264/AAC encoders, performs a real
frame-processing pass, targets a 720-pixel short side and 30 fps, emits typed
preparing/rendering/finalizing/completed/failed/cancelled states, shows progress
and metrics, confirms cancellation, and deletes only incomplete output. Opening
the completed 720p file in the in-app player unlocks the 1080p test. Phase 6E.3A
raises the application baseline to API 28 and Media3 `1.10.0`; API 21–27 are no
longer installation targets. AndroidIDE compilation and real Mi Pad
output/playback verification remain acceptance gates after dependency changes.

## POC operation

```text
Input MP4
    ↓
Scale / transcode
    ↓
H.264 + AAC MP4
    ↓
Output
```

## Render UX gate

- Begin with one clear `720p test` action after a successful probe.
- Show preparing, rendering, finalizing, completed, failed, and cancelled as
  distinct typed states.
- Show stage, progress, elapsed time, and output destination without blocking
  the UI thread.
- Confirm cancellation and remove only incomplete output.
- Unlock the 1080p test after the 720p output opens successfully on Android.

## Test presets

### Test A — lightweight

```text
720p
30 fps
H.264
AAC
```

### Test B — target

```text
1080p
30 fps
H.264
AAC
```

## Hardware strategy

Detect the device encoder at runtime.

Preferred target:

```text
H.264 MediaCodec
```

Hardware availability and behavior must be capability-tested per device.

Do not assume every Android device exposes identical encoder behavior.

## Metrics to record

- [x] Render duration captured by the completed render state/UI
- [x] Source duration carried from the FFmpeg probe
- [x] Realtime factor calculated as render duration / source duration
- [ ] Peak temperature observation
- [x] Output size captured from the export result/file
- [ ] Audio/video sync
- [ ] Device stability
- [ ] App memory
- [x] Cancel confirmation and incomplete-output cleanup implemented

## Definition of done

Mi Pad can render a valid locally generated MP4 that plays correctly.

---

# PHASE 6 — RecapFlow Basic Editing Engine

## Goal

Build deterministic local editing primitives from a typed, reproducible plan.

Implement and verify one operation at a time. Phase 5 is owner-confirmed complete;
Phase 6 must keep its working import, preview, cancellation, cleanup, 720p, and
1080p paths intact.

## Product-control rule

Editing behavior must be user-controlled on Android. Every optional feature must
have a typed disabled state, and disabling it must cause the render compiler to
omit that operation completely. The UI must retain the user's last configuration
when a feature is turned off so it can be restored if re-enabled.

Profiles:

- `NORMAL` — conservative defaults; optional transformative effects are off.
- `ADAPTIVE` — deterministic local rules may enable selected operations.
- `CUSTOM` — the user controls each implemented operation directly.

Do not expose a control merely because it exists in the plan model. A control is
shown only when its executor and verification gate are implemented.

## Delivery gates

### Phase 6E.3A — Platform and Media3 baseline stabilization (owner-confirmed done)

Adopt the version stack that the owner compiled successfully in AndroidIDE, without mixing
new editor behavior into the dependency migration:

- [x] Raise the application minimum from API 21 to API 28 (Android 9).
- [x] Use AGP `8.13.0`, Kotlin `2.1.0`, Core KTX `1.16.0`, AppCompat `1.7.1`,
  Material `1.13.0`, ConstraintLayout `2.1.4`, and Media3 `1.10.0`.
- [x] Retain Gradle `9.0.0`, Java/JVM `17`, compileSdk `36`, targetSdk `34`, NDK
  `24.0.8215888`, CMake `3.18.1`, and the `arm64-v8a` delivery baseline.
- [x] Remove the obsolete API 21–22 render capability branch while retaining the Android 9
  `READ_EXTERNAL_STORAGE` fallback for the legacy picker path.
- [x] Preserve the verified EditPlan, preview, FFmpeg probe, cancellation, 720p/1080p,
  slider-only source blur, and realtime logo-control behavior.
- [x] Keep source-blur and logo direct screen-touch editing disabled; this baseline change
  does not reopen the release-touch crash path.
- [x] Add a source preflight that verifies the exact dependency values and prevents the old
  Media3 `1.8.0` / minSdk 21 constraint from silently returning.
- [x] Record the owner's successful AndroidIDE build of the updated version stack.
- [x] Build this merged `1.0-phase6e3a` latest-source archive in AndroidIDE; owner
  confirmed Phase 6E.3A done on 2026-08-27.
- [ ] Run the import/probe, H.264 and HEVC preview, realtime blur/logo, Trim, transform,
  720p/1080p export, cancellation, and Activity recreation regression matrix on device.

This gate intentionally excludes realtime-preview session redesign, public Gallery export,
automatic editor-state persistence, image animation loops, Telegram delivery, Gemini, and
direct screen-touch effect editing. Task scope and device checks are in
[`docs/tasks/PHASE6E3A_PLATFORM_MEDIA3_BASELINE.md`](docs/tasks/PHASE6E3A_PLATFORM_MEDIA3_BASELINE.md)
and
[`docs/PHASE6E3A_PLATFORM_MEDIA3_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6E3A_PLATFORM_MEDIA3_ANDROIDIDE_VERIFICATION.md).

### Phase 6E.3B — Realtime preview session stability (owner-confirmed done)

After Phase 6E.3A builds and installs, make one retained preview session the owner of source
playback and live effects. This gate must not change encoded output semantics:

- [x] Add a preview-session state machine with source generations, latest-request coalescing,
  already-applied graph suppression, and stale-request rejection.
- [x] Debounce Transform/Trim effect-list changes while keeping one source `ExoPlayer` instance.
- [x] Move source-blur rectangle, strength, enable state, and active-time preview updates into a
  thread-safe per-frame shader snapshot, matching the existing realtime logo-state design.
- [x] Redraw paused frames without rebuilding the graph when only blur/logo live values change.
- [x] Keep the last valid frame visible during Media3 replacement with
  `keep_content_on_player_reset` and avoid media-item reloads for normal editor control changes.
- [x] Add a 10-second readiness deadline, one bounded no-effects fallback per session generation,
  and a terminal unavailable state so buffering cannot remain indefinite.
- [x] Keep adaptive candidate/sequence preview and replacement/mix audio on the retained player
  path; no render is required between editor controls.
- [x] Keep `forRender` immutable and preserve the reviewed EditPlan/export effect semantics.
- [x] Add unit tests for graph coalescing, duplicate suppression, bounded recovery, and realtime
  source-blur state replacement/disable behavior.
- [x] Add a Phase 6E.3B source preflight and AndroidIDE/device verification matrix.
- [x] Build/install `1.0-phase6e3b` in AndroidIDE; owner confirmed Phase 6E.3B done on
  2026-08-27.
- [x] Pass rapid paused/playing Blur, Logo, Transform, Trim, Audio, and Adaptive control changes on
  H.264 and HEVC sources without crash, black frame, stale effect, or indefinite loading.
- [x] Confirm 720p/1080p output parity and cancellation remain unchanged at the owner gate.

Task scope is in
[`docs/tasks/PHASE6E3B_REALTIME_PREVIEW_SESSION.md`](docs/tasks/PHASE6E3B_REALTIME_PREVIEW_SESSION.md)
and device checks are in
[`docs/PHASE6E3B_REALTIME_PREVIEW_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6E3B_REALTIME_PREVIEW_ANDROIDIDE_VERIFICATION.md).

### Phase 6F.1.1.1 — Media3 encoder-factory compile hotfix (owner-confirmed done)

The first Phase 6F.1.1 AndroidIDE compile proved that the resolved Media3 `1.10.0`
`DefaultEncoderFactory.Builder` does not expose `setEnableFormatFallback`. Correct only that API
compatibility error without weakening the render-quality request:

- [x] Remove the unavailable `setEnableFormatFallback(true)` call.
- [x] Retain supported encoder-settings fallback through `setEnableFallback(true)`.
- [x] Preserve `VideoEncoderSettings.setBitrate`, the 6/12 Mbps floors, source-aware caps,
  actual-bitrate reporting, output semantics, and public-export copy behavior.
- [x] Add a source-preflight rejection so the unavailable method cannot be reintroduced.
- [x] Give the retry a distinct `RecapFlowAI_Phase6F1_1_1` / `1.0-phase6f1.1.1` identity.
- [x] Compile and install `1.0-phase6f1.1.1` in AndroidIDE; owner confirmed Phase 6F.1.1 done.
- [ ] Continue the original-source 720p/1080p quality comparison after compilation succeeds.

### Phase 6F.1.1 — H.264 render-quality hotfix (owner-confirmed done; extended comparison pending)

After the owner confirmed Phase 6F.1, correct the owner-observed soft/blurry 720p and 1080p
exports before starting persistence:

- [x] Stop relying on an opaque device-default H.264 bitrate for reviewed exports.
- [x] Request at least 6 Mbps for 720p and 12 Mbps for 1080p through Media3
  `DefaultEncoderFactory` and `VideoEncoderSettings`.
- [x] Preserve a stronger probed source bitrate within conservative 12 Mbps (720p) and 24 Mbps
  (1080p) caps.
- [x] Keep supported encoder-settings fallback enabled so unsupported requested settings remain
  recoverable; do not call the unavailable Media3 `setEnableFormatFallback` API.
- [x] Capture and display `ExportResult.averageVideoBitrate` beside the requested target.
- [x] Warn when the source short side is smaller than the selected preset; higher output bitrate
  reduces new compression damage but cannot create missing source detail.
- [x] Warn when the input appears to be an earlier `RecapFlow_` export because repeated H.264
  generations compound quality loss.
- [x] Keep the immutable EditPlan, effects, audio, cancellation, private render, and byte-for-byte
  MediaStore publication behavior unchanged.
- [x] Add policy unit tests, source preflight, implementation status, and an AndroidIDE/device
  quality comparison matrix.
- [x] Build/install the compile-patched `1.0-phase6f1.1.1` in AndroidIDE; owner confirmed done.
- [ ] Render an original source at 720p and 1080p, record target/actual bitrate, dimensions,
  codec, file size, A/V sync, still-frame detail, and high-motion detail.
- [ ] Confirm Transform/Audio/Overlay parity, cancellation, Gallery/Open/Share, and private/public
  byte-count equality.

This gate does not add AI upscaling, sharpening, denoising, H.265, a software encoder, or
background rendering. Task scope is in
[`docs/tasks/PHASE6F1_1_RENDER_QUALITY_HOTFIX.md`](docs/tasks/PHASE6F1_1_RENDER_QUALITY_HOTFIX.md)
and device checks are in
[`docs/PHASE6F1_1_RENDER_QUALITY_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F1_1_RENDER_QUALITY_ANDROIDIDE_VERIFICATION.md).

### Phase 6F.1 — Public export destination (owner-confirmed done; extended matrix pending)

After preview stability passes, publish completed renders through MediaStore into a visible
Movies/RecapFlowAI or DCIM/RecapFlowAI collection on API 29+, with an API 28 legacy storage
path and permission flow. Keep partial files private until success, preserve cancellation
cleanup, and expose Share/Open only after MediaStore finalization.

- [x] Keep Media3 render output app-private until Transformer completion and file-size validation.
- [x] Publish API 29+ copies to `Movies/RecapFlowAI` with `MediaStore.IS_PENDING=1`, then clear
  pending only after a complete byte-for-byte copy.
- [x] Delete the incomplete MediaStore row when copy/finalization fails or is cancelled.
- [x] Add the API 28-only `WRITE_EXTERNAL_STORAGE` permission flow, hidden `.pending` copy,
  final rename, MediaScanner notification, and FileProvider URI.
- [x] Preserve every completed private render if public permission is denied or publishing fails.
- [x] Expose the fifth Review Editor `Export` tab now that its local executor exists.
- [x] Lock Open and Share until a finalized public URI exists; grant only temporary read access.
- [x] Add retry UI, collision-safe file names, pure name-policy tests, source preflight, and a
  target-device verification matrix.
- [x] Build/install `1.0-phase6f1` in AndroidIDE; owner confirmed Phase 6F.1 done on
  2026-08-27.
- [ ] Confirm Gallery visibility, Open/Share, denial/retry, low-storage failure, duplicate export,
  cancellation cleanup, source preservation, and 720p/1080p regressions on real devices.

Task scope is in
[`docs/tasks/PHASE6F1_PUBLIC_MEDIASTORE_EXPORT.md`](docs/tasks/PHASE6F1_PUBLIC_MEDIASTORE_EXPORT.md)
and device checks are in
[`docs/PHASE6F1_PUBLIC_EXPORT_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F1_PUBLIC_EXPORT_ANDROIDIDE_VERIFICATION.md).

### Phase 6F.2.6.1 — Final-render geometry + non-destructive live-preview hotfix (source implemented; AndroidIDE/device verification pending)

Correct the two owner-reported regressions without reintroducing per-tool renders:

- [x] Treat MP4 track rotation as part of output geometry validation. A portrait 1080p track encoded
  as `1920x1080 + rotation=90` now validates as display `1080x1920` instead of being deleted as a
  false quality mismatch.
- [x] Keep H.264 even-dimension validation on coded dimensions while exposing display dimensions to
  Completed/export UI and diagnostics.
- [x] Add pure validation coverage for rotated portrait 720p and exact portrait 1080p output.
- [x] Keep Clips, Transform, Audio, Overlay, and Export as independent EditPlan inputs; no control
  change calls Transformer or creates an intermediate video.
- [x] Make realtime effect-topology changes rebuild only the ExoPlayer decoder/GPU graph, preserving
  source position/play state and all saved edit settings. This is a preview reset, not a render.
- [x] Keep parameter-only live effects on the retained player and retain source-only fallback only
  when a clean graph rebuild is also rejected by the device.
- [x] Preserve the Phase 6F.2.6 single authoritative `EditPlan -> Media3 Composition -> Transformer`
  final-export path.
- [ ] Build/install with FFmpeg enabled in AndroidIDE and reproduce the owner sample that previously
  reported `Expected exact 1080x1920, but received 1920x1080`.
- [ ] Verify sequential editing without rendering: Clips -> Transform -> Audio -> Overlay -> Export,
  then perform exactly one final 720p render and one separate 1080p test render.

This hotfix does not claim that visual transforms make third-party footage legally safe to reuse;
rights/licensing and platform rules remain separate from the media-processing pipeline. Scope and
checks are in
[`docs/tasks/PHASE6F2_6_1_FINAL_RENDER_PREVIEW_HOTFIX.md`](docs/tasks/PHASE6F2_6_1_FINAL_RENDER_PREVIEW_HOTFIX.md)
and
[`docs/PHASE6F2_6_1_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_6_1_ANDROIDIDE_VERIFICATION.md).

### Phase 6F.2.7 — CompositionPlayer live preview with explicit fallback (source implemented; AndroidIDE/device verification pending)

Connect the verified shared `EditPlan -> Media3CompositionPlan` topology to Media3
`CompositionPlayer` without changing the one-pass final Transformer export:

- [x] Add build-time feature flag `recapflow.composition.preview.enabled` /
  `BuildConfig.ENABLE_COMPOSITION_PLAYER_PREVIEW`, defaulting to enabled for this device gate.
- [x] Add `Media3CompositionCompiler.compileForPreview(...)` so CompositionPlayer receives explicit
  pre-clipping source duration on every encoded source item while the verified Transformer export
  duration path remains unchanged.
- [x] Reuse the same Trim/Adaptive, Transform, Audio, Blur/Logo and planned-duration decisions from
  `Media3CompositionPlanCompiler`; preview does not reinterpret EditPlan topology independently.
- [x] Preserve semantic playhead position across Composition rebuilds with source/output timeline
  mapping that accounts for Adaptive ranges and Speed.
- [x] Keep aspect/Fit/Fill geometry changes behind the existing preview-surface settle boundary before
  rebuilding the CompositionPlayer graph.
- [x] Compile Replace/Mix audio into CompositionPlayer preview and disable the separate replacement
  ExoPlayer simulator while CompositionPlayer owns preview audio, preventing doubled audio.
- [x] Keep Intro Freeze on the proven ExoPlayer preview simulation for this gate; the immutable
  EditPlan still includes Freeze and final export still renders it.
- [x] Keep Adaptive candidate/sequence inspection on ExoPlayer source-time preview and safely return
  to CompositionPlayer afterward.
- [x] On CompositionPlayer setup/playback/readiness failure, block it only for the current source
  session and restore ExoPlayer live effects first; source-only preview remains the second fallback.
- [x] Reset the CompositionPlayer session block when the user explicitly retries live effects or
  imports a different source.
- [x] Add pure routing/timeline policy coverage, source preflight, implementation contract, and
  AndroidIDE verification instructions.
- [x] Preserve exactly one final `Transformer.start(Composition, ...)` path and prohibit preview
  actions from starting Transformer or creating intermediate MP4 files.
- [ ] Build/install `1.0-phase6f2.7` with FFmpeg enabled in AndroidIDE.
- [ ] Verify Trim/Adaptive + Transform + Audio + Blur + Logo realtime preview on the target device.
- [ ] Verify Intro Freeze and `-Precapflow.composition.preview.enabled=false` both use ExoPlayer
  fallback without losing the EditPlan.
- [ ] Force/capture a CompositionPlayer device failure if reproducible and confirm Exo live-effects
  recovery precedes source-only fallback.
- [ ] Perform exactly one final 1080p export after the preview matrix and confirm output parity,
  A/V sync, duration tolerance, bitrate diagnostics, and no intermediate renders.

Scope and device checks are documented in
[`docs/tasks/PHASE6F2_7_COMPOSITIONPLAYER_PREVIEW.md`](docs/tasks/PHASE6F2_7_COMPOSITIONPLAYER_PREVIEW.md)
and
[`docs/PHASE6F2_7_COMPOSITIONPLAYER_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_7_COMPOSITIONPLAYER_ANDROIDIDE_VERIFICATION.md).

### Phase 6F.2.6 — Shared Media3 Composition workflow (source implemented; AndroidIDE/device verification pending)

Create one authoritative `EditPlan → Composition` path before introducing CompositionPlayer live
preview, so export and the next preview gate cannot independently reinterpret clips, freeze, audio,
speed, overlays, or planned duration:

- [x] Add a Media3-independent composition-plan compiler for reviewed Trim/Adaptive ranges,
  Intro Freeze placement, audio topology, sequence counts, and expected duration.
- [x] Add a Media3 compiler that converts the shared plan into one explicit `Composition` with
  durations on every moving/still/audio item.
- [x] Route every final export through `Transformer.start(Composition, ...)`; remove the special
  single-item `EditedMediaItem` start branch.
- [x] Keep one video sequence for freeze + reviewed clips and add a second looping audio-only
  sequence only for Replace/Mix.
- [x] Preserve the source-audio continuity rule for Intro Freeze, H.264/AAC selection, bitrate,
  strict 720p/1080p/2K validation, cancellation, and private-first Gallery publication.
- [x] Add composition-topology logging and pure Kotlin unit coverage for normal, adaptive/freeze,
  replace, mix, mute, and silent-source decisions.
- [x] Add a Phase 6F.2.6 source preflight, task contract, implementation status, and target-device
  verification instructions.
- [ ] Build/install `1.0-phase6f2.6` with FFmpeg enabled in AndroidIDE.
- [ ] Verify normal Trim, Adaptive Cuts, Intro Freeze, Mute, Replace, Mix, cancellation, and exact
  720p/1080p/2K output metadata on the owner device.

This foundation does not activate CompositionPlayer in the editor, replace the proven ExoPlayer
fallback, add thumbnail/range-handle UI, add 4K/H.265, or change the FFmpeg build. Phase 6F.2.7 may
connect CompositionPlayer behind an explicit capability/fallback boundary after this shared export
graph passes the device matrix. Scope and checks are in
[`docs/tasks/PHASE6F2_6_SHARED_MEDIA3_COMPOSITION.md`](docs/tasks/PHASE6F2_6_SHARED_MEDIA3_COMPOSITION.md)
and
[`docs/PHASE6F2_6_SHARED_MEDIA3_COMPOSITION_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_6_SHARED_MEDIA3_COMPOSITION_ANDROIDIDE_VERIFICATION.md).

### Phase 6F.2.5 — Final-duration reconciliation (owner-confirmed done; extended device matrix pending)

Prevent small Media3 frame/codec timing differences or a sub-second reviewed clip remainder from
deleting an otherwise correct final render:

- [x] Compile the complete reviewed EditPlan, including Adaptive Cuts, Speed, and Intro Freeze,
  before displaying final-duration advice.
- [x] Calculate and display the nearest safely reachable whole second plus the exact millisecond
  difference in Review Editor → Export.
- [x] Require an explicit `Update clips` action; never silently change a user-reviewed timeline.
- [x] Adjust only Trim end or the final applied Adaptive range while preserving one-second range
  minimums, source bounds, prior clip order, and exact compiled target duration.
- [x] Keep the historical 250 ms post-render drift floor, allow at most 0.1% for longer outputs,
  and cap the exception at 750 ms.
- [x] Surface accepted drift above 250 ms as a warning while keeping exact resolution, even
  dimensions, H.264, AAC/mute policy, and selected 720p/1080p/2K quality as hard requirements.
- [x] Cover the owner-reported 293430 ms output versus 293154 ms plan: 276 ms is accepted inside
  the calculated 294 ms allowance rather than failing by 26 ms.
- [x] Add pure advisor/validation tests, source preflight, implementation status, and device
  verification instructions.
- [x] Build/install/complete `1.0-phase6f2.5`; owner confirmed Phase 6F.2.5 done on
  2026-08-27.
- [ ] Verify Trim/Adaptive update, 293154 ms reproduction, A/V sync, cancellation, Gallery, and
  720p/1080p/2K metadata on the owner device.

This gate does not add a second trim/pad transcode, ATS/voice timing, automatic changes without
confirmation, 4K, H.265, background rendering, or VPS processing. Scope and checks are in
[`docs/tasks/PHASE6F2_5_DURATION_RECONCILIATION.md`](docs/tasks/PHASE6F2_5_DURATION_RECONCILIATION.md)
and
[`docs/PHASE6F2_5_DURATION_RECONCILIATION_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_5_DURATION_RECONCILIATION_ANDROIDIDE_VERIFICATION.md).

### Phase 6F.2.4 — Preview capability and fallback separation (source implemented; AndroidIDE/device verification pending)

Prevent a rejected Media3 GPU graph from turning every subsequent editor interaction into the
same preview failure while leaving the final export plan authoritative:

- [x] Model live-effects, source-only, and unavailable preview states independently from the
  immutable edit/export settings.
- [x] Stop automatic effect-graph retries after fallback; Trim, Transform, Audio, and Overlay
  changes continue saving for final export without repeating the failure snackbar.
- [x] Add one explicit `Retry live effects` action so retry is user-controlled.
- [x] Recreate ExoPlayer before source-only recovery instead of asking a rejected player instance
  to decode the same source again.
- [x] Keep a successful source-only player visible and playable while preserving all selected
  effects for 720p/1080p/2K export.
- [x] Separate interactive preview geometry from export quality: do not upscale a 540-pixel source
  to 720 pixels merely to preview it, and cap higher-resolution previews at a 720-pixel short side.
- [x] Confirm an applying graph on the first rendered frame instead of labelling it applied at the
  `setVideoEffects` call site.
- [x] Log Media3 error name/code, graph summary, source dimensions, codec, recovery stage, and
  explicit retry generation for target-device diagnosis.
- [x] Replace misleading FFmpeg-metadata UI text with edit/export-safe fallback messaging.
- [x] Keep the Phase 6F.2.3 final render coordinator, exact output validation, Gallery publication,
  and optional rendered-output playback unchanged.
- [ ] Build/install `1.0-phase6f2.4` with FFmpeg enabled in AndroidIDE.
- [ ] Verify effect failure → one source-only recovery → continued editing → explicit retry,
  including 540×960 input, combined Transform/Blur/Logo, recreation, and 720p/1080p/2K export.

This gate does not add a proxy file, FFmpeg frame renderer, custom decoder, effect blacklist,
background render job, or change final export pixels. Proxy generation remains a later opt-in
fallback only if the target-device matrix proves source-only recovery insufficient. Task scope
and checks are in
[`docs/tasks/PHASE6F2_4_PREVIEW_FALLBACK_SEPARATION.md`](docs/tasks/PHASE6F2_4_PREVIEW_FALLBACK_SEPARATION.md)
and
[`docs/PHASE6F2_4_PREVIEW_FALLBACK_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_4_PREVIEW_FALLBACK_ANDROIDIDE_VERIFICATION.md).

### Phase 6F.2.3 — Production render quality selection (source implemented; AndroidIDE/device verification pending)

Replace the inherited Phase 5 sequential test gate with one user-selected final export:

- [x] Add one typed production quality model for `720p`, `1080p`, and `2K`; do not expose 4K.
- [x] Define 2K as QHD/1440p: an exact 1440-pixel short side, producing 1440×2560
  portrait or 2560×1440 landscape output for the standard 9:16/16:9 presets.
- [x] Put the single-selection quality control in Review Editor → Export and default to 1080p.
- [x] Persist the selected quality in schema-2 editor preferences while migrating schema-1
  Transform/Audio/Overlay presets with a safe 1080p default.
- [x] Compile the chosen short side into Media3 Presentation; never silently downgrade a 2K
  request to a completed 1080p file.
- [x] Request deterministic H.264 bitrate bands of 6–12 Mbps (720p), 12–24 Mbps (1080p), and
  20–40 Mbps (2K), while continuing to display the device encoder's actual average bitrate.
- [x] Replace 720p → Play → 1080p unlock with one `Render and save <quality>` action.
- [x] Keep rendered-video playback as an optional preview only; it no longer mutates render state
  or unlocks another quality.
- [x] Validate finalized output track dimensions, H.264 codec, AAC/mute policy, and planned
  duration before the private file may enter Completed or public MediaStore publication.
- [x] Reject/delete an unexpected resolution/codec/audio fallback rather than labelling it as the
  selected quality.
- [x] Keep realtime ExoPlayer/GPU preview, immutable EditPlan effects, cancellation, private-first
  output, and byte-for-byte Gallery publication unchanged.
- [ ] Build/install `1.0-phase6f2.3` with FFmpeg enabled in AndroidIDE.
- [ ] Verify 720p, 1080p, and 2K in portrait, landscape, square, and Original aspect on-device,
  including exact dimensions, H.264/AAC tracks, duration, actual bitrate, playback, Gallery,
  Open/Share, upscaling warnings, cancellation, and an unsupported-2K encoder failure.

This gate does not add 4K, H.265, AI upscaling, sharpening, background render jobs, proxy files,
or a software encoder. Task scope and device checks are in
[`docs/tasks/PHASE6F2_3_PRODUCTION_RENDER_QUALITY.md`](docs/tasks/PHASE6F2_3_PRODUCTION_RENDER_QUALITY.md)
and
[`docs/PHASE6F2_3_PRODUCTION_RENDER_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_3_PRODUCTION_RENDER_ANDROIDIDE_VERIFICATION.md).

### Phase 6F.2.2 — Video gallery picker UX (source implemented; AndroidIDE/device verification pending)

Replace the folder-first source video picker with Android's privacy-preserving system media picker
so users can identify clips by thumbnail instead of searching device folders:

- [x] Replace the source-video `OpenDocument` contract with `PickVisualMedia`.
- [x] Restrict the request to `PickVisualMedia.VideoOnly`.
- [x] Pin AndroidX Activity KTX 1.10.1 for an explicit supported picker contract.
- [x] Ask Google Play services to install the backported Photo Picker on eligible API 28-29
  devices.
- [x] Retain AndroidX's automatic `ACTION_OPEN_DOCUMENT` fallback when Photo Picker is absent.
- [x] Preserve cancellation, best-effort persistable URI grants, private working-copy import,
  FFmpeg probe, source replacement, preview, editing, render, and export behavior.
- [x] Avoid a broad `READ_MEDIA_VIDEO` permission and keep selection limited to one video.
- [x] Add a source preflight, implementation status, and owner-device verification matrix.
- [ ] Build/install `1.0-phase6f2.2` in AndroidIDE.
- [ ] Confirm Import video and Choose another video open video thumbnails, selection imports and
  probes successfully, cancellation is safe, and unsupported devices retain the file fallback.

This gate does not add a custom gallery, multi-select, camera capture, cloud upload, or change the
audio/logo document pickers. Task scope and device checks are in
[`docs/tasks/PHASE6F2_2_VIDEO_GALLERY_PICKER.md`](docs/tasks/PHASE6F2_2_VIDEO_GALLERY_PICKER.md)
and
[`docs/PHASE6F2_2_VIDEO_GALLERY_PICKER_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_2_VIDEO_GALLERY_PICKER_ANDROIDIDE_VERIFICATION.md).

### Phase 6F.2.1 — Source-subtitle blur ghosting hotfix (source implemented; AndroidIDE/device verification pending)

Correct the owner-observed result where the localized blur displays several readable copies of
the same source subtitle instead of obscuring it:

- [x] Replace the sparse 13-tap shader, whose full-radius and double-radius samples create
  visible text echoes, with one normalized dense 9×9 kernel.
- [x] Map editor strength to the same bounded `strength / 2` radius contract used by the
  GitHub RecapFlowAI server box-blur implementation.
- [x] Clamp every shader sample to the selected blur rectangle so texture wrap/mirror behavior
  cannot repeat subtitle pixels at the region edges.
- [x] Scale the kernel from a 720-pixel short-side reference and cap it for narrow regions so
  720p/1080p and portrait/landscape output retain equivalent normalized behavior.
- [x] Keep feathered region edges, unchanged pixels outside the rectangle, source-time gating,
  retained realtime state, immutable export snapshots, and Transform → Blur → Logo order.
- [x] Keep direct source-blur screen touch disabled; this quality hotfix does not reopen the
  separate release-touch crash path.
- [x] Preserve all Phase 6F.2 preferences, public export, bitrate, audio, cancellation, and
  source/project lifecycle behavior.
- [x] Add pure kernel-policy tests and a source preflight that rejects the former sparse-tap
  shader markers.
- [ ] Build/install `1.0-phase6f2.1` in AndroidIDE.
- [ ] Verify strengths 4/14/30/32 in paused and playing preview plus 720p/1080p export; strength
  30 must make source text unreadable without repeated lines, trails, or edge wrapping.

Task scope and device checks are in
[`docs/tasks/PHASE6F2_1_SOURCE_BLUR_QUALITY.md`](docs/tasks/PHASE6F2_1_SOURCE_BLUR_QUALITY.md)
and
[`docs/PHASE6F2_1_SOURCE_BLUR_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_1_SOURCE_BLUR_ANDROIDIDE_VERIFICATION.md).

### Phase 6F.2 — Save/reset editor configuration (source implemented; AndroidIDE/device verification pending)

Persist typed editor preferences separately from each immutable render snapshot. Add explicit
Save preset, restore-last-session, Reset section, and Reset all actions; use versioned local
storage and never persist transient player/render state or secrets.

- [x] Add typed `EditorPreferencesSnapshot`, schema-versioned local storage, and sanitization.
- [x] Keep last-session state separate from the explicit saved preset.
- [x] Add Auto-restore, Save preset, Restore saved preset, and Restore last session controls in
  Settings.
- [x] Add Reset current section and confirmed Reset all actions.
- [x] Persist Transform, Audio values/policy, Overlay geometry, Adaptive preset, selected editor
  section, expanded panels, and preview-card layout through debounced editor hooks.
- [x] Exclude source/imported asset paths, trim/player/adaptive-reviewed ranges, render/output
  state, API keys, tokens, and credentials.
- [x] Keep missing replacement-audio/image asset operations Off during restore.
- [x] Disable saved-setting mutations while an immutable render is active.
- [x] Add sanitizer unit tests, source preflight, task scope, implementation status, and device
  verification matrix.
- [ ] Build/install `1.0-phase6f2` in AndroidIDE.
- [ ] Pass save/restore/auto-restore/reset and existing preview/render/export regressions on device.

Task scope is in
[`docs/tasks/PHASE6F2_EDITOR_PREFERENCES.md`](docs/tasks/PHASE6F2_EDITOR_PREFERENCES.md)
and device checks are in
[`docs/PHASE6F2_EDITOR_PREFERENCES_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_EDITOR_PREFERENCES_ANDROIDIDE_VERIFICATION.md).

### Phase 6E.2.1 — Realtime logo controls hotfix (implemented; merged into 6E.3A)

Fix the owner-observed mismatch where the Position preset, Horizontal/Vertical, Logo width,
Opacity, and active-time controls update their labels/state but an already-created Media3 custom
preview shader continues drawing the logo at its first top-right/default-size geometry:

- [x] Add one thread-safe preview-only snapshot bridge for the compiled image overlay.
- [x] Synchronize that bridge immediately from every Overlay control change, including master/item
  Off, Remove, Reset, preset, sliders, time range, import, and source replacement.
- [x] Resolve logo position, size, opacity, and active source-time from the current snapshot in
  every preview frame so a retained custom shader cannot keep stale geometry.
- [x] Match snapshots by app-private asset path so an old shader cannot draw a replaced image.
- [x] Pass the same bridge through normal, Trim, adaptive-candidate, and adaptive-sequence preview
  graphs; retain the same-position seek for paused-frame redraw.
- [x] Keep `forRender` immutable and continue compiling export from the reviewed `EditPlan` rather
  than from mutable preview state.
- [x] Keep direct screen-touch logo drag/resize unregistered; Phase 6E.2.1 is slider/preset only.
- [x] Add state replacement, asset mismatch, and disabled-state regression tests plus a source
  preflight that verifies every preview call receives the bridge.
- [ ] Build/install `1.0-phase6e2.1` in AndroidIDE on the owner device.
- [ ] Verify TL/TR/Center/BL/BR, both position sliders, 8–80% size, 10–100% opacity, time range,
  paused and playing frames, Remove/Off, asset replacement, adaptive preview, and 720p/1080p
  preview/export parity.

Task scope is in
[`docs/tasks/PHASE6E2_1_REALTIME_LOGO_CONTROLS.md`](docs/tasks/PHASE6E2_1_REALTIME_LOGO_CONTROLS.md),
with device checks in
[`docs/PHASE6E2_1_REALTIME_LOGO_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6E2_1_REALTIME_LOGO_ANDROIDIDE_VERIFICATION.md).

### Phase 6E.2 — Static image/logo overlay (source implemented; hotfix verification open)

Add one local still image/logo operation to the verified Review Editor Overlay tab:

- [x] Add a typed, default-Off Image/logo operation under the Overlay master switch.
- [x] Use Android's system document picker for PNG, JPEG, and WebP; copy and validate the
  selected image in an app-private cache with bounded file/dimension checks.
- [x] Preserve PNG transparency and decode a bounded 2048-pixel texture for GPU safety.
- [x] Add Replace, Remove, and Reset actions plus five position presets.
- [x] Add normalized Horizontal/Vertical center sliders, 8–80% frame-width size, 10–100%
  opacity, and an absolute source-time range initialized from Trim.
- [x] Preserve the source image aspect ratio on Original, 9:16, 16:9, and 1:1 output frames;
  clamp the complete logo inside the composed frame without stretching it.
- [x] Composite in deterministic Transform → Source blur → Image order so source blur never
  unintentionally softens the logo.
- [x] Use the same typed effect for ExoPlayer realtime preview, adaptive ranges, intro freeze,
  and 720p/1080p Transformer export.
- [x] Restore image settings and the validated app-private asset across Activity recreation;
  remove the previous asset when the source project is replaced.
- [x] Keep both master-Off and item-Off as true compiler omissions, and add compiler,
  validation, range, and aspect-preserving layout tests.
- [x] Keep direct image drag/resize touch listeners unregistered; owner explicitly deferred
  Phase 6E.2.2 until the separate source-blur release crash has a reproducible trace.
- [x] Owner imported a logo and confirmed it is composited in preview; device testing exposed stale
  preset/slider geometry, now addressed by Phase 6E.2.1.
- [ ] Build/install `1.0-phase6e2.1` on the target device.
- [ ] Verify PNG alpha, all presets/sliders, paused/playing realtime preview, Trim/time range,
  Original/9:16/16:9/1:1, Fit/Fill, adaptive ranges, intro freeze, rotation/state restore,
  Remove/Off omission, source replacement cleanup, 720p/1080p parity, and cancellation.

Phase 6E.2 does not add animation loops, direct screen-touch image editing, video overlay,
Gemini, an Export destination, or Telegram delivery. Those remain separate gates after this
static image path passes on-device. Task scope and device checks are in
[`docs/tasks/PHASE6E2_STATIC_IMAGE_OVERLAY.md`](docs/tasks/PHASE6E2_STATIC_IMAGE_OVERLAY.md)
and
[`docs/PHASE6E2_STATIC_IMAGE_OVERLAY_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6E2_STATIC_IMAGE_OVERLAY_ANDROIDIDE_VERIFICATION.md).

### Phase 6E.1.5 — Fit preview aspect parity (owner confirmed)

Correct the owner-reported realtime preview regression where a portrait 9:16 source was
stretched horizontally after selecting 16:9 + Fit:

- [x] Preserve Media3 `Presentation.LAYOUT_SCALE_TO_FIT` as the shared preview/export
  geometry operation; Fit retains all source pixels and may add pillarbox/letterbox bars.
- [x] Introduce one explicit preview-aspect owner policy so PlayerView and Media3 effects do
  not both reinterpret the decoded source aspect.
- [x] Keep ordinary source/fallback playback on PlayerView `RESIZE_MODE_FIT`.
- [x] When Presentation or custom Crop owns output geometry, make PlayerView display the
  complete effect frame in its matching card with `RESIZE_MODE_FILL`; this fills only the
  card with the already-composed frame and does not stretch source pixels.
- [x] Return aspect ownership to PlayerView if the live-effects path falls back.
- [x] Preserve 9:16, 16:9, 1:1, Fit/Fill, custom Crop, preview move/resize, slider-only blur,
  and the unchanged shared Transformer export effect chain.
- [x] Add policy tests for ordinary source, portrait-to-landscape Fit, custom Crop, and
  live-effects fallback.
- [x] Build/install `1.0-phase6e1.5` on the target device; owner confirmed Phase 6E.1.5
  complete on 2026-08-26.
- [x] Verify a portrait source in 16:9 Fit shows unchanged people/text proportions with
  black side bars, while 16:9 Fill center-crops without stretching.
- [ ] Compare realtime preview with 720p and 1080p output for portrait and landscape input.

Direct blur manipulation stays disabled by the Phase 6E.1.4 safety gate. This patch does
not add another crop mode or alter encoded Fit/Fill semantics. Task scope and device checks
are in
[`docs/tasks/PHASE6E1_5_FIT_PREVIEW_ASPECT_PARITY.md`](docs/tasks/PHASE6E1_5_FIT_PREVIEW_ASPECT_PARITY.md)
and
[`docs/PHASE6E1_5_FIT_PREVIEW_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6E1_5_FIT_PREVIEW_ANDROIDIDE_VERIFICATION.md).

### Phase 6E.1.4 — Touch rollback + preview bounds recovery (source implemented; superseded by Phase 6E.1.5)

Stabilize the owner workflow before opening Phase 6E.2:

- [x] Temporarily disable direct drag/resize of the source-blur guide behind an explicit
  default-false source flag after Phase 6E.1.3 still terminated on finger release.
- [x] Keep the normalized blur rectangle, guide outline, realtime GPU result, Reset,
  Horizontal, Vertical, Width, Height, strength, time range, and export executor working.
- [x] Make the guide non-clickable/non-focusable, hide its resize handle, and tell the user
  to use the stable geometry sliders; retain the isolated gesture implementation only for
  later crash-trace work.
- [x] Change the movable/resizable PlayerView from its default separate `SurfaceView` to an
  embedded `TextureView` so video pixels follow the card through dynamic aspect/layout changes.
- [x] Clip the preview hierarchy and refresh PlayerView/TextureView geometry after Original,
  9:16, 16:9, 1:1, or crop-driven preview dimensions change.
- [x] Preserve preview move/resize/reset, Transform effect parity, Trim, Adaptive Cuts, Audio,
  Overlay settings, cancellation, 720p/1080p rendering, and local-only media behavior.
- [ ] Build/install `1.0-phase6e1.4` on the target device.
- [ ] Verify slider-only blur, PlayerView controls, preview move/resize, and repeated aspect
  switching with portrait and landscape H.264/HEVC sources before resuming Phase 6E.2.

Direct blur manipulation on the preview is deliberately unavailable in this gate; it must
not be re-enabled until a complete release-time crash trace and a separate regression test
prove it safe. Task scope and device checks are in
[`docs/tasks/PHASE6E1_4_TOUCH_ROLLBACK_PREVIEW_BOUNDS.md`](docs/tasks/PHASE6E1_4_TOUCH_ROLLBACK_PREVIEW_BOUNDS.md)
and
[`docs/PHASE6E1_4_TOUCH_ROLLBACK_PREVIEW_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6E1_4_TOUCH_ROLLBACK_PREVIEW_ANDROIDIDE_VERIFICATION.md).

### Phase 6E.1.3 — Release commit deferral (owner result: release crash remains)

Fix the owner-confirmed crash that remains specifically after the finger is released:

- [x] Keep the Phase 6E.1.2 compositor-only drag/resize outline during `ACTION_MOVE`.
- [x] Make `ACTION_UP`/`ACTION_CANCEL` release parent interception and schedule work only;
  do not reset layout, write sliders, mutate the typed rectangle, or update Media3 there.
- [x] Run the geometry/layout/slider commit from `postOnAnimation`, after Android finishes
  dispatching the touch stream to the guide or resize handle.
- [x] Keep the released outline visually in place until the deferred rectangle is committed.
- [x] Coalesce the Media3 live-effect update through the existing 140 ms preview queue
  rather than rebuilding the graph immediately in the deferred layout frame.
- [x] Remove `performClick()` from these non-clickable drag surfaces and document the
  deliberate accessibility-lint suppression on the gesture binder.
- [x] Validate the active source and editor state before applying a deferred commit.
- [x] Cancel pending release commits on a new gesture, source replacement, player error,
  stop, and Activity destruction; restore guide compositor identity safely.
- [x] Add `RecapFlowBlur` scheduled/applied/discarded/failure logs and source-preflight
  guards that prohibit release-time committed-state work.
- [ ] Build/install `1.0-phase6e1.3` on the target device.
- [ ] Verify repeated drag/resize releases while playing/paused, slider regressions,
  lifecycle/source cancellation, LogWire output, and 720p/1080p export parity.

This hotfix does not change the blur shader, typed export plan, supported time/strength
range, or open Phase 6E.2. Task scope and device checks are in
[`docs/tasks/PHASE6E1_3_RELEASE_COMMIT_DEFERRAL.md`](docs/tasks/PHASE6E1_3_RELEASE_COMMIT_DEFERRAL.md)
and
[`docs/PHASE6E1_3_RELEASE_COMMIT_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6E1_3_RELEASE_COMMIT_ANDROIDIDE_VERIFICATION.md).

### Phase 6E.1.2 — Touch gesture isolation (owner device result: move stable, release crash)

Fix the owner-confirmed direct-touch-only crash while preserving the stable slider,
typed blur, preview, and export paths:

- [x] Keep drag/resize outline feedback on every pointer frame using view compositor
  `translation`/`scale` only.
- [x] Do not mutate the touched guide's layout params, programmatically write geometry
  sliders, change typed blur settings, or rebuild Media3 effects during `ACTION_MOVE`.
- [x] Commit one validated normalized rectangle after `ACTION_UP`/`ACTION_CANCEL`, then
  update sliders/layout and apply one immediate live-preview graph update.
- [x] Isolate drag and resize pending rectangles from the last committed `EditPlan`.
- [x] Restore gesture transforms and parent interception after release or a caught
  runtime failure.
- [x] Add targeted `RecapFlowBlur` commit/failure logs for LogWire App Logs.
- [x] Preserve direct slider editing and Phase 6E.1 720p/1080p export behavior.
- [x] Add current-source identity and gesture-isolation preflight markers.
- [x] Build/install `1.0-phase6e1.2` on the target device.
- [ ] Verify at least 20 drag and 20 resize gestures while playing and paused, followed
  by slider sweeps, recreation, 720p/1080p export parity, and LogWire inspection.

This stability gate deliberately updates the rendered blur pixels only after the finger
is released; the marked outline remains realtime during the gesture. It does not add a
second blur region, tracking/keyframes, image/video overlay, Gemini, Export/Download,
Telegram, or background rendering. Task scope and checks are in
[`docs/tasks/PHASE6E1_2_TOUCH_GESTURE_ISOLATION.md`](docs/tasks/PHASE6E1_2_TOUCH_GESTURE_ISOLATION.md)
and
[`docs/PHASE6E1_2_TOUCH_GESTURE_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6E1_2_TOUCH_GESTURE_ANDROIDIDE_VERIFICATION.md).

### Phase 6E.1.1 — Blur drag/resize realtime preview stability (superseded hotfix)

Stabilize the owner-reported Phase 6E.1 crash without changing the typed blur or
export contract:

- [x] Keep the marked guide moving/resizing on every touch frame while avoiding a
  synchronous Media3 effect-graph rebuild for every `ACTION_MOVE` event.
- [x] Coalesce geometry, strength, and time-range live-preview changes to at most one
  graph update every 140 ms and commit the latest drag/resize rectangle immediately
  on `ACTION_UP` or `ACTION_CANCEL`.
- [x] Update only geometry labels/sliders/guide during direct rectangle movement rather
  than rebinding the complete Overlay control card for every touch frame.
- [x] Bound the guide inside the preview and safely reject unexpected non-FrameLayout
  layout parameters instead of allowing an unchecked cast to terminate the Activity.
- [x] Catch synchronous live-effect update failures, restore the existing ordinary-source
  preview fallback, and retain the typed settings for export verification.
- [x] Emit targeted `RecapFlowPreview` and `RecapFlowBlur` logs, including asynchronous
  ExoPlayer errors, so Android Code Studio's LogWire App Logs can capture the cause.
- [x] Cancel queued updates during source replacement, player error, stop, and destroy;
  retain one dirty update across a normal stop/resume when safe.
- [x] Add source-preflight markers and focused AndroidIDE/LogWire verification steps.
- [ ] Build/install `1.0-phase6e1.1` on the target device.
- [ ] Verify repeated drag/resize and slider sweeps while playing/paused, LogWire error
  capture, fallback, recreation, 720p/1080p parity, and no regression to Phase 6E.1.

This hotfix does not integrate LogWire into release builds, alter shader pixels, add a
second blur region, or open Phase 6E.2. Android Code Studio already supplies the LogWire
receiver shown by the owner; RecapFlow emits conventional Android log tags only.
Task scope and device checks are in
[`docs/tasks/PHASE6E1_1_BLUR_DRAG_STABILITY.md`](docs/tasks/PHASE6E1_1_BLUR_DRAG_STABILITY.md)
and
[`docs/PHASE6E1_1_BLUR_DRAG_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6E1_1_BLUR_DRAG_ANDROIDIDE_VERIFICATION.md).

### Phase 6E.1 — Manual Source Subtitle Blur (baseline source complete)

Open the Review Editor Overlay surface with one deterministic, reversible operation
that lets the user cover existing source-video captions without Gemini or automatic
detection:

- [x] Add `Overlay` as the fourth visible Review Editor tab after Clips, Transform,
  and Audio; keep Export hidden until its executor exists.
- [x] Add a default-Off Overlay master switch and a separately remembered default-Off
  `Source subtitle blur` switch. Either Off state omits the blur effect completely.
- [x] Represent one normalized top-left-origin blur rectangle, source-time range, and
  strength in typed `OverlaySettings`; Activities do not build shader or FFmpeg strings.
- [x] Default the rectangle to the lower caption-safe area and default the time range
  to the selected Trim when the source is first loaded or reset.
- [x] Let the user drag the marked preview rectangle, resize its bottom corner, or use
  precise Horizontal, Vertical, Width, and Height sliders.
- [x] Add a 4–32 blur-strength control, an absolute source-time RangeSlider, Reset,
  and collapsible detail controls.
- [x] Implement one localized GPU blur shader with feathered edges; pixels outside the
  selected rectangle remain unchanged.
- [x] Apply the same typed effect after Transform presentation in realtime ExoPlayer
  preview and Media3 720p/1080p export.
- [x] Map absolute source time into ordinary Trim, reviewed Adaptive candidate/sequence,
  concatenated render items, and the Intro Freeze source frame.
- [x] Preserve Transform, Audio, Trim, Adaptive Cuts, preview move/resize, render locks,
  cancellation, stale-output invalidation, and Activity recreation state.
- [x] Add compiler/validator tests, GLSL/resource markers, source preflight, and
  AndroidIDE/device verification instructions.
- [ ] Build/install `1.0-phase6e1` on the target device.
- [ ] Verify drag/resize/sliders, strength/time boundaries, outside-region sharpness,
  preview/export parity, Transform combinations, Adaptive Cuts, Freeze, 720p/1080p,
  cancellation, recreation, fallback, and source replacement.

**Phase 6E.1 compile hotfix (2026-08-25):** The first AndroidIDE
`:app:compileDebugKotlin` run showed that Media3 `1.8.0` resolves
`GlProgram(Context, String, String)` rather than the later raw-resource-ID overload.
The blur shaders now ship under `app/src/main/assets/shaders/` and the effect passes
their asset paths. The source verifier rejects a future `R.raw` regression. The
AndroidIDE build/install and device matrix remain open.

This gate supports one manual source-caption blur region only. It does not add multiple
blur regions, tracking/keyframes, image/video assets, text rendering, automatic subtitle
detection, Gemini, Export/Download delivery, Telegram, or VPS processing. Task scope and
device checks are tracked in
[`GitHub issue #21`](https://github.com/ZeusOwner/recapflow-ai/issues/21),
[`docs/tasks/PHASE6E1_SOURCE_SUBTITLE_BLUR.md`](docs/tasks/PHASE6E1_SOURCE_SUBTITLE_BLUR.md)
and
[`docs/PHASE6E1_SOURCE_SUBTITLE_BLUR_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6E1_SOURCE_SUBTITLE_BLUR_ANDROIDIDE_VERIFICATION.md).

### Phase 6D.4 — Mix Audio: independent two-track preview and export (owner confirmed complete)

Combine the source soundtrack with one selected, app-private external audio asset
without changing the verified Keep, Mute, or Replace behaviors:

- [x] Expose `Mix` only after adding its typed plan, validation, preview, and export
  executor.
- [x] Reuse the verified `audio/*` picker/private asset lifecycle from Replace; switching
  between Replace and Mix keeps the valid selected asset until it is changed or cleared.
- [x] Require original source audio for Mix and show a pre-render validation message for
  silent sources; recommend Replace for those inputs.
- [x] Add remembered, independent 0–100% `Original volume` and `Added audio volume`
  controls with a conservative 70% + 30% reset balance.
- [x] Apply both gains immediately in realtime preview while the external player follows
  Trim, Adaptive candidate/sequence, Speed, seek/pause/play, and Intro Freeze output time.
- [x] Keep source audio in every edited video item and add the selected looping audio-only
  sequence to the same Media3 Composition.
- [x] Normalize both Mix inputs to signed 16-bit stereo before Composition mixing so mono,
  stereo, and conservative multi-channel inputs expose a consistent mixer format.
- [x] Keep the final duration owned by the non-looping edited video sequence: short added
  audio loops and long added audio is truncated at the final video frame.
- [x] Preserve Audio Off, Keep Original, Mute, Replace, cancellation, lifecycle cleanup,
  source-change cleanup, stale-output invalidation, 720p, and 1080p behavior.
- [x] Add compiler, validator, PCM normalization, source-preflight, and AndroidIDE/device
  verification coverage.
- [x] Build/install `1.0-phase6d4` on the target device; owner confirmed completion
  on 2026-08-25.
- [ ] Verify MP3/M4A/AAC selection; mono/stereo combinations; realtime balance/sync;
  short-loop/long-trim; Trim/Adaptive/Speed/Freeze; 0/30/50/70/100% gains; 720p/1080p;
  cancellation; recreation; and Keep/Mute/Replace regressions.

This gate uses constant user-selected gains. It does not add automatic ducking, audio
fades, loudness normalization, boost above unity, waveform editing, multiple added
tracks, Overlay, upload media, or call Gemini. Task scope and device checks are in
[`docs/tasks/PHASE6D4_MIX_AUDIO.md`](docs/tasks/PHASE6D4_MIX_AUDIO.md) and
[`docs/PHASE6D4_MIX_AUDIO_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6D4_MIX_AUDIO_ANDROIDIDE_VERIFICATION.md).

### Phase 6D.3 — Replace Audio: local picker, realtime preview, and export parity (owner confirmed complete)

Replace the source soundtrack with one user-selected, app-private audio asset while
keeping the verified Phase 6D.2 volume semantics:

- [x] Expose `Replace` only after adding its typed asset, validation, preview, and
  render executor; keep `Mix` blocked.
- [x] Use Android's system document picker with `audio/*`, persist read permission
  when available, and copy the selected file into the private on-device workspace.
- [x] Read and validate local duration, filename, and size without network upload;
  reject empty/unreadable selections and remove superseded private copies.
- [x] Restore the private selected asset across Activity recreation and clear it when
  the project source video changes.
- [x] Mute original source audio immediately under Replace and synchronize a dedicated
  local audio preview player to ordinary Trim, Adaptive candidate, continuous
  sequence, Speed, seek/pause/play, and Intro Freeze output time.
- [x] Apply the existing 0–100% Volume value to the active replacement track in both
  preview and export; 0% keeps a silent AAC replacement track.
- [x] Export a Media3 Composition containing one non-looping edited video sequence and
  one looping audio-only sequence; short replacement audio loops and long audio ends
  exactly with the edited video.
- [x] Keep Mute as track removal and Keep Original as the verified source-audio path.
- [x] Invalidate stale output when the selected asset or Replace/Volume state changes,
  release both preview decoders before render, and lock picker controls during render.
- [x] Add compiler/validator tests, source preflight, and AndroidIDE/device verification
  instructions.
- [x] Build/install `1.0-phase6d3` on the target device; owner confirmed completion
  on 2026-08-25.
- [ ] Verify MP3/M4A/AAC selection, short-loop/long-trim behavior, realtime seek and
  playback sync, Trim/Adaptive/Speed/Freeze, 0/50/100% Volume, 720p/1080p,
  cancellation, recreation, and source replacement.

This gate fully replaces original source audio; it does not mix tracks, edit a
waveform, add fades/ducking/normalization, upload media, expose Overlay, or call
Gemini. Task scope and device checks are in
[`docs/tasks/PHASE6D3_REPLACE_AUDIO.md`](docs/tasks/PHASE6D3_REPLACE_AUDIO.md)
and
[`docs/PHASE6D3_REPLACE_AUDIO_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6D3_REPLACE_AUDIO_ANDROIDIDE_VERIFICATION.md).

### Phase 6D.2 — Audio Volume: realtime preview/export parity (owner confirmed complete)

Extend only the verified Keep Original path with a conservative source-volume
control:

- [x] Add a remembered 0–100% Volume slider and Reset Volume action under Audio On +
  Keep Original; hide the detail controls for Audio Off and Mute.
- [x] Keep Audio Off and Keep Original at 100% as true compiler no-ops.
- [x] Apply the compiled linear gain immediately to ordinary source, candidate, and
  continuous cut-sequence preview without changing device-wide volume.
- [x] Apply the same constant gain after the Speed audio processor for every exported
  Trim/Adaptive range.
- [x] Preserve the distinction between Volume 0% (silent AAC track retained) and Mute
  (audio track removed).
- [x] Constrain this gate to attenuation/unity because Media3 Player realtime volume
  is 0–1; defer boost above unity until a limiter/normalization gate can preserve
  preview/export parity and prevent clipping.
- [x] Restore Volume across Activity recreation, invalidate stale output on change,
  and lock the control during an active render.
- [x] Keep Replace/Mix, audio asset picking, waveform editing, Gemini, and system
  volume changes unavailable.
- [x] Add compiler/validator/PCM scaling tests, source preflight, and AndroidIDE/device
  verification instructions.
- [x] Build/install `1.0-phase6d2` on the target device; owner confirmed completion
  on 2026-08-25.
- [ ] Verify 0/25/50/75/100% preview/export loudness, silent-track retention at 0%,
  Mute track removal, A/V sync, Speed/Adaptive/Freeze, 720p/1080p, cancellation, and
  recreation.

This gate changes decoded source-audio amplitude only. It does not change duration,
video pixels, device volume, or the selected audio asset. Task scope and device
checks are in
[`docs/tasks/PHASE6D2_AUDIO_VOLUME.md`](docs/tasks/PHASE6D2_AUDIO_VOLUME.md)
and
[`docs/PHASE6D2_AUDIO_VOLUME_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6D2_AUDIO_VOLUME_ANDROIDIDE_VERIFICATION.md).

### Phase 6D.1 — Audio Foundation: Keep Original / Mute (owner confirmed complete)

Open the first verified Audio surface without exposing unfinished asset or mixing
operations:

- [x] Add `Audio` as the third visible Review Editor tab after Clips and Transform.
- [x] Add a default-Off master Audio switch with remembered policy state.
- [x] Expose only `Keep Original` and `Mute`; keep Replace, Mix, and Volume hidden.
- [x] Treat Audio Off and enabled Keep Original as compiler no-ops.
- [x] Apply Mute immediately to ordinary source, candidate, and continuous cut-sequence
  preview without changing device-wide volume.
- [x] Compile Mute to Media3 `EditedMediaItem.Builder.setRemoveAudio(true)` for every
  selected Trim/Adaptive range.
- [x] Keep enabled Speed video processing while omitting its audio processor for a
  muted export.
- [x] Avoid forcing the Intro Freeze silence track when Mute requests a video-only MP4.
- [x] Restore Audio On/Off, policy, and selected Audio tab across Activity recreation.
- [x] Invalidate stale completed output when the Audio policy changes and lock Audio
  controls during an active render.
- [x] Block typed Replace/Mix plans until their executors exist.
- [x] Add compiler/validator tests, source preflight, and AndroidIDE/device checks.
- [x] Build/install `1.0-phase6d1` on the target device; owner confirmed completion
  on 2026-08-25.
- [ ] Verify Off/Keep/Mute realtime preview, audio-track presence/absence, 720p/1080p,
  Trim, Adaptive Cuts, Speed, Freeze, cancellation, and recreation.

This gate does not change video duration or pixels. It does not add Volume, Replace,
Mix, an audio picker, waveform editing, Overlay, Gemini, or background rendering.
Task scope and device checks are in
[`docs/tasks/PHASE6D1_AUDIO_FOUNDATION.md`](docs/tasks/PHASE6D1_AUDIO_FOUNDATION.md)
and
[`docs/PHASE6D1_AUDIO_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6D1_AUDIO_ANDROIDIDE_VERIFICATION.md).

### Phase 6C.2.1 — Movable and resizable Editor preview overlay (owner confirmed complete)

Refine only the floating Editor preview presentation before opening Audio controls:

- [x] Add an explicit `Move` handle that drags the preview without stealing
  PlayerView playback/seek gestures.
- [x] Add a bottom-corner resize handle that preserves the active source/output
  aspect ratio.
- [x] Allow resizing below and above the former one-third default while keeping
  the full preview card inside the live Editor viewport.
- [x] Keep the localized underlay mask aligned to the exact moved/resized preview
  rectangle so content outside the preview is not dimmed.
- [x] Add `Reset` to restore the device-adaptive one-third size and centered-top
  position.
- [x] Preserve normalized position and scale across Activity recreation and clamp
  them safely after screen-size/orientation changes.
- [x] Keep PlayerView controls, live Transform preview, Adaptive sequence preview,
  render progress, cancellation, and output playback unchanged.
- [x] Add source preflight and AndroidIDE/device verification instructions.
- [x] Build/install `1.0-phase6c2.1` on the target device; owner confirmed completion
  on 2026-08-25.
- [ ] Verify drag/resize/reset, bounds, rotation, compact/tablet layouts, localized
  underlay, PlayerView controls, and Phase 6C.2 preview/render regressions.

This is an Editor UI customization only. It does not move or resize pixels in the
rendered MP4, add an Overlay edit item, implement Audio, or expose Gemini. Task
scope and device checks are in
[`docs/tasks/PHASE6C2_1_MOVABLE_PREVIEW_OVERLAY.md`](docs/tasks/PHASE6C2_1_MOVABLE_PREVIEW_OVERLAY.md)
and
[`docs/PHASE6C2_1_MOVABLE_PREVIEW_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6C2_1_MOVABLE_PREVIEW_ANDROIDIDE_VERIFICATION.md).

### Phase 6C.2 — Continuous cut preview and per-clip edge transitions (owner confirmed complete)

Close the review/render feedback gap for the multi-range plan while preserving the
Phase 6C.1 concatenation path:

- [x] Add `Preview full cut sequence` / `Stop sequence preview` to Clips review.
- [x] Build a local ExoPlayer playlist from the reviewed clipping ranges in original
  story order; no temporary preview render and no network request are required.
- [x] Follow the playing playlist index in the candidate UI and restore the ordinary
  source preview after completion, stop, error, navigation, or edit changes.
- [x] Apply the shared Crop/Mirror/Color/Zoom/Aspect/Speed preview configuration to
  every sequence item.
- [x] Remove the Phase 6C.1 Visual Fade conflict and apply the selected fade mode and
  duration independently to every reviewed item in preview and export.
- [x] Define `In + Out` as a fade-through-black clip boundary; `Fade In` and
  `Fade Out` remain one-sided edge treatments.
- [x] Validate fade duration against every reviewed range after Speed, not only the
  outer Trim duration.
- [x] Keep transition duration out of planned duration and keep source audio unfaded.
- [x] Preserve Apply Off as a normal continuous Trim render.
- [x] Add validator coverage, source preflight, and an AndroidIDE/device matrix.
- [x] Build/install `1.0-phase6c2` on the target device; owner confirmed completion
  on 2026-08-25.
- [ ] Verify seamless playlist order, boundary visuals, 720p/1080p parity, duration,
  source-audio continuity/A-V sync, fallback, cancellation, and lifecycle behavior.

Media3 items in one `EditedMediaItemSequence` are sequential and do not overlap.
Therefore this gate deliberately implements fade-through-black/edge fades rather
than claiming an overlapping dissolve crossfade. Intro Freeze remains a separate
pre-render preview and the first exported segment only. Gemini remains unexposed.
Task scope and device checks are in
[`docs/tasks/PHASE6C2_CONTINUOUS_PREVIEW_TRANSITIONS.md`](docs/tasks/PHASE6C2_CONTINUOUS_PREVIEW_TRANSITIONS.md)
and
[`docs/PHASE6C2_CONTINUOUS_PREVIEW_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6C2_CONTINUOUS_PREVIEW_ANDROIDIDE_VERIFICATION.md).

### Phase 6C.1 — Reviewed Adaptive Cut draft and concatenation (owner confirmed complete)

Introduce the first multi-clip render plan without Gemini or opaque automatic
decisions:

- [x] Add typed `AdaptiveCutSettings`, `AdaptiveCutPreset`, and ordered `TrimRange`
  candidates to `EditPlan`.
- [x] Generate transparent deterministic pacing drafts with Gentle, Balanced, and
  Compact presets inside the current Trim range.
- [x] Preserve source/story order, retain the ending, enforce a one-second minimum,
  and bound drafts to at most 120 reviewed ranges.
- [x] Require candidate-by-candidate Previous/Preview/Next review and a separate
  `Apply reviewed cuts to render` switch.
- [x] Keep an unapplied draft out of `EditPlan` execution so normal Trim remains the
  render source until the user opts in.
- [x] Invalidate stale drafts when Trim or source media changes and restore valid
  draft state across Activity recreation.
- [x] Concatenate applied ranges in one Media3 `EditedMediaItemSequence`, retaining
  the selected visual transforms, constant Speed, source audio, and optional intro
  Freeze behavior for the first range.
- [x] Calculate planned output duration from reviewed ranges, then Speed, then Freeze.
- [x] Block Phase 6B.8 Visual Fade while Adaptive Cuts are applied because it is a
  single-clip edge fade, not a cross-clip transition.
- [x] Add range compiler/validator tests and source-verification markers.
- [x] Build/install the Phase 6C.1.1 resource-fixed project on the target device;
  owner confirmed completion on 2026-08-24.
- [ ] Verify draft review, Apply Off/On, 720p/1080p concatenation, audio continuity,
  A/V sync, combined effects, cancellation, process recreation, and source changes.

**Phase 6C.1.1 resource hotfix (2026-08-24):** The first AndroidIDE build stopped
at `:app:processDebugResources` because the new Clips-card separator referenced
an undefined `@color/rf_outline_variant`. The layout now uses the existing
theme-aware `@color/rf_outline`; the source preflight also validates every
app-owned `@color` reference before delivery. Patch identity is
`RecapFlowAI_Phase6C1_1` / `1.0-phase6c1.1`. Build/device verification remains open.

Phase 6C.1 is rule-based pacing assistance, not scene understanding. Gemini remains
unexposed. Task scope and
device checks are in
[`docs/tasks/PHASE6C1_ADAPTIVE_CUT_DRAFT.md`](docs/tasks/PHASE6C1_ADAPTIVE_CUT_DRAFT.md)
and
[`docs/PHASE6C1_ADAPTIVE_CUT_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6C1_ADAPTIVE_CUT_ANDROIDIDE_VERIFICATION.md).

### Phase 6B.8 — Single-clip visual Fade transitions (owner confirmed complete)

Add one reversible transition operation to the selected moving clip without
introducing a multi-clip timeline:

- [x] Expose a default-Off Visual Fade switch inside the collapsible Transform group.
- [x] Add remembered `Fade In`, `Fade Out`, and `In + Out` modes.
- [x] Add remembered `0.5 sec`, `1 sec`, and `1.5 sec` duration presets.
- [x] Implement a timestamp-driven RGB gain effect that fades to/from black.
- [x] Use the same effect compiler for realtime ExoPlayer preview and Media3 export.
- [x] Compensate the source-time fade span for enabled Speed so the selected
  user-facing duration remains stable in preview and exported output.
- [x] Keep Transition out of `EditPlan.plannedDurationMs`; it changes pixels, not time.
- [x] Validate unsupported durations and clips too short for the selected fade mode.
- [x] Preserve remembered Transition state across master Transform Off and Activity recreation.
- [x] Keep Intro Freeze separate: Fade targets only the selected moving source clip.
- [x] Keep audio unchanged; audio fades remain deferred to the Audio gate.
- [x] Add compiler/validator tests and source-verification markers.
- [x] Build/install `1.0-phase6b8` on the target device; owner confirmed completion
  on 2026-08-24.
- [ ] Verify realtime preview and 720p/1080p export for all modes/durations,
  Speed combinations, Trim boundaries, short-clip validation, cancellation, and recreation.

Crossfade requires two adjacent clips and is deferred to the multi-clip Phase 6C
timeline. Gemini remains unexposed. Task scope and device checks are in
[`docs/tasks/PHASE6B8_TRANSITIONS.md`](docs/tasks/PHASE6B8_TRANSITIONS.md) and
[`docs/PHASE6B8_TRANSITIONS_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6B8_TRANSITIONS_ANDROIDIDE_VERIFICATION.md).

### Phase 6B.7.1 — Collapsible Transform controls (owner confirmed complete)

Reduce Review Editor vertical clutter without changing any edit or render value:

- [x] Add an always-reachable `Hide controls` / `Show controls` action below
  the master Transform summary.
- [x] Collapse the entire Aspect/Crop/Mirror/Color/Zoom/Speed/Freeze detail group
  while retaining the Transform badge, master switch, and selected-settings summary.
- [x] Keep collapse/expand independent from Transform and per-feature On/Off states.
- [x] Preserve every configured value, live preview behavior, and `EditPlan` output
  while the control group is hidden.
- [x] Restore the collapse/expand state across Activity recreation.
- [x] Keep the action available during render because it changes presentation only.
- [x] Build/install `1.0-phase6b7.1` and verify expand/collapse on the target device;
  owner confirmed completion on 2026-08-24.
- [ ] Verify rotation/recreation, active render, and combined enabled-feature summary.

This is a UI-density refinement only. It does not change Freeze export, add
Transitions, expose Gemini, or introduce another editing operation.

### Phase 6B.7 — Intro Freeze (source implemented; device verification pending)

Add one reversible still-frame introduction at the selected Trim start:

- [x] Expose a default-Off Intro Freeze switch in Review Editor → Transform.
- [x] Add remembered `1 sec`, `2 sec`, and `3 sec` duration presets.
- [x] Treat Transform Off, Freeze Off, and unsupported duration plans as compiler no-ops.
- [x] Add a pre-render preview action that holds the selected Trim-start frame for
  the configured duration, then resumes source playback.
- [x] Include Freeze in `EditPlan.plannedDurationMs` after any Speed duration change.
- [x] Extract the selected source frame off the main thread into a bounded cache image.
- [x] Export the image intro and clipped source as one sequential Media3 composition.
- [x] Force a continuous audio track so the image intro receives silence before
  original source audio begins.
- [x] Apply enabled Aspect/Crop/Mirror/Color/Zoom visual settings to the image intro;
  Speed applies only to the source clip.
- [x] Clean the temporary still image after success, failure, cancellation, and close.
- [x] Preserve stale-output invalidation, active-render locks, and Activity recreation state.
- [x] Add compiler/validator tests for Off states, valid presets, planned duration,
  and invalid durations.
- [ ] Build/install `1.0-phase6b7` on the target device.
- [ ] Verify 1/2/3-second preview and 720p/1080p output duration, silent intro,
  first-frame continuity, A/V sync, combined effects, cancellation, and recreation.

This gate does not expose Transitions, Adaptive Edit, Audio controls, Overlay, or
Gemini. Task scope and device checks are in
[`docs/tasks/PHASE6B7_FREEZE.md`](docs/tasks/PHASE6B7_FREEZE.md) and
[`docs/PHASE6B7_FREEZE_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6B7_FREEZE_ANDROIDIDE_VERIFICATION.md).

### Phase 6B.6 — Speed (owner confirmed)

Add one reversible, constant-speed operation to the verified Transform pipeline:

- [x] Expose a default-Off Speed switch in Review Editor → Transform.
- [x] Add `0.5×`, `0.75×`, `1×`, `1.25×`, `1.5×`, and `2×` presets.
- [x] Remember the selected preset while Speed or master Transform is off.
- [x] Treat Transform Off, Speed Off, and enabled `1×` as compiler no-ops.
- [x] Apply the selected speed immediately to source preview playback.
- [x] Reset playback to `1×` for rendered-output verification.
- [x] Build paired Media3 audio/video speed effects from one constant
  `SpeedProvider` so the exported tracks share one timing source.
- [x] Place the video speed effect before export frame-rate normalization.
- [x] Include Speed in `EditPlan.plannedDurationMs` and render realtime-factor
  calculations.
- [x] Show an estimated output duration before render.
- [x] Preserve playback position, paused-frame refresh, stale-output
  invalidation, active-render locks, and Activity recreation state.
- [x] Validate the supported `0.5×`–`2×` speed range.
- [x] Add compiler/validator tests for Off, neutral `1×`, valid speed duration,
  and invalid range handling.
- [x] Build/install `1.0-phase6b6` on the target device; owner confirmed Phase
  6B.6 complete on 2026-08-22.
- [ ] Verify all presets during playing/paused preview, state restoration,
  720p/1080p duration, pitch-preserved audio, A/V sync, cancellation, and
  combined Crop/Mirror/Color/Zoom behavior.

This gate did not expose Freeze, Transitions, Adaptive Edit, Audio controls,
Overlay, or Gemini. Task scope and device checks are in
[`docs/tasks/PHASE6B6_SPEED.md`](docs/tasks/PHASE6B6_SPEED.md) and
[`docs/PHASE6B6_SPEED_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6B6_SPEED_ANDROIDIDE_VERIFICATION.md).

### Phase 6B.5 — Zoom modes (owner confirmed)

Add one reversible, canvas-preserving Zoom operation to the verified Transform
preview/export pipeline:

- [x] Expose a default-Off Zoom switch in Review Editor → Transform.
- [x] Keep the selected Zoom mode remembered while Zoom or master Transform is off.
- [x] Add `Zoom In`, `Zoom Out`, and `Alternate` mode controls.
- [x] Treat Zoom Off and explicit `ZoomMode.OFF` as true compiler no-ops.
- [x] Compile Zoom In to centered `1.15×` scale and Zoom Out to centered `0.90×` scale.
- [x] Compile Alternate to a deterministic `0.90×`–`1.10×` four-second cycle.
- [x] Keep frame-canvas dimensions unchanged while Zoom In crops outer edges and
  Zoom Out reveals the canvas background.
- [x] Apply one shared Crop → Mirror → Color → Zoom → Presentation effect order
  for ExoPlayer live preview and Transformer export.
- [x] Normalize Alternate preview time against the selected Trim start while
  export starts the same cycle from the clipped media timeline.
- [x] Preserve paused-frame redraw, playback position, stale-output
  invalidation, active-render locks, and live-effects fallback.
- [x] Restore Zoom On/Off and the selected mode across Activity recreation.
- [x] Add compiler test source for master-Off, Zoom-Off, static modes, and the
  repeatable Alternate cycle.
- [x] Build/install `1.0-phase6b5` on the target device; owner confirmed Phase
  6B.5 complete on 2026-08-22.
- [ ] Verify Zoom In/Out/Alternate while playing and paused, state restoration,
  preview/output parity, combined Crop/Mirror/Color/Aspect behavior, 720p/1080p
  A/V, cancellation, and low-device fallback.

This gate does not expose Speed, Freeze, Transitions, Adaptive Edit, Audio,
Overlay, or Gemini. Task scope and device checks are in
[`docs/tasks/PHASE6B5_ZOOM.md`](docs/tasks/PHASE6B5_ZOOM.md) and
[`docs/PHASE6B5_ZOOM_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6B5_ZOOM_ANDROIDIDE_VERIFICATION.md).

### Phase 6B.4 — Color adjustments (owner confirmed)

Add one collapsible, reversible Color operation to the verified Transform
preview/export pipeline:

- [x] Expose a default-Off Color switch in Review Editor → Transform.
- [x] Add Brightness and Contrast controls from `-50` to `+50`, Saturation
  from `-100` to `+100`, and Temperature from `-50` cool to `+50` warm.
- [x] Add one Reset Color action that restores all four neutral values.
- [x] Retain Color On/Off and all values while the master Transform switch is
  off, but omit Color from preview/export until Transform is re-enabled.
- [x] Compile UI units into Media3 Brightness, Contrast, HSL saturation, and
  conservative red/blue temperature scaling.
- [x] Omit the Color effect graph when Color is off, Transform is off, or every
  enabled value is neutral.
- [x] Share Crop → Mirror → Brightness → Contrast → Saturation → Temperature →
  Presentation order between ExoPlayer live preview and Transformer export.
- [x] Preserve paused-frame redraw, playback position, stale-output
  invalidation, active-render locks, and live-effects fallback.
- [x] Restore Color state across Activity recreation.
- [x] Add compiler and validation test source for Off, neutral, mapped, valid,
  and invalid states.
- [x] Build/install `1.0-phase6b4` on the target device; owner confirmed Phase
  6B.4 complete on 2026-08-22.
- [ ] Verify every slider while playing/paused, Reset, state restoration,
  preview/output parity, combined Crop/Mirror/Aspect behavior, 720p/1080p A/V,
  cancellation, and low-device fallback.

This gate did not expose Zoom, Speed, Freeze, Transitions, Adaptive Edit,
Audio, Overlay, or Gemini. Task scope and device checks are in
[`docs/tasks/PHASE6B4_COLOR.md`](docs/tasks/PHASE6B4_COLOR.md) and
[`docs/PHASE6B4_COLOR_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6B4_COLOR_ANDROIDIDE_VERIFICATION.md).

### Phase 6B.3 — Horizontal Mirror (owner confirmed)

Add the first independent Transform toggle on top of the verified live-preview
pipeline. Mirror must be visible before render and compile identically for the
720p/1080p export path:

- [x] Expose `Mirror horizontally` inside Review Editor → Transform.
- [x] Keep Mirror explicitly `Off` by default and remember its selection while
  the master Transform switch is off.
- [x] Compile Mirror On to a horizontal matrix flip (`scaleX = -1`,
  `scaleY = 1`) and omit the operation completely when either switch is off.
- [x] Apply the shared visual effect order Crop → Mirror → Presentation in both
  ExoPlayer live preview and Transformer export.
- [x] Preserve playback position and redraw paused frames after Mirror changes.
- [x] Invalidate a stale completed render after Mirror changes while leaving an
  active render's controls locked.
- [x] Restore Mirror state across Activity recreation.
- [x] Add compiler unit-test source for master-Off, Mirror-Off, and Mirror-On.
- [x] Build/install `1.0-phase6b3` on the target device; owner confirmed Phase
  6B.3 complete on 2026-08-22.
- [ ] Verify left/right preview motion, paused redraw, preview/output parity,
  Transform-Off regression, state restoration, 720p/1080p A/V, and cancellation.

This gate does not expose Color, Zoom, Speed, Freeze, Transitions, Adaptive
Edit, Audio, Overlay, or Gemini. Task scope and device checks are in
[`docs/tasks/PHASE6B3_MIRROR.md`](docs/tasks/PHASE6B3_MIRROR.md) and
[`docs/PHASE6B3_MIRROR_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6B3_MIRROR_ANDROIDIDE_VERIFICATION.md).

### Phase 6B.2.1 — Live Transform preview before render (owner confirmed)

Close the edit/render feedback gap reported during Phase 6B.2 device testing.
The user must see aspect, Fit/Fill, and custom crop changes in the floating
preview before committing time to a 720p or 1080p export:

- [x] Replace the legacy `VideoView` source preview with Media3 `PlayerView`
  backed by `ExoPlayer`.
- [x] Apply enabled Transform operations with `ExoPlayer.setVideoEffects`.
- [x] Build preview and export video-effect lists from one shared compiler so
  Crop → Presentation order and scale-mode semantics cannot drift.
- [x] Keep Transform Off as ordinary source playback with no optional effects.
- [x] Update the floating preview aspect immediately for Original/crop, 9:16,
  16:9, and 1:1 selections.
- [x] Preserve playback position and playing/paused intent across live changes;
  seek to the current position to redraw a paused frame.
- [x] Never apply live source effects a second time when playing a rendered
  720p/1080p output.
- [x] Restore ordinary source preview with a visible explanation if a device
  cannot initialize the live effects path; keep the EditPlan and render path.
- [x] Keep render-time codec release, progress, cancellation, partial-output
  cleanup, playback unlock, and 1080p sequencing unchanged.
- [x] Keep preview seek defaults strongly typed as `Long` milliseconds so the
  Kotlin 2.1 compiler does not infer a mixed `Number` default argument.
- [x] Build/install `1.0-phase6b2.1` on the target device; owner confirmed the
  gate complete on 2026-08-22 after the strongly typed preview-position fix.
- [ ] Verify live updates while playing and paused, preview/output framing
  parity, Transform-Off regression, 720p/1080p A/V, and low-device fallback.

This refinement does not add Mirror, Color, Zoom, Speed, Freeze, Transitions,
Adaptive Edit, Audio, Overlay, or Gemini. Task scope and device checks are in
[`docs/tasks/PHASE6B2_1_LIVE_TRANSFORM_PREVIEW.md`](docs/tasks/PHASE6B2_1_LIVE_TRANSFORM_PREVIEW.md)
and
[`docs/PHASE6B2_1_LIVE_PREVIEW_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6B2_1_LIVE_PREVIEW_ANDROIDIDE_VERIFICATION.md).

### Phase 6B.2 — Custom crop rectangle (device run confirmed)

Extend the working Transform tab with one reversible crop operation. Keep the
Phase 6B.1 aspect/Fit/Fill path and the verified Trim/render pipeline intact:

- [x] Add a nested Custom crop switch that defaults to `Off`.
- [x] Retain the remembered crop rectangle while Crop or Transform is off.
- [x] Expose Left, Top, Right, and Bottom edge controls in five-percent steps.
- [x] Keep crop controls collapsed until Transform and Custom crop are both on.
- [x] Validate normalized crop bounds and preserve at least ten percent of each
  source-frame dimension.
- [x] Compile the rectangle into Media3 normalized device coordinates.
- [x] Apply Media3 `Crop` before the existing `Presentation` and frame-rate
  effects so aspect Fit/Fill remains deterministic.
- [x] Prove by unit-test source that Transform Off and Crop Off omit Crop.
- [x] Restore Crop On/Off and all four edges across Activity recreation.
- [x] Disable crop controls during an active render and invalidate stale output
  when the user changes crop settings after completion.
- [x] Preserve output-aspect preview sizing when Original aspect uses a custom
  crop rectangle.
- [x] Build/install `1.0-phase6b2` on the target device; the owner screenshot on
  2026-08-20 confirms Transform On, 9:16 Fit, asymmetric custom crop values,
  and a completed 1080p output.
- [ ] Finish the Off regression, preview/output crop comparison, rotation
  restoration, measured A/V sync, and cancellation matrix on-device.

This gate does not add draggable preview handles, animated crop/keyframes,
Zoom, Mirror, Color, Freeze, Speed, transitions, Adaptive Edit, Audio, Overlay,
or Gemini. The task and device checks are recorded in
[`docs/tasks/PHASE6B2_CUSTOM_CROP.md`](docs/tasks/PHASE6B2_CUSTOM_CROP.md) and
[`docs/PHASE6B2_CUSTOM_CROP_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6B2_CUSTOM_CROP_ANDROIDIDE_VERIFICATION.md).

### Phase 6B.1 — Aspect ratio + Fit/Fill transform (device run confirmed)

Start Phase 6B with one reversible, typed transform slice. The existing Trim
and verified 720p/1080p path remain the baseline:

- [x] Add a master Transform switch that defaults to `Off`.
- [x] Preserve the selected aspect and scale mode while the master switch is off.
- [x] Add `Original`, `9:16`, `16:9`, and `1:1` aspect presets.
- [x] Add `Fit` (whole frame/letterbox) and `Fill` (center crop) scale modes.
- [x] Expose `Clips` and `Transform` as mutually exclusive Review Editor tabs
  so the scrolling sheet does not show both tool panels at once.
- [x] Compile enabled presets into deterministic 720p/1080p dimensions.
- [x] Prove by unit-test source that `Off` omits the optional transform entirely.
- [x] Map the compiled operation to Media3 `Presentation`; retain the verified
  short-side Presentation path when disabled or set to Original.
- [x] Restore Transform configuration across Activity recreation.
- [x] Restore the selected Review Editor tab across Activity recreation.
- [x] Size playback preview from the transformed output aspect when opening a
  completed render.
- [x] Keep Zoom, Mirror, Color, Freeze, Speed, Transition, Adaptive Edit,
  Overlay, Audio, and Gemini controls hidden.
- [x] Build/install `1.0-phase6b1` on the target device; owner screenshot on
  2026-08-20 confirmed Transform On, 9:16 Fill, and an active 1080p render.
- [ ] Verify Off regression, each aspect preset, Fit/Fill framing, 720p playback,
  1080p unlock/render, rotation, and configuration restoration on-device.

`Fit` may produce black bars to preserve every source pixel. `Fill` removes
bars by cropping centered frame edges. Neither mode stretches the image.
The implementation task and device matrix are recorded in
[`docs/tasks/PHASE6B1_ASPECT_FIT_FILL.md`](docs/tasks/PHASE6B1_ASPECT_FIT_FILL.md).

### Phase 6A.3.1 — One-third preview + localized underlay mask (device run confirmed)

The Phase 6A.3 target-device run verified the automatic device profile and
removed the fixed-ratio black area, but the portrait preview consumed too much
of the Editor viewport and the sheet-level blur affected text outside the
preview overlap. Correct only those presentation behaviors:

- [x] Cap preview height at exactly one third of the live Editor viewport.
- [x] Preserve the rotation-aware source aspect ratio within that cap.
- [x] Keep landscape preview width within the responsive horizontal margins.
- [x] Remove alpha and `RenderEffect` from the complete Editor sheet.
- [x] Restrict the progressive underlay mask to the exact preview rectangle.
- [x] Preserve the verified automatic device profile and Settings diagnostics.
- [x] Preserve Trim, Media3/FFmpeg, cancellation, 720p verification, and 1080p
  unlock behavior unchanged.
- [x] Build/install `1.0-phase6a3.1` on the target device; owner screenshot
  confirmed the corrected preview and 720p playback on 2026-08-20.
- [ ] Verify portrait/landscape size, swipe-under locality, rotation, and the
  Trim → 720p → playback → 1080p regression path.

This refinement deliberately avoids a sheet-wide blur because Android's view
`RenderEffect` applies to the whole target view, not only the geometric region
covered by the floating preview.

### Phase 6A.3 — Adaptive preview + automatic device profile (device run confirmed)

The Phase 6A.2 device run confirmed the three destinations and sticky Editor,
but exposed a fixed preview-card ratio that left unused black space around a
portrait source. Make the presentation responsive without changing media output:

- [x] Derive preview width and height from the probed, rotation-aware source ratio.
- [x] Constrain the overlay against the live Editor viewport instead of a fixed
  tablet rectangle, and center the `VideoView` inside its card.
- [x] Expand the preview according to a conservative `LIGHT`, `BALANCED`, or
  `HIGH` device capability profile.
- [x] Add a scroll-progress frosted dim scrim; use a subtle Android 12+ blur for
  balanced/high devices and the cheaper dim-only fallback for light devices.
- [x] Automatically inspect model/type, Android version, screen px/dp,
  CPU cores/ABI, RAM, app-volume storage, and active network status.
- [x] Show the read-only device profile and adaptive recommendation in Settings.
- [x] Keep Trim, Media3/FFmpeg, cancellation, 720p verification, and the 1080p
  unlock path unchanged.
- [x] Build/install `1.0-phase6a3` on the target Android device; owner
  screenshots confirmed the adaptive preview and populated Settings profile on
  2026-08-20.
- [ ] Verify portrait/landscape preview fit, swipe-under dim/blur, Settings
  values, tab switching, rotation, Trim, and 720p/1080p regressions.

The device check does not benchmark the CPU, measure internet speed, upload
telemetry, or automatically enable unimplemented editing operations.

### Phase 6A.2 — Sticky preview + scrolling Editor sheet (device run confirmed)

The three-destination shell now builds and runs on the target device, but the
tablet Editor's two-column composition makes the preview feel off-center and
gives metadata too much visual weight. Refine only the Editor presentation:

- [x] Center the video preview at the top as a pinned overlay.
- [x] Place metadata, Trim, render state, and output actions in one vertical
  scrolling sheet below the preview.
- [x] Draw the preview above the scrolling content so an upward swipe moves the
  sheet underneath it instead of moving the video away.
- [x] Show only filename and media summary by default; keep the full metadata
  grid behind an explicit `Video details` action.
- [x] Use one shared Editor layout contract for compact and `sw600dp` screens,
  with preview size and padding controlled by responsive dimensions.
- [x] Preserve every existing ViewBinding action and the Phase 6A Trim/render
  logic without changing `EditPlan`, Media3, FFmpeg, or render state.
- [x] Give the clean handoff a distinct `RecapFlowAI_Phase6A2` project identity,
  `1.0-phase6a2` version, visible toolbar marker, and source preflight script so
  AndroidIDE cannot silently present the older Phase 6A project as this gate.
- [x] Build/install the sticky-preview layout on the target Android device;
  owner screenshot confirmed the Phase 6A.2 shell on 2026-08-20.
- [ ] Verify swipe-under behavior, preview controls, details expansion, Trim,
  render progress, tab switching, and rotation on the target device.

This is a presentation-only refinement. It does not add a media overlay
operation, Gemini, new editor tools, or background render persistence.

### Phase 6A.1 — Home / Editor / Settings navigation shell (device build confirmed)

The Phase 6A Trim workflow is working on the target Android device. Before more
editing tools are exposed, split the single crowded workspace into three clear
top-level destinations without changing the verified media behavior:

- `Home` — import a video, show the active local project, and continue editing.
- `Editor` — preview, metadata, Clips/Trim, render progress, and output actions.
- `Settings` — current on-device processing and FFmpeg diagnostics only.

Navigation rules:

- [x] Use one native Activity shell with a Material bottom navigation bar.
- [x] Restore the selected destination across Activity recreation.
- [x] Android Back returns from Editor/Settings to Home before exiting.
- [x] Starting import navigates to Editor so preparation/probe progress remains visible.
- [x] Switching tabs must not cancel an active local render.
- [x] Pause preview playback when leaving Editor.
- [x] Keep unimplemented Transform, Audio, Overlay, and AI controls hidden.
- [x] Preserve the existing Trim/render code path without changing its operations.
- [x] Build/install this navigation shell on the target Android device; owner
  screenshot confirmed the three destinations on 2026-08-20.
- [ ] Verify all three destinations, Activity recreation, Back behavior, and tab
  switching during an active render on the target device.
- [ ] Regression-check Trim → 720p → playback unlock → 1080p after navigation.

This is an information-architecture refactor, not a Fragment, render-job, project
persistence, Gemini, or new editing-operation gate. A Navigation Rail remains a
later tablet refinement after the three-destination behavior is verified.

### Phase 6A — Typed EditPlan + Trim MVP (verified functional baseline)

- [x] Define a typed `EditPlan`; the UI never emits FFmpeg command strings.
- [x] Represent future adaptive/transform/audio/overlay features with explicit
  disabled defaults.
- [x] Validate source duration, trim bounds, and a 1-second minimum selection.
- [x] Add an Android Review Editor trim card with start/end selection and Reset.
- [x] Compile the trim range to Media3 `MediaItem.ClippingConfiguration`.
- [x] Preserve the Phase 5 H.264/AAC 720p → playback → 1080p render sequence.
- [x] Calculate realtime factor against planned clip duration, not full source.
- [x] Build/install on the target Android device; owner confirmed Trim and 720p
  playback unlock on 2026-08-20.
- [ ] Verify output duration within `max(100 ms, 3 frames)` of the plan.
- [ ] Verify A/V sync is within 100 ms on the target device.
- [ ] Verify cancellation removes incomplete output and preserves the source.

### Phase 6B — Transform controls

- [x] Scale and aspect-ratio presets (Phase 6B.1 source implemented).
- [x] Crop modes: original, fit, and fill (Phase 6B.1 source implemented).
- [x] Custom crop rectangle (Phase 6B.2 source implemented).
- [x] Live pre-render Crop/Aspect/Fit/Fill preview (Phase 6B.2.1 source implemented).
- [x] Horizontal Mirror Off/On with live preview/export parity (Phase 6B.3 source implemented).
- [x] Color Off/On, Brightness, Contrast, Saturation, Temperature, and Reset
  with live preview/export parity (Phase 6B.4 source implemented).
- [x] Zoom modes: off, in, out, and alternate (Phase 6B.5 source implemented).
- [x] Speed Off/On and constant presets with realtime preview/export parity
  (Phase 6B.6 source implemented).
- [x] Intro Freeze Off/On, 1/2/3-second presets, pre-render preview, and sequential
  image/source export (Phase 6B.7 source implemented).
- [x] Transition controls with explicit Off (Phase 6B.8 owner confirmed complete).
- [x] Mirror compiler tests proving Off omits the associated operation.
- [x] Color compiler tests proving Off and neutral values omit the operation.
- [x] Zoom compiler tests proving Transform Off and Zoom Off omit the operation.
- [x] Speed compiler tests proving Transform Off, Speed Off, and neutral `1×`
  omit the operation.
- [x] Freeze compiler tests proving Transform Off and Freeze Off omit the operation.
- [x] Transition compiler tests proving Off omits the associated operation.
- [ ] Compiler tests proving Off omits each remaining associated operation.

### Phase 6C — Adaptive editing

- [x] Deterministic pacing/cut inputs and typed cut suggestions (Phase 6C.1).
- [x] User review and explicit Apply before adaptive cuts alter the render plan.
- [x] Clip concatenate execution without cross-clip transitions (Phase 6C.1).
- [x] Continuous reviewed-sequence preview and per-clip fade-through-black/edge
  transition execution (Phase 6C.2 owner confirmed complete).
- [x] User-movable/resizable Editor preview with reset, bounds, localized underlay,
  and recreation state (Phase 6C.2.1 source implemented).
- [ ] Overlapping dissolve crossfade, if product testing still requires it.

### Phase 6D — Audio controls

- [x] Keep Original/Mute controls, realtime preview, and export execution
  (Phase 6D.1 source implemented).
- [x] Replace policy with local picker, synchronized preview, short-loop/long-trim
  duration policy, and composition export (Phase 6D.3 owner confirmed complete).
- [x] Mix policy with independent source/added gains, synchronized preview, stereo
  normalization, and composition export (Phase 6D.4 owner confirmed complete).
- [x] 0–100% Volume control with realtime preview/export parity
  (Phase 6D.2 owner confirmed complete; reused by Replace in Phase 6D.3).

### Phase 6E — Review Editor Overlay tab

- [x] Add `Overlay` as a tab inside Review Editor, not as top-level navigation.
- [x] Support a master switch plus a per-operation enable/disable control.
- [x] Support one manual source-subtitle blur rectangle, time range, strength,
  realtime preview, and export parity (Phase 6E.1 source implemented).
- [x] Add manual static image/logo selection, slider/preset position, aspect-preserving
  scale, opacity, time range, realtime preview, and export parity (Phase 6E.2 source implemented).
- [ ] Add optional static-image animation loops after Phase 6E.2 passes on-device.
- [ ] Revisit direct image drag/resize only after the disabled source-blur touch path has a
  complete crash trace and a stable gesture lifecycle design.
- [ ] Add manual video overlay asset selection, transform controls, audio policy,
  realtime preview, and duration behavior.
- [ ] Do not implement Gemini-generated overlay decisions in Phase 6.

The intended Review Editor tab order is `Clips`, `Transform`, `Audio`, `Overlay`,
and `Export`. A tab stays hidden until its matching executor is working.

## Priority order

1. [x] Trim source slice (Phase 6A source implementation; device verification pending)
2. [x] Scale
3. [x] Crop
4. [x] Aspect-ratio conversion
5. [x] Audio replace (Phase 6D.3 source implemented; device matrix pending)
6. [x] Volume adjustment (Phase 6D.2 source implemented; device matrix pending)
7. [x] Speed change
8. [x] Concatenate reviewed ranges from one source (Phase 6C.1 source implemented)
9. [ ] Image/video overlay
10. [ ] Subtitle/text rendering
11. [ ] Multi-stage filter graphs

## TikTok preset

Initial Recap Flow target:

```text
1080 x 1920
9:16
30 fps
H.264
AAC
MP4
```

## Important design rule

UI must never create arbitrary FFmpeg strings directly.

Use a typed edit model whose optional operations default to disabled:

```text
EditPlan
├── sourcePath + sourceDuration
├── profile
├── trimRange
├── adaptiveCuts(enabled)
├── transform(zoom/mirror/color/freeze/speed/transition)
├── audio(policy/volume/mixVolume/externalAsset)
├── overlays(enabled/items)
├── subtitles(enabled/items)
└── exportPreset
```

The media layer validates `EditPlan` and translates enabled operations into
Media3 or FFmpeg graph/API operations. Disabled operations are not compiled.

## Definition of done

Basic RecapFlow edits can be reproduced predictably from a stored edit plan, and
every optional transformative edit can be disabled by the user.

---

### Phase 6F.2.6.1A follow-up — paused logo live refresh (2026-08-27)

- [x] Preserve the successful rotation-aware 720p/1080p final-render validation.
- [x] Preserve one mutable EditPlan and one final Transformer export.
- [x] Keep image/logo geometry and opacity in `RealtimeImageOverlayState`; do not rebuild the effect graph for parameter-only changes.
- [x] Replace the ineffective same-position paused seek with a bounded two-frame refresh pulse so retained preview shaders redraw immediately.
- [x] Settle the playhead back to the original timestamp after control input becomes idle.
- [x] Refuse to restore the old timestamp when the user intentionally scrubbed elsewhere.
- [x] Add pure refresh-policy coverage and a source preflight.
- [ ] Device: verify X/Y/preset/size/opacity updates while the video is paused without resizing the floating preview.
- [ ] Device: verify final 1080p logo geometry matches the preview after the paused-frame fix.

**Rule:** this pulse is preview invalidation only. It must never create an intermediate video or transition the export coordinator into a render state.

### Phase 6F.2.6.1B follow-up — aspect-ratio preview surface rebind (2026-08-28)

- [x] Diagnose the owner-device 540x960 HEVC case where 9:16/16:9 Presentation leaves video clipped to the right side of the floating preview.
- [x] Treat aspect preset and Presentation FIT/FILL changes as geometry-critical rather than class-only retained-effect updates.
- [x] Resize the movable preview card first and wait for the TextureView bounds to settle before rebuilding the source preview graph.
- [x] Keep the playhead, playback intent, EditPlan, blur state, and logo state across the preview-only rebind.
- [x] Keep color/logo/blur parameter-only edits on the retained player; no render is introduced.
- [x] Add pure geometry-change policy coverage and a source preflight.
- [ ] Device: verify 9:16 -> 16:9 -> 1:1 -> Original while paused and playing on the reported HEVC source.
- [ ] Device: verify FIT/FILL, logo, blur, and one final 1080p export after repeated aspect switches.

**Rule:** a Presentation geometry change may rebuild only the live decoder/effect graph after surface layout settles. It must never render an intermediate video or replace the imported source.


### Phase 6F.2.6.2 — Full EditPlan Combination Regression (2026-08-28)

Status: **DONE — OWNER CONFIRMED 2026-08-28; BASELINE FREEZE/PUSH PENDING**

Purpose: freeze the one-EditPlan / one-final-render editor contract before migrating the verified
source into a new GitHub repository. This phase adds no second media pipeline and no new user-facing
effect. It verifies that Clips, Transform, Audio, Overlay and Export can all remain configured at the
same time without an intermediate render or destructive cross-tab state reset.

Implemented source work:

- Added `FullEditPlanCombinationRegressionTest` for a maximal plan containing reviewed Adaptive Cuts,
  9:16 Fill, Crop, Mirror, Color, Alternate Zoom, Intro Freeze, Speed, Fade, Audio Mix, source Blur,
  image/logo Overlay and one final quality preset.
- Added explicit regression checks that absolute Blur/Logo source windows project correctly into
  every adaptive clip, including later clips.
- Added master-switch regression checks: disabling Transform/Audio/Overlay omits the effect but
  preserves remembered child settings for later re-enable.
- Added 720p/1080p/2K isolation checks so the final preset changes output geometry/quality budget,
  not the reviewed edit duration/state.
- Removed an accidental duplicate `input = input` argument in `LocalRenderCoordinator` discovered
  while hardening the final Composition path.
- Added `scripts/verify_phase6f2_6_2_source.sh`, implementation status, and the exact AndroidIDE/device
  evidence contract.
- Project identity is now `RecapFlowAI_Phase6F2_6_2`; app version is `1.0-phase6f2.6.2`.

Owner-device acceptance gate — **PASS / owner confirmed 2026-08-28**:

1. [x] configure Clips + Transform + Audio + Overlay without rendering between tabs;
2. [x] revisit every tab and edit settings while the same imported source remains usable;
3. [x] confirm logo/aspect/blur hotfix behavior remains stable in the combined plan;
4. [x] perform one 1080p final render and retain topology, actual bitrate, A/V sync, duration and
   `1080x1920`/`1920x1080` validation evidence;
5. [x] confirm preset changes do not clear the edit;
6. [x] confirm cancellation/failure leaves the imported source and reviewed EditPlan intact.

The Phase 6F.2.6.2 baseline is now frozen on GitHub. The active gate is Phase 6F.2.7 AndroidIDE/device
verification for the feature-flagged CompositionPlayer preview. External FFmpegAndroid reference work
remains queued until the planned preview/overlay milestones are complete.



## GitHub Baseline Migration — 2026-08-28

Target repository: [`kothar-1992/RecapFlowAI-Android`](https://github.com/kothar-1992/RecapFlowAI-Android)

Repository workflow rules:

- `main` contains verified/stable code only.
- One Issue -> one `feature/`, `fix/`, or `hotfix/` branch -> one PR.
- Every implementation PR must update this `PLAN.md`.
- Do not commit API keys, tokens, keystores, `local.properties`, IDE state, APK/build outputs, or generated FFmpeg archives.
- Tag the first verified baseline as `phase-6f2.6.2-stable`.
- Do not start the external FFmpegAndroid reference integration before baseline issue #1 is complete.

Queued GitHub work:

- [x] [#1 Baseline: freeze verified Phase 6F.2.6.2 source](https://github.com/kothar-1992/RecapFlowAI-Android/issues/1) — merged to `main` as `7411b54ba922c49a28fde4ea7e0250b50d019900`; stable branch `stable/phase-6f2.6.2` created
- [ ] [#2 Phase 6F.2.7: CompositionPlayer live preview with explicit fallback](https://github.com/kothar-1992/RecapFlowAI-Android/issues/2)
- [ ] [#3 Phase 6G.1: Timed video overlay support](https://github.com/kothar-1992/RecapFlowAI-Android/issues/3)
- [ ] [#4 Phase 6G.2: Subtitle and text rendering pipeline](https://github.com/kothar-1992/RecapFlowAI-Android/issues/4)
- [ ] [#5 Phase 6G.3: Unified multi-stage edit graph](https://github.com/kothar-1992/RecapFlowAI-Android/issues/5)
- [ ] [#6 Phase 7: Persistent render job engine](https://github.com/kothar-1992/RecapFlowAI-Android/issues/6)
- [ ] [#7 Research: evaluate FFmpegAndroid patterns after stable baseline](https://github.com/kothar-1992/RecapFlowAI-Android/issues/7)

Baseline preflight evidence in the prepared Phase 6F.2.6.2 source:

- `scripts/verify_phase6f2_6_2_source.sh`: PASS on 2026-08-28.
- Secret-like source scan: no embedded API key, bot token, private key, or bearer credential detected.
- `.gitignore` already excludes Gradle/IDE state, `local.properties`, signing material, generated FFmpeg headers/static archives, and build outputs.
- Baseline merge SHA: `7411b54ba922c49a28fde4ea7e0250b50d019900`.
- Stable freeze branch: `stable/phase-6f2.6.2`.
- Git tag `phase-6f2.6.2-stable` remains a repository-maintenance follow-up; the stable branch already preserves the exact merged baseline.

### Phase 6F.2.6.1D — High-bitrate social-export quality hotfix

- [x] Raise H.264 reviewed-export request bands to 25–30 Mbps (720p), 30–45 Mbps (1080p), and 45–60 Mbps (2K).
- [x] Request MediaCodec CBR mode explicitly through Media3 `VideoEncoderSettings` while keeping encoder fallback enabled for device compatibility.
- [x] Keep reporting the actual encoder bitrate after export; flag <80% target as a quality shortfall and <50% as a severe quality shortfall.
- [x] Preserve exact output geometry, H.264/AAC validation, one-EditPlan/one-final-render behavior, Gallery publication, blur/logo timing, and aspect-preview hotfixes.
- [x] Clarify in the UI that exact 720p/1080p/2K dimensions do not recreate source detail when the input is lower-resolution or already compressed.
- [ ] AndroidIDE owner gate: render representative original sources at 720p, 1080p, and 2K; record requested/actual bitrate, source resolution, output size, visual sharpness, thermal behavior, and social-platform upload result.

### Phase 6F.2.6.1C follow-up — full-duration blur/logo timeline reconciliation (2026-08-28)

- [x] Diagnose the owner final-render sample where subtitle blur is visible in the first part of a long video but missing in the latter part.
- [x] Make untouched blur/logo time windows follow the current Trim instead of retaining a stale shorter initialization range.
- [x] Preserve explicit user time-window edits as manual source ranges.
- [x] Migrate pre-1C saved ranges with no intent flag back to Trim-linked default behavior.
- [x] Project absolute source overlay windows into every clipped Media3 sequence item.
- [x] Remove each item's Media3 sequence offset before blur/logo shaders evaluate the projected local time window.
- [x] Remove the invalid encoded-video `setDurationUs(clippedRange)` override; encoded input now uses intrinsic pre-clipping duration as required by Media3.
- [x] Apply the same timing correction to image/logo overlay so both manual overlays share one deterministic timeline contract.
- [x] Add pure Kotlin timeline/projection coverage and source preflight.
- [ ] Device: verify blur at 25/50/75/95% of the reported long source and final 1080p export.
- [ ] Device: verify at least three Adaptive Cut ranges, Intro Freeze, 0.5x/2x speed, and a deliberate manual blur time window.

**Rule:** Overlay controls store absolute source intent, preview remains non-destructive, and final Composition projects that intent per item. Editing must never create an intermediate MP4; final Export remains one Transformer pass.

# PHASE 7 — Render Job Engine

## Goal

Prevent long renders from being tied to one Activity instance.

## Proposed model

```text
RenderJob
├── id
├── projectId
├── state
├── progress
├── stage
├── startedAt
├── output
└── error
```

States:

```text
QUEUED
PREPARING
RENDERING
FINALIZING
COMPLETED
FAILED
CANCELLED
```

## Tasks

- [ ] Native progress callback.
- [ ] Kotlin progress flow/listener.
- [ ] Cancellation signal.
- [ ] Render error mapping.
- [ ] Partial-output cleanup.
- [ ] Activity recreation must not corrupt job state.
- [ ] Notification integration for long jobs.
- [ ] Add the correct media-processing foreground-service model when background rendering is enabled.

## Important

The current manifest contains:

```text
FOREGROUND_SERVICE_DATA_SYNC
```

Do not treat media transcoding as a generic data-sync job.

When long-running background rendering is implemented, update the manifest/service architecture for Android's media-processing foreground-service rules.

## Definition of done

A render can survive normal UI lifecycle changes and can be cancelled safely.

---

# PHASE 8 — Project Workspace + Local Persistence

## Goal

Move from "single render demo" to repeatable RecapFlow projects.

## Directory concept

```text
RecapFlowAI/
└── projects/
    └── <project-id>/
        ├── project.json
        ├── source/
        ├── audio/
        ├── subtitles/
        ├── preview/
        ├── temp/
        └── output/
```

## Project model

```text
RecapProject
├── id
├── title
├── platform
├── sourceMedia
├── targetDuration
├── script
├── narration
├── subtitles
├── editPlan
├── renderState
└── outputs
```

## Tasks

- [ ] Create/new project.
- [ ] Save project state.
- [ ] Reopen project.
- [ ] Auto-save important edits.
- [ ] Clear temp files without deleting projects.
- [ ] Detect missing source media.
- [ ] Export final media to user-visible storage.

## Definition of done

Closing and reopening the app does not lose project state.

---

# PHASE 9 — Full Native Recap Flow Workflow UI

## Goal

Expand the verified import/probe/render UI foundation into the complete native
workflow. Do not rebuild the Phase 4 screens from scratch.

## Screen order

```text
Home
 ↓
New Project
 ↓
Import Video
 ↓
Project Setup
 ↓
Analyze
 ↓
Script
 ↓
Narration / Voice
 ↓
Timeline
 ↓
Preview
 ↓
Render
 ↓
Export
```

## Recommended implementation

Remain with:

```text
Kotlin
XML
ViewBinding
Material Components
```

Do not migrate to Compose during the core media-engine build unless there is a concrete reason.

Use the Phase 4 Material 3 tokens, responsive layout rules, typed UI states, and
accessibility baseline consistently across every later destination.

## Definition of done

A complete local project can move through the UI without WebView.

---

# PHASE 10 — AI Layer

## Goal

Add AI only after the local video engine is stable.

## Services

### Transcription

```text
Source audio
 ↓
Transcription service
 ↓
Timed transcript
```

### Script generation

```text
Transcript
 ↓
Recap rules
 ↓
Narrator script
```

### TTS

```text
Narrator script
 ↓
TTS
 ↓
Narration audio
```

## Security rules

- [ ] Never commit API keys.
- [ ] Do not hard-code secrets into source files.
- [ ] Store user credentials/settings with Android-appropriate protected storage.
- [ ] Separate provider configuration from project content.
- [ ] Redact sensitive values from logs.

## Definition of done

The app can create narration assets while all media rendering remains local.

---

# PHASE 11 — ATS / Timeline Engine

## Goal

Replace simple whole-video compression with timeline-aware recap editing.

## Pipeline

```text
Transcript / Script
      ↓
Story segmentation
      ↓
ATS Timeline
      ↓
Segment selection
      ↓
Duration budgeting
      ↓
EditPlan
      ↓
FFmpeg engine
      ↓
Final video
```

## Segment model

```text
TimelineSegment
├── sourceStartMs
├── sourceEndMs
├── targetDurationMs
├── importance
├── narrationRange
├── transition
└── enabled
```

## Rules

- [ ] Preserve story order unless explicitly planned otherwise.
- [ ] Avoid accidental removal of story-critical ending material.
- [ ] Duration must come from segment planning, not blind global speed-up.
- [ ] Timeline should be inspectable before final render.
- [ ] A manual confirmation gate should exist before expensive processing.

## Definition of done

Recap length is controlled by planned segments rather than only whole-video time compression.

---

# PHASE 12 — Subtitle + Burmese Text Pipeline

## Goal

Produce reliable Burmese text rendering.

## Requirements

- [ ] Unicode Burmese.
- [ ] Correct line wrapping.
- [ ] Safe margins for 9:16.
- [ ] Timing from SRT/ATS.
- [ ] Configurable size and position.
- [ ] Preview/render consistency.
- [ ] TTS-friendly spoken-number rules remain a script concern, not subtitle corruption.
- [ ] Text assets/fonts must have appropriate redistribution rights if bundled.

## Definition of done

Burmese subtitles render correctly on exported video.

---

# PHASE 13 — Performance Optimization

## Goal

Make 5–10 minute mobile renders practical.

## Optimization order

1. [ ] Avoid redundant decode/encode stages.
2. [ ] Hardware encoder capability detection.
3. [ ] Reuse intermediate assets.
4. [ ] Avoid unnecessary copies of large input files.
5. [ ] Reduce temporary disk usage.
6. [ ] Use bounded native buffers.
7. [ ] Prevent memory leaks across repeated renders.
8. [ ] Add thermal-aware quality recommendations if needed.
9. [ ] Consider an explicitly optional 720p proxy preview only if device benchmarks justify it.
10. [x] Keep optional playback independent from the user-selected 720p/1080p/2K final export.

## Definition of done

Repeated renders do not progressively increase memory usage and the device remains stable.

---

# PHASE 14 — Export + Reliability

## Export presets

### TikTok

```text
9:16
1080x1920
H.264
AAC
MP4
```

### YouTube / Facebook

Additional presets can be added only after TikTok export is proven.

## Reliability tests

- [ ] Source has no audio.
- [ ] Source has variable frame rate.
- [ ] Source is rotated.
- [ ] Source is portrait.
- [ ] Source is landscape.
- [ ] Large file.
- [ ] Cancel at 10%, 50%, 90%.
- [ ] App background/foreground transition.
- [ ] Storage almost full.
- [ ] Input removed after project creation.
- [ ] Encoder unavailable/fails.
- [ ] Device restarts after completed project save.

---

# PHASE 15 — Release Hardening

## Tasks

- [ ] Remove diagnostic native functions.
- [ ] Release build.
- [ ] R8/ProGuard validation.
- [ ] Native symbol strategy.
- [ ] Crash logging without secrets.
- [ ] FFmpeg license notices and build configuration documentation.
- [ ] Validate third-party codec/license choices before distribution.
- [ ] Validate native `.so` packaging.
- [ ] Validate 16 KB page-size compatibility.
- [ ] Clean install test.
- [ ] Upgrade test.
- [ ] Final performance benchmark.

## Definition of done

A clean APK can be installed on the target device and complete the end-to-end workflow without VPS processing.

---

# 4. Proposed Native API Surface

Do not expose every FFmpeg primitive directly to Activities.

Use a small controlled interface.

```text
NativeMediaBridge
│
├── nativeVersion()
├── ffmpegVersion()
├── probe(input)
├── validateEncoder()
├── render(editPlan, output)
├── cancel(jobId)
└── cleanup(jobId)
```

Later:

```text
extractAudio()
generateThumbnailFrame()
renderPreview()
```

---

# 5. Threading Rule

Never execute native rendering on the Android main/UI thread.

```text
UI Thread
   ↓
Job Controller
   ↓
Worker/Service Thread
   ↓
JNI
   ↓
FFmpeg
```

Progress returns asynchronously to the UI.

---

# 6. Error Model

Do not return only `true/false`.

Use structured results.

```text
NativeResult
├── code
├── stage
├── message
├── ffmpegError
└── recoverable
```

Example categories:

```text
INPUT_OPEN_FAILED
UNSUPPORTED_CODEC
ENCODER_NOT_FOUND
FILTER_INIT_FAILED
OUTPUT_CREATE_FAILED
STORAGE_FULL
CANCELLED
NATIVE_ERROR
```

---

# 7. Logging

Native logs must use Android logging and an internal RecapFlow tag.

Suggested tags:

```text
RecapFlowNative
RecapFlowRender
RecapFlowStorage
RecapFlowAI
```

Production logs must not expose:

- API keys
- authentication tokens
- full secret configuration
- private credentials

---

# 8. Git / Issue / PR Strategy

Create the new GitHub repository only after the baseline is frozen.

Suggested repository:

```text
RecapFlowAI-Android
```

## Suggested first issues

### Issue 1
`Establish native Android baseline and build guardrails`

### Issue 2
`Integrate ARM64 FFmpeg libraries with JNI/CMake`

### Issue 3
`Add FFmpeg runtime diagnostics and media probe`

### Issue 4
`Implement local video import and project workspace`

### Issue 5
`Implement first H.264/AAC local render`

### Issue 6
`Phase 6A: add typed EditPlan and user-controlled Trim MVP`

Tracking: https://github.com/ZeusOwner/recapflow-ai/issues/20

Acceptance:

- Review Editor exposes only the implemented Clips/Trim surface.
- Reset restores the full source range.
- Invalid or sub-1-second ranges cannot start a render.
- The rendered MP4 uses the selected range and preserves source audio.
- The 720p output must be played before the 1080p test is unlocked.
- Cancel deletes only the incomplete output.

Follow-up issues split Phase 6B–6E into transform, adaptive cut, audio, and
manual overlay gates instead of combining them into one unreviewable change.

### Issue 7
`Add render progress, cancellation and job state`

### Issue 8
`Add local project persistence`

### Issue 9
`Build native Recap Flow workflow screens`

### Issue 10
`Integrate transcription, script generation and TTS`

### Issue 11
`Implement ATS timeline and duration planner`

### Issue 12
`Implement Burmese subtitle rendering`

### Issue 13
`Performance and thermal optimization`

### Issue 14
`Release hardening and device regression tests`

## PR rule

One functional gate per PR.

Do not combine:

```text
FFmpeg integration
+
full UI redesign
+
AI pipeline
+
timeline engine
```

into one PR.

---

# 9. Immediate Next Milestone

The current implementation work stops at this exact gate:

```text
Import and probe one local video
    ↓
Build a typed EditPlan
    ↓
Review edits and open Export
    ↓
Choose exactly 720p, 1080p, or 2K
    ↓
Render one final H.264 output
    ↓
Validate dimensions/codec/audio/duration
    ↓
Publish to Gallery; preview is optional
```

### Milestone acceptance criteria

All must pass:

- [x] Project builds in Android Code Studio.
- [x] App installs on Mi Pad.
- [x] App launches.
- [x] JNI bridge loads.
- [x] FFmpeg runtime loads and reports version `9.0.1`.
- [x] Input video can be selected.
- [x] FFmpeg metadata probe works.
- [x] Phase 5 720p/1080p local render is owner-confirmed complete.
- [x] Typed EditPlan and validation are implemented for Phase 6A.
- [x] Review Editor Trim UI is implemented with Reset.
- [x] Selected trim is compiled into the local Media3 render.
- [x] Export offers exactly 720p, 1080p, and 2K with no 4K control.
- [x] Selected quality is persisted and drives one final render action.
- [x] Playback no longer gates or unlocks another render.
- [x] Completed state requires exact short-side, H.264, audio-policy, and bounded duration validation.
- [ ] 720p trimmed output duration matches the Phase 6F.2.5 calculated tolerance.
- [ ] Trimmed output audio remains synchronized within 100 ms.
- [x] Historical 1080p playback-unlock path was owner-confirmed on 2026-08-20 and is now
  superseded by the Phase 6F.2.3 direct quality selector.
- [ ] Cancellation cleanup and source preservation pass on-device.
- [x] No VPS is involved in the Phase 6A media path.

Only after these checks pass should Phase 6B transform controls be exposed.

---

# 10. Key Baseline Evolution

Completed Phase 1 baseline:

```text
MainActivity
    ↓
NativeMediaBridge.kt
    ↓
recapflow_jni.cpp
    ↓
MediaEngine
```

Target after the first media milestone:

```text
MainActivity / Diagnostics UI
          ↓
NativeMediaBridge.kt
          ↓
recapflow_jni.cpp
          ↓
MediaEngine
          ↓
FFmpeg
          ↓
Local MP4
```

This preserves the currently working native foundation while creating a clean path toward the complete Recap Flow AI Android application.

---

# 11. Definition of Project Success

Recap Flow AI Android is considered independent from the VPS when the following pipeline works on-device:

```text
Import video
    ↓
Analyze locally
    ↓
Create/edit project
    ↓
Generate AI assets through API when needed
    ↓
Build ATS/EditPlan
    ↓
Render locally with FFmpeg
    ↓
Preview
    ↓
Export final MP4
```

The VPS must not be required for:

- video import
- probing
- trimming
- crop/scale
- speed processing
- audio replacement
- timeline rendering
- subtitle burn-in
- final video export

AI providers may remain online services.

---

## Status

- **Baseline:** BUILD + DEVICE RUNTIME VERIFIED WITH NDK 24
- **Native JNI path:** PHASE 1 VERIFIED
- **CMake integration:** VERIFIED WITH CMAKE 3.18.1
- **FFmpeg:** 9.0.1 ARM64 BUILD, STATIC LINK, APK LAUNCH, AND RUNTIME VERSION VERIFIED
- **Phase 3:** CORE VERSION SMOKE TEST VERIFIED; CONFIGURATION/ENCODER DIAGNOSTICS PENDING
- **Phase 5:** OWNER-CONFIRMED COMPLETE ON 2026-08-20
- **Phase 6B.1:** DEVICE BUILD/RUN CONFIRMED; FULL OFF/FIT/FILL MATRIX PENDING
- **Phase 6B.2:** DEVICE BUILD/RUN + 1080P CUSTOM-CROP OUTPUT CONFIRMED; FULL MATRIX PENDING
- **Phase 6B.2.1:** OWNER-CONFIRMED COMPLETE ON 2026-08-22; FULL MATRIX PENDING
- **Phase 6B.3:** OWNER-CONFIRMED COMPLETE ON 2026-08-22; FULL MATRIX PENDING
- **Phase 6B.4:** OWNER-CONFIRMED COMPLETE ON 2026-08-22; FULL MATRIX PENDING
- **Phase 6B.5:** OWNER-CONFIRMED COMPLETE ON 2026-08-22; FULL MATRIX PENDING
- **Phase 6B.6:** OWNER-CONFIRMED COMPLETE ON 2026-08-22; FULL MATRIX PENDING
- **Phase 6B.7:** INTRO FREEZE SOURCE IMPLEMENTED; DEVICE MATRIX PENDING
- **Phase 6B.7.1:** OWNER-CONFIRMED COMPLETE ON 2026-08-24; FULL MATRIX PENDING
- **Phase 6B.8:** OWNER-CONFIRMED COMPLETE ON 2026-08-24; FULL MATRIX PENDING
- **Phase 6C.1:** OWNER-CONFIRMED COMPLETE ON 2026-08-24; FULL MATRIX PENDING
- **Phase 6C.2:** OWNER-CONFIRMED COMPLETE ON 2026-08-25; FULL MATRIX PENDING
- **Phase 6C.2.1:** OWNER-CONFIRMED COMPLETE ON 2026-08-25; FULL MATRIX PENDING
- **Phase 6D.1:** OWNER-CONFIRMED COMPLETE ON 2026-08-25; FULL MATRIX PENDING
- **Phase 6D.2:** OWNER-CONFIRMED COMPLETE ON 2026-08-25; FULL MATRIX PENDING
- **Phase 6D.3:** OWNER-CONFIRMED COMPLETE ON 2026-08-25; FULL MATRIX PENDING
- **Phase 6D.4:** OWNER-CONFIRMED COMPLETE ON 2026-08-25; FULL MATRIX PENDING
- **Phase 6E.1.5:** OWNER-CONFIRMED COMPLETE ON 2026-08-26; FULL MATRIX PENDING
- **Phase 6E.2:** OWNER CONFIRMED LOGO IMPORT/PREVIEW; STALE CONTROL GEOMETRY FOUND ON DEVICE
- **Phase 6E.3A:** OWNER-CONFIRMED VERSION STACK ADOPTED IN LATEST SOURCE; MERGED BUILD/DEVICE MATRIX PENDING
- **Phase 6E.3B:** OWNER-CONFIRMED COMPLETE ON 2026-08-27; EXTENDED DEVICE MATRIX PENDING
- **Phase 6F.1:** OWNER-CONFIRMED COMPLETE ON 2026-08-27; EXTENDED API 28/API 29+ MATRIX PENDING
- **Phase 6F.1.1:** OWNER-CONFIRMED COMPLETE ON 2026-08-27; EXTENDED QUALITY MATRIX PENDING
- **Phase 6F.1.1.1:** OWNER-CONFIRMED COMPILE CORRECTION COMPLETE ON 2026-08-27
- **Phase 6F.2:** SAVE/RESTORE/RESET SOURCE IMPLEMENTED; ANDROIDIDE/DEVICE MATRIX PENDING
- **Phase 6F.2.1:** SOURCE-BLUR GHOSTING HOTFIX IMPLEMENTED; ANDROIDIDE/DEVICE MATRIX PENDING
- **Phase 6F.2.2:** VIDEO GALLERY PICKER SOURCE IMPLEMENTED; ANDROIDIDE/DEVICE MATRIX PENDING
- **Phase 6F.2.3:** 720P/1080P/2K PRODUCTION RENDER QUALITY SOURCE IMPLEMENTED; ANDROIDIDE/DEVICE MATRIX PENDING
- **Phase 6F.2.4:** PREVIEW FALLBACK SEPARATION SOURCE IMPLEMENTED; ANDROIDIDE/DEVICE MATRIX PENDING
- **Phase 6F.2.5:** OWNER-CONFIRMED COMPLETE ON 2026-08-27; EXTENDED DURATION/OUTPUT MATRIX PENDING
- **Phase 6F.2.6:** SHARED MEDIA3 COMPOSITION WORKFLOW SOURCE IMPLEMENTED; BASE DEVICE PATH PARTIALLY VERIFIED THROUGH 6F.2.6.1 HOTFIX LINE
- **Phase 6F.2.6.1 base:** OWNER CONFIRMED 1080P EXACT RENDER + CROSS-TAB EDITING/LIVE PREVIEW RECOVERY
- **Phase 6F.2.6.1A:** OWNER CONFIRMED LOGO LIVE-REFRESH FIX
- **Phase 6F.2.6.1B:** ASPECT-RATIO LIVE-PREVIEW HOTFIX SOURCE IMPLEMENTED; COMBINED DEVICE REGRESSION INCLUDED IN CURRENT GATE
- **Phase 6F.2.6.1C:** FULL-DURATION BLUR/LOGO TIMELINE HOTFIX SOURCE IMPLEMENTED; COMBINED DEVICE REGRESSION INCLUDED IN CURRENT GATE
- **Phase 6F.2.6.1D:** HIGH-BITRATE 720P/1080P/2K EXPORT POLICY SOURCE IMPLEMENTED; ACTUAL DEVICE BITRATE EVIDENCE PENDING
- **Phase 6F.2.6.2:** OWNER-CONFIRMED DONE; VERIFIED BASELINE MERGED TO GITHUB `main`
- **Phase 6F.2.7:** COMPOSITIONPLAYER FEATURE-FLAG PREVIEW SOURCE IMPLEMENTED; ANDROIDIDE/DEVICE MATRIX PENDING
- **Current gate:** PHASE 6F.2.7 COMPOSITIONPLAYER PREVIEW DEVICE VERIFICATION
- **Next app gate after PASS:** MERGE PHASE 6F.2.7 PR, THEN PHASE 6G.1 TIMED VIDEO OVERLAY
- **Queued preview gate:** EXOPLAYER LIVE-EFFECTS/SOURCE-ONLY FALLBACK REGRESSION UNDER PHASE 6F.2.7
- **Queued quality gate:** PHASE 6F.1.1.1 ANDROIDIDE + ORIGINAL-SOURCE 720P/1080P COMPARISON
- **Queued export gate:** PHASE 6F.1 EXTENDED API 28/API 29+ DEVICE VERIFICATION
- **Queued persistence gate:** PHASE 6F.2 ANDROIDIDE + DEVICE VERIFICATION
- **Deferred media gate:** PHASE 6E.2.3 OPTIONAL IMAGE ANIMATION LOOPS AFTER STATIC PARITY AND PREVIEW STABILITY
- **Media verification:** HISTORICAL TRIM/720P/1080P PATH OWNER-CONFIRMED; NEW DIRECT 720P/1080P/2K QUALITY, DURATION/A-V, AND CANCELLATION MATRIX PENDING
- **VPS dependency removal:** TARGET ARCHITECTURE; CORE MEDIA PATH REMAINS LOCAL

Phase 6E.1 source evidence and the exact remaining AndroidIDE/device checks are in
[`docs/IMPLEMENTATION_STATUS_PHASE6E1_2026-08-25.md`](docs/IMPLEMENTATION_STATUS_PHASE6E1_2026-08-25.md)
and
[`docs/PHASE6E1_SOURCE_SUBTITLE_BLUR_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6E1_SOURCE_SUBTITLE_BLUR_ANDROIDIDE_VERIFICATION.md).

Phase 6E.3B retained-preview source evidence and its exact AndroidIDE/device checks are in
[`docs/IMPLEMENTATION_STATUS_PHASE6E3B_2026-08-27.md`](docs/IMPLEMENTATION_STATUS_PHASE6E3B_2026-08-27.md)
and
[`docs/PHASE6E3B_REALTIME_PREVIEW_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6E3B_REALTIME_PREVIEW_ANDROIDIDE_VERIFICATION.md).

Phase 6F.1 public-export source evidence and its exact AndroidIDE/device checks are in
[`docs/IMPLEMENTATION_STATUS_PHASE6F1_2026-08-27.md`](docs/IMPLEMENTATION_STATUS_PHASE6F1_2026-08-27.md)
and
[`docs/PHASE6F1_PUBLIC_EXPORT_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F1_PUBLIC_EXPORT_ANDROIDIDE_VERIFICATION.md).

Phase 6F.1.1 render-quality diagnosis, source evidence, and owner-device comparison steps are in
[`docs/IMPLEMENTATION_STATUS_PHASE6F1_1_2026-08-27.md`](docs/IMPLEMENTATION_STATUS_PHASE6F1_1_2026-08-27.md)
and
[`docs/PHASE6F1_1_RENDER_QUALITY_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F1_1_RENDER_QUALITY_ANDROIDIDE_VERIFICATION.md).

Phase 6F.2 persistence scope, source evidence, and device checks are in
[`docs/tasks/PHASE6F2_EDITOR_PREFERENCES.md`](docs/tasks/PHASE6F2_EDITOR_PREFERENCES.md),
[`docs/IMPLEMENTATION_STATUS_PHASE6F2_2026-08-27.md`](docs/IMPLEMENTATION_STATUS_PHASE6F2_2026-08-27.md),
and
[`docs/PHASE6F2_EDITOR_PREFERENCES_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_EDITOR_PREFERENCES_ANDROIDIDE_VERIFICATION.md).

Phase 6F.2.1 blur-quality diagnosis, source evidence, and device checks are in
[`docs/tasks/PHASE6F2_1_SOURCE_BLUR_QUALITY.md`](docs/tasks/PHASE6F2_1_SOURCE_BLUR_QUALITY.md),
[`docs/IMPLEMENTATION_STATUS_PHASE6F2_1_2026-08-27.md`](docs/IMPLEMENTATION_STATUS_PHASE6F2_1_2026-08-27.md),
and
[`docs/PHASE6F2_1_SOURCE_BLUR_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_1_SOURCE_BLUR_ANDROIDIDE_VERIFICATION.md).

Phase 6F.2.2 gallery-picker scope, source evidence, and device checks are in
[`docs/tasks/PHASE6F2_2_VIDEO_GALLERY_PICKER.md`](docs/tasks/PHASE6F2_2_VIDEO_GALLERY_PICKER.md),
[`docs/IMPLEMENTATION_STATUS_PHASE6F2_2_2026-08-27.md`](docs/IMPLEMENTATION_STATUS_PHASE6F2_2_2026-08-27.md),
and
[`docs/PHASE6F2_2_VIDEO_GALLERY_PICKER_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_2_VIDEO_GALLERY_PICKER_ANDROIDIDE_VERIFICATION.md).

Phase 6F.2.3 production-quality scope, source evidence, and device checks are in
[`docs/tasks/PHASE6F2_3_PRODUCTION_RENDER_QUALITY.md`](docs/tasks/PHASE6F2_3_PRODUCTION_RENDER_QUALITY.md),
[`docs/IMPLEMENTATION_STATUS_PHASE6F2_3_2026-08-27.md`](docs/IMPLEMENTATION_STATUS_PHASE6F2_3_2026-08-27.md),
and
[`docs/PHASE6F2_3_PRODUCTION_RENDER_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_3_PRODUCTION_RENDER_ANDROIDIDE_VERIFICATION.md).

Phase 6F.2.4 preview-capability scope, source evidence, and device checks are in
[`docs/tasks/PHASE6F2_4_PREVIEW_FALLBACK_SEPARATION.md`](docs/tasks/PHASE6F2_4_PREVIEW_FALLBACK_SEPARATION.md),
[`docs/IMPLEMENTATION_STATUS_PHASE6F2_4_2026-08-27.md`](docs/IMPLEMENTATION_STATUS_PHASE6F2_4_2026-08-27.md),
and
[`docs/PHASE6F2_4_PREVIEW_FALLBACK_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_4_PREVIEW_FALLBACK_ANDROIDIDE_VERIFICATION.md).

Phase 6F.2.5 duration-reconciliation scope, source evidence, and device checks are in
[`docs/tasks/PHASE6F2_5_DURATION_RECONCILIATION.md`](docs/tasks/PHASE6F2_5_DURATION_RECONCILIATION.md),
[`docs/IMPLEMENTATION_STATUS_PHASE6F2_5_2026-08-27.md`](docs/IMPLEMENTATION_STATUS_PHASE6F2_5_2026-08-27.md),
and
[`docs/PHASE6F2_5_DURATION_RECONCILIATION_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_5_DURATION_RECONCILIATION_ANDROIDIDE_VERIFICATION.md).

Phase 6F.2.6 shared-composition scope, source evidence, and device checks are in
[`docs/tasks/PHASE6F2_6_SHARED_MEDIA3_COMPOSITION.md`](docs/tasks/PHASE6F2_6_SHARED_MEDIA3_COMPOSITION.md),
[`docs/IMPLEMENTATION_STATUS_PHASE6F2_6_2026-08-27.md`](docs/IMPLEMENTATION_STATUS_PHASE6F2_6_2026-08-27.md),
and
[`docs/PHASE6F2_6_SHARED_MEDIA3_COMPOSITION_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_6_SHARED_MEDIA3_COMPOSITION_ANDROIDIDE_VERIFICATION.md).

Phase 6F.2.6.2 full-combination regression scope, source evidence, and the pre-GitHub baseline gate are in
[`docs/tasks/PHASE6F2_6_2_FULL_EDITPLAN_COMBINATION_REGRESSION.md`](docs/tasks/PHASE6F2_6_2_FULL_EDITPLAN_COMBINATION_REGRESSION.md),
[`docs/IMPLEMENTATION_STATUS_PHASE6F2_6_2_2026-08-28.md`](docs/IMPLEMENTATION_STATUS_PHASE6F2_6_2_2026-08-28.md),
and
[`docs/PHASE6F2_6_2_FULL_EDITPLAN_ANDROIDIDE_VERIFICATION.md`](docs/PHASE6F2_6_2_FULL_EDITPLAN_ANDROIDIDE_VERIFICATION.md).
