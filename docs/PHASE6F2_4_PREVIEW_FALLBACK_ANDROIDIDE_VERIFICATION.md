# Phase 6F.2.4 AndroidIDE/device verification

## Build identity

1. Open `RecapFlowAI_Phase6F2_4` as a fresh AndroidIDE project.
2. Confirm the app version is `1.0-phase6f2.4`.
3. Run `bash scripts/verify_phase6f2_4_source.sh`.
4. Run `./gradlew :app:testDebugUnitTest --stacktrace`.
5. Build with the verified FFmpeg gate:
   `./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace`.
6. Confirm the APK contains ARM64 `libflowai.so` and the app reports the expected native/FFmpeg
   version after launch.

## Required samples

Record duration, displayed/raw dimensions, rotation, frame rate, video/audio codecs, and bitrate:

| Sample | Required characteristic |
|---|---|
| A | Reported 540×960 portrait H.264/AAC source |
| B | 1080p portrait source |
| C | 1080p landscape source |
| D | Source known to reproduce the Media3 effect failure, if different from A |

## Workflow matrix

For each applicable sample:

1. Import from the gallery and play/seek the untouched source.
2. Set a non-full Trim, enable 9:16 Fit/Fill, Mirror, Color, Zoom, Speed, source blur, and logo in
   small combinations before testing the full combination.
3. If live effects work, confirm one frame reflects each change and no fallback message appears.
4. If live effects fail, confirm exactly one fallback notice appears and original-source playback
   resumes with correct position/play intent.
5. While source-only is active, change at least ten Transform/Overlay values. Confirm no repeated
   automatic retry, no repeated snackbar, no 00:00 stuck surface, and no crash.
6. Confirm controls retain the selected settings and the retry action stays visible.
7. Tap `Retry live effects` once. Confirm the log shows one new generation. Record whether the
   graph succeeds or returns once to source-only playback.
8. Rotate/recreate the Activity, leave/return to Editor, preview an adaptive candidate/sequence,
   and play a completed output. Confirm source-only state does not corrupt those workflows.

## Final render regression

Render one supported sample at 720p, 1080p, and 2K using the same selected effects. For each output
record actual dimensions, codec, audio presence/policy, duration, bitrate, private/public byte
counts, Gallery visibility, Open/Share, and visible effect parity. A preview fallback is not a
reason to skip or silently downgrade final validation.

## Log capture

Filter LogWire/logcat to `RecapFlowPreview`, `RecapFlowBlur`, and the app process. Capture the first
Media3 exception plus cause, error name/code, graph summary, source dimensions/codec, recovery
reason, retry generation, and any terminal fallback line. Do not report only the snackbar text.

## Pass criteria

- No automatic retry loop after source-only fallback.
- Source-only playback is stable, or terminal unavailability is explicit without a stale-frame
  claim.
- Explicit retry is bounded and observable.
- 540×960 preview is not upscaled merely for interaction.
- Final 720p/1080p/2K output passes the existing exact-quality validator.
- No crash, ANR, partial output leak, source deletion, or progressive memory growth in 20 repeated
  edit/fallback/retry cycles.
