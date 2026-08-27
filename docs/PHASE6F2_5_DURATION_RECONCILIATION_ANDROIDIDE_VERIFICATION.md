# Phase 6F.2.5 AndroidIDE/device verification

## Build

1. Open `RecapFlowAI_Phase6F2_5` as a fresh AndroidIDE project.
2. Confirm version `1.0-phase6f2.5`, JDK 17, NDK `24.0.8215888`, CMake `3.18.1`, and ARM64.
3. Run `bash scripts/verify_phase6f2_5_source.sh`.
4. Run `./gradlew :app:testDebugUnitTest --stacktrace`.
5. Run `./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace`.
6. Install and launch the APK; confirm JNI and FFmpeg `9.0.1` diagnostics.

## Duration advisor

1. Import the owner sample and reproduce a reviewed plan near `04:53.154`.
2. Open Export and confirm it shows reviewed `04:53.154`, closest `04:53`, `−154 ms`, and
   the calculated drift allowance.
3. Tap `Update clips to 04:53`; confirm only Trim end changes when Adaptive Cuts are off.
4. Apply Adaptive Cuts, create a sub-second remainder, tap Update, and confirm only the final
   reviewed range changes while earlier ranges and story order remain identical.
5. Repeat with 0.5×, 0.75×, 1.25×, 1.5×, 2× and Intro Freeze; the resulting compiled duration
   must land exactly on the displayed whole second.
6. Confirm an unsafe final-range adjustment hides the action and asks for manual Trim/clip review.

## Render validation

For 720p, 1080p, and 2K, record planned/output duration, delta, allowed delta, dimensions, video
codec, audio codec/presence, actual bitrate, file size, and encoder.

- Reproduce 293430 ms output against 293154 ms plan: it must complete with a 276 ms warning.
- A synthetic/unit result 295 ms away from the same plan must fail because the allowance is
  294 ms.
- No output may pass with the wrong short side, odd dimensions, non-H.264 video, or wrong
  AAC/mute policy.
- Completed private output must publish byte-for-byte to `Movies/RecapFlowAI`.
- Playback inspection must confirm A/V sync; equal stream durations alone are insufficient.
- Cancel at early/middle/late progress; partial output is removed and the source survives.

## Regression

Verify Photo Picker import, source-only preview fallback and explicit retry, Trim, Transform,
Audio, Overlay, optional rendered playback, Gallery Open/Share, Activity recreation, and source
replacement. No VPS request is permitted.

