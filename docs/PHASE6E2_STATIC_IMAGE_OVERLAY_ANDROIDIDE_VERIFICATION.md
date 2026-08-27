# Phase 6E.2 AndroidIDE Verification

## Clean import

Import the extracted folder whose root project name is `RecapFlowAI_Phase6E2_1`. Confirm
`app/build.gradle.kts` shows `versionName = "1.0-phase6e2.1"` before building.

## Source preflight

From the project root:

```bash
bash scripts/verify_phase6e2_1_source.sh
```

Expected: resource/ViewBinding, Kotlin delimiter, typed compiler, shader, test, disabled-touch,
and project identity checks all report `PASS`.

## Build

Use the already verified AndroidIDE environment and FFmpeg archives:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true
```

If AndroidIDE needs its local AAPT2 override, retain the previously verified
`android.aapt2FromMavenOverride` configuration. Do not change NDK 24 or CMake 3.18.1.

## Device matrix

1. Import portrait H.264 and HEVC sources; repeat with one landscape source.
2. Overlay master On → Image/logo On. Confirm render is blocked until an image is selected.
3. Pick transparent PNG, JPEG, and WebP separately. Verify name, dimensions, and size summary.
4. Test five presets, X/Y sliders, size min/default/max, opacity 10/50/100, and Reset.
5. Pause on a frame and adjust every slider; confirm the same-frame preview refreshes.
6. Test time range before/inside/after the active interval.
7. Test Original, 9:16, 16:9, 1:1 with Fit and Fill. The image must never stretch or leave
   the output frame.
8. Combine with Source subtitle blur. The logo remains sharp even if it overlaps the blur area.
9. Combine with Adaptive Cuts and intro Freeze; verify absolute source-time behavior.
10. Rotate/recreate the Activity; confirm asset, switch, X/Y, size, opacity, and time restore.
11. Render/play 720p, unlock/render/play 1080p, and compare position/size/alpha/time.
12. Turn item Off and master Off; confirm both outputs omit the image. Re-enable and confirm
    remembered settings return.
13. Remove image and choose another source; verify no stale image appears.
14. Cancel at early/middle/late progress; incomplete output is removed and source/image remain.

## Touch safety check

There must be no image guide or image touch listener on the floating PlayerView. Image geometry
is changed only with the stable sliders and presets in Phase 6E.2. Source-blur direct touch also
remains disabled by `SOURCE_BLUR_DIRECT_TOUCH_ENABLED = false`.

## Failure evidence

Capture AndroidIDE Build output and LogWire/App Logs filtered by:

```text
RecapFlowPreview
RecapFlowBlur
AndroidRuntime
FATAL EXCEPTION
VideoFrameProcessingException
```

Record source codec/orientation, image type/dimensions, selected aspect/Fit/Fill, whether the
preview was playing, and the exact control changed. Never include private media or credentials.
