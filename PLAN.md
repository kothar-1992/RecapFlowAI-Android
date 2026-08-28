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
- **Current gate:** Phase 6F.2.7 OWNER-DEVICE PASS — exact tested source synchronized to PR #10; final diff review/merge pending. Phase 6F.2.8 social export quality is the next active gate after merge.
- **GitHub repository:** [`kothar-1992/RecapFlowAI-Android`](https://github.com/kothar-1992/RecapFlowAI-Android)
- **Current GitHub task:** [#2 Phase 6F.2.7 CompositionPlayer live preview with explicit fallback](https://github.com/kothar-1992/RecapFlowAI-Android/issues/2)
- **Verified Phase 6F.2.7 source commit:** `5042d7012ad309aea511d66661efcc4dd10b5522`

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

UI work must not hide media-engine state. Every major editor operation needs a visible state, a recoverable failure path, and a testable capability boundary.

---

## 4. Completed Milestones

### Phase 4 — UI/UX Foundation + Local MP4 Import/Probe — DONE

- Native Home / Editor / Settings foundation.
- Local MP4 import and probe.
- Runtime permission handling/fallback.
- Device diagnostics groundwork.

### Phase 5 — Local Render Foundation — DONE

- Local FFmpeg/Media3 render plumbing proven.
- One-final-render direction established.

### Phase 6A–6E — Editor controls / overlay / subtitle blur foundation — DONE

- Multi-tab native editor.
- Transform, transitions, manual overlay controls.
- Subtitle blur and overlay positioning.
- Crash mitigations and geometry corrections.

### Phase 6F.2.6.2 — Full EditPlan Combination Regression — DONE

Verified baseline commit on `main`:
`7411b54ba922c49a28fde4ea7e0250b50d019900`

The baseline proves that Clips, Transform, Audio, Overlay and Export remain independently editable, accumulate into a single `EditPlan`, and do not force an intermediate render for each edit.

### Phase 6F.2.7 — CompositionPlayer live preview + fallback — OWNER-DEVICE PASS

Architecture:

```text
EditPlan
   ↓
Shared Media3 Composition Plan
   ├── CompositionPlayer live preview
   └── Transformer final export
```

Verified behavior/source contract:

- CompositionPlayer is feature-gated and preferred for supported realtime preview paths.
- ExoPlayer live-effects fallback remains available.
- Source-only preview remains the final fallback.
- Trim / Adaptive Cuts / Speed source↔output timeline mapping preserves semantic playhead position.
- CompositionPlayer receives explicit original encoded duration before clipping.
- Intro Freeze and adaptive candidate/sequence inspection keep explicit ExoPlayer fallback behavior in this gate.
- Audio preview ownership prevents duplicate replacement/mix audio.
- Blur/logo changes can force in-memory composition refresh without intermediate MP4 output.
- Final export remains one authoritative `Transformer.start(compiledComposition.composition, ...)`.
- AndroidIDE build/device matrix reported PASS by the owner on 2026-08-28.
- Exact tested source synchronized to PR #10 as commit `5042d7012ad309aea511d66661efcc4dd10b5522`.

Merge gate: final PR #10 diff review, then merge and close Issue #2.

---

## 5. Immediate Next Gate — Phase 6F.2.8 Social Export Quality

Issue/PR track: Issue #11 / draft PR #12.

Goal: replace the temporary oversized social-export bitrate policy with source-frame-rate-aware H.264/VBR targets while preserving the one-final-render architecture.

Planned/partially implemented targets:

- 720p: 5 Mbps standard frame rate / 7.5 Mbps high frame rate.
- 1080p: 8 Mbps standard frame rate / 12 Mbps high frame rate.
- 1440p: 16 Mbps standard frame rate / 24 Mbps high frame rate.
- Preserve practical source frame-rate class up to 60 fps; normalize common fractional rates.
- Do not synthesize 60 fps from a 24/25/30 fps source.
- Switch final encoder request from CBR to VBR.
- Inspect/validate actual output frame rate.
- Reconcile PR #12 on top of merged Phase 6F.2.7 because both touch `Media3CompositionCompiler`.
- AndroidIDE/unit/device matrix required before merge.

---

## 6. Planned Editor / Render Roadmap

### Phase 6G.1 — Timed video overlay

- Multi-video/picture-in-picture overlay sequence.
- Source-time aware start/end ranges.
- Shared preview/export plan.
- No per-edit intermediate MP4 render.

### Phase 6G.2 — Subtitle/Text rendering

- Text/subtitle model and timing.
- Burmese Unicode/wrapping/safe margins.
- SRT/ATS integration direction.
- Font licensing tracked explicitly.

### Phase 6G.3 — Unified multi-stage edit graph

- Consolidate clips, transform, audio, blur/logo, video overlay and text into one graph contract.
- Preserve one final render.
- Expand parity/regression matrix.

### Phase 7 — Persistent Render Job Engine

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

Required behavior:

- progress reporting;
- cancellation;
- meaningful error propagation;
- partial output cleanup;
- activity recreation survival;
- notification state;
- proper media-processing foreground-service model rather than a generic data-sync service.

### Phase 8 — Project Workspace / persistence

Persist project source, EditPlan, imported auxiliary assets, preview state and export history.

### Phase 9 — Native RecapFlow workflow UI

```text
Home
 → New Project
 → Import
 → Setup
 → Analyze
 → Script
 → Narration / Voice
 → Timeline
 → Preview
 → Render
 → Export
```

### Phase 10 — AI layer

- Transcription.
- Script planning/rewrite.
- TTS/narration orchestration.
- AI operations may use cloud APIs; media rendering remains local.

### Phase 11 — ATS / timeline intelligence

- Preserve story order and ending.
- Automated segment/timeline suggestions.
- Manual confirmation/edit boundary.

### Phase 12 — Subtitle + Burmese production hardening

- Burmese Unicode correctness.
- Wrapping and safe margins.
- SRT/ATS flows.
- Font licensing/packaging.

### Phase 13 — Performance optimization

- Codec/device capability tuning.
- Memory and thermal behavior.
- MediaCodec/FFmpeg runtime profiling.

### Phase 14 — Reliability

- Recovery, invalid media, low storage, cancellation and lifecycle cases.

### Phase 15 — Release hardening

- 16 KB page-size compatibility.
- ABI/release packaging.
- Third-party notices/licensing.
- Signing/release checks.

---

## 7. Engineering Rules

1. `EditPlan` is the authoritative typed immutable edit state.
2. Clips, Transform, Audio, Overlay and Export remain independently editable.
3. Turning on one feature must never force a render before another feature can be edited.
4. Preview is disposable/recoverable; final export is authoritative.
5. Preview failure must never invalidate final render availability.
6. No hidden intermediate MP4 workflow for ordinary editing.
7. Final export is one authoritative composition/render pass.
8. Feature gates and fallbacks must be explicit.
9. `PLAN.md` must be updated with every phase/PR status change.
10. Device verification is required before a media-pipeline PR is marked DONE.
11. Do not claim a build/device PASS unless it was actually observed or explicitly reported by the owner.
12. Third-party code/license provenance must be tracked before shipping.

---

## 8. Current Action Order

1. Final-review PR #10 against the synchronized AndroidIDE-tested Phase 6F.2.7 source.
2. Merge PR #10 and close Issue #2.
3. Rebase/reconcile Phase 6F.2.8 PR #12 on merged `main`.
4. Finish VBR request + output-FPS inspection/validation + UI/telemetry wording.
5. Prepare Phase 6F.2.8 AndroidIDE test package.
6. Run 30 fps and 60 fps device export matrix.
7. Merge Phase 6F.2.8 only after device PASS.
8. Continue Phase 6G.1 / Phase 7 roadmap as scheduled.
