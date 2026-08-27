# Phase 6F.2.6.2 — Full EditPlan Combination AndroidIDE Verification

## 1. Source gate

From the project root:

```bash
bash scripts/verify_phase6f2_6_2_source.sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --no-daemon --max-workers=2
```

Confirm:

- root project: `RecapFlowAI_Phase6F2_6_2`
- app version: `1.0-phase6f2.6.2`
- RecapFlow Native runtime loads
- FFmpeg 9.0.1 runtime loads

## 2. No-intermediate-render editor test

Import one source with visible motion, speech, source captions and enough duration to scrub at least
four separated points. Do not press Final Render yet.

1. Clips: set a Trim range; optionally review 3 Adaptive Cut ranges.
2. Transform: enable 9:16 or 16:9, Mirror, one visible Color adjustment, Zoom, 1.25x/1.5x Speed,
   optional Intro Freeze and Fade.
3. Audio: test Keep Original first; then Mix with one prepared external audio file.
4. Overlay: enable source Blur and one PNG/JPEG/WebP logo; move/resize the logo.
5. Return to Clips, Transform, Audio and Overlay and modify at least one control in each tab.

Expected before export:

- no intermediate MP4 is required,
- the imported source remains the editor source,
- live effects update when supported,
- a preview fallback does not delete the edit settings,
- logo movement does not require resizing the floating preview,
- aspect changes do not push the frame to one side or corrupt the source preview,
- blur remains visible in the first, middle and final quarters of its selected source range.

## 3. Single final export test

Choose **1080p** and render once.

Expected:

- only the final Export action starts `RenderUiState.Preparing/Rendering`,
- Logcat contains one `Starting 1080p H.264 export` line and one `composition=...` topology summary,
- output validates as display `1080 x 1920` for portrait or `1920 x 1080` for landscape,
- H.264 output and requested audio policy validate,
- requested 1080p bitrate is at least 30 Mbps under the current quality policy,
- UI shows the actual encoder average bitrate; record it for the baseline evidence,
- Blur and Logo remain present at the intended later timeline points,
- A/V remains synchronized after Speed/Adaptive Cuts/Freeze.

## 4. Preset isolation test

Without changing the edit settings:

- switch to 720p and confirm the edit/preview state remains intact,
- switch back to 1080p and confirm the edit remains identical,
- if device storage/thermal budget permits, run one 2K export as a separate quality test.

The export preset must affect final dimensions/quality only; it must not clear Clips, Transform,
Audio or Overlay settings.

## 5. Master-switch persistence test

For Transform, Audio and Overlay:

1. configure visible child settings,
2. turn the master switch Off,
3. verify the effect is omitted,
4. turn the master switch On again.

Expected: remembered controls return with the same values. Turning a tool Off must not destructively
reset another tool or require a render.

## 6. Failure/cancel safety

- Cancel one final render in progress: partial private output is removed and the source/edit plan
  remains usable.
- Try a missing/invalid replacement-audio asset: render fails before Transformer starts.
- Trigger the established preview fallback if reproducible: final export still uses the saved plan.

## Evidence to retain before GitHub baseline freeze

- AndroidIDE source verifier output
- unit-test output
- assembleDebug success output
- one screenshot of the combined live editor state
- one screenshot of validated 1080p result
- Logcat `composition=...` line
- actual average bitrate shown for 1080p
- output metadata (resolution, duration, video/audio codec)

Only after these checks pass should Phase 6F.2.6.2 be marked DONE and frozen as the first stable GitHub
baseline.
