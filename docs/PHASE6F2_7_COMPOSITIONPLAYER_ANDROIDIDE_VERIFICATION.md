# Phase 6F.2.7 — AndroidIDE / Device Verification

## 1. Source preflight

From the project root:

```bash
bash scripts/verify_phase6f2_7_source.sh
```

Expected final line:

```text
PASS: RecapFlowAI Phase 6F.2.7 CompositionPlayer preview source contract is valid.
```

## 2. Unit tests

```bash
./gradlew :app:testDebugUnitTest --no-daemon --max-workers=2
```

The new `CompositionPreviewTimelinePolicyTest` and `RealtimePreviewSessionTest` must pass together
with the existing regression suite.

## 3. CompositionPlayer-enabled build

```bash
./gradlew :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true \
  -Precapflow.composition.preview.enabled=true \
  --no-daemon \
  --max-workers=2
```

Install the generated debug APK. Confirm the installed version is `1.0-phase6f2.7`.

## 4. No-render live-edit matrix

Import the same long portrait source used for the recent blur/aspect tests. Do not press Final Render
while running this matrix.

1. Clips: change Trim, restore full Trim, then apply reviewed Adaptive Cuts spanning early/mid/late source time.
2. Transform: test 9:16 -> 16:9 -> 9:16, FIT/FILL, Mirror, Color, Zoom and Speed at 2x then 0.5x.
3. Audio: Keep -> Mute -> Replace -> Mix. Confirm Replace/Mix does not produce doubled audio.
4. Overlay: enable source subtitle Blur and Logo. Scrub approximately 25%, 50%, 75% and 95% of the selected output; both must stay correct where their time windows apply.
5. Return between Clips/Transform/Audio/Overlay repeatedly. The imported source must remain valid and no render state should start.
6. While paused, change blur rectangle/strength and logo position/size/opacity. The visible frame should update after the debounced Composition rebuild without creating an MP4.
7. During playback and after edits, verify playhead does not jump to the wrong source segment when Adaptive Cuts or Speed are active.

## 5. Explicit ExoPlayer capability paths

### Intro Freeze

Enable Intro Freeze and preview it. This phase intentionally switches normal preview to the existing
ExoPlayer simulation. Disable Freeze again; normal preview may return to CompositionPlayer. The
EditPlan must remain intact.

### Feature flag OFF

Build once with:

```bash
./gradlew :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true \
  -Precapflow.composition.preview.enabled=false \
  --no-daemon \
  --max-workers=2
```

Repeat a short Transform + Blur + Logo test. ExoPlayer live preview must still work and final export
must remain available.

## 6. Fallback evidence

If CompositionPlayer fails naturally on the device, capture Logcat lines containing
`CompositionPlayer fallback to ExoPlayer`. Confirm preview continues with Exo live effects and the
EditPlan controls retain their values. If no failure reproduces, record that instead; do not invent
failure evidence.

## 7. One final render only

After all live-edit checks, return to the enabled build and perform exactly one 1080p final export.
Verify:

- one final render session only; no intermediate render while editing,
- exact `1080x1920` for portrait or `1920x1080` for landscape output geometry,
- H.264 and expected AAC/mute policy,
- requested 1080p bitrate budget remains at least 30 Mbps and output diagnostics report actual bitrate,
- Blur/Logo timing remains correct through the late section,
- Adaptive Cut order and Speed duration are correct,
- A/V sync is acceptable,
- final duration passes the existing tolerance policy,
- Gallery/public export still works.

## 8. Result reporting

Report either `Phase 6F.2.7 PASS` or paste the first compile/runtime error plus the related Logcat
section. PR #10 must remain draft/unmerged until this matrix passes.
