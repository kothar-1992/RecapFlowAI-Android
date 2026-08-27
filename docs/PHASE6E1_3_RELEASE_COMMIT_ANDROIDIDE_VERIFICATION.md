# Phase 6E.1.3 Release Commit Deferral — AndroidIDE / LogWire Verification

## Build

```bash
bash scripts/verify_phase6e1_source.sh
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

Confirm the project is `RecapFlowAI_Phase6E1_3` and the installed version is
`1.0-phase6e1.3`. Use the freshly extracted project; do not copy only `MainActivity.kt`
into the older Phase 6E.1.2 folder.

## Focused release test

1. Open Android Code Studio → App Logs, clear it, and filter for `RecapFlowBlur`,
   `RecapFlowPreview`, `AndroidRuntime`, `FATAL EXCEPTION`, `Fatal signal`, and `GlException`.
2. Import the same source that reproduced the crash.
3. Enable Overlay and Source subtitle blur.
4. Drag the complete guide and release it at least 20 times while playing.
5. Repeat 20 times while paused.
6. Resize and release at least 20 times while playing and 20 times while paused.
7. Confirm the outline remains at the released location, then the blur pixels and slider
   values catch up without the Activity closing.

Expected log order for a changed rectangle:

```text
Blur gesture commit scheduled after touch dispatch: ...
Deferred blur gesture commit applied: ...
```

The Media3 update follows later under `RecapFlowPreview`. No filename, path, URI, raw
coordinate, `FATAL EXCEPTION`, or fatal signal should appear.

## Cancellation and regression

- Release and immediately begin another drag; only the newest valid release must commit.
- Release and immediately switch tabs/background the app; pending work must be discarded
  or cancelled without a stale transform or crash.
- Replace the source after release; the previous rectangle must not reach the new source.
- Sweep Horizontal, Vertical, Width, Height, strength, and time sliders.
- Rotate/recreate and confirm normalized geometry restoration.
- Render/play 720p, unlock/render/play 1080p, and compare geometry, timing, outside-region
  sharpness, duration, and A/V sync.

## If it still terminates

Copy one complete LogWire block before rebuilding:

- `FATAL EXCEPTION` through the final `Caused by`, or
- `Fatal signal` plus `Abort message`, or
- all `RecapFlowBlur`/`RecapFlowPreview` lines around the release.

Record whether `scheduled` appeared, whether `applied` appeared, and whether termination
happened before or after the blur pixels moved. Those three observations distinguish input,
deferred layout, and Media3/GPU failure.
