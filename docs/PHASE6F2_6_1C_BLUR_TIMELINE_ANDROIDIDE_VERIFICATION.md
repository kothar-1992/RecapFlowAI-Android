# Phase 6F.2.6.1C — Full-duration source blur timeline hotfix

## Owner-reported regression

A source-subtitle blur that appeared correctly in the first part of a long final video stopped being
applied in the later part. The reported validation sample was about five minutes long: an early
frame around 02:15 was blurred, while a later frame around 03:57 showed the original subtitle text.

This is a time-domain regression, not a Gaussian-blur strength/geometry failure.

## Root causes addressed

1. The default blur/logo time window was initialized from the current Trim only once. If Clips was
   later expanded, the old shorter overlay end time could remain silently cached, so an untouched
   "full current video" blur could stop halfway.
2. Final Media3 export can contain multiple clipped `EditedMediaItem`s. Media3 adds each preceding
   item's presentation duration before item `GlEffect`s execute. RecapFlow stored blur/logo windows
   in absolute source time but previously added only the source clip start inside the shader. Later
   sequence items could therefore double-count timeline progress and cross the overlay end early.
3. Encoded source video items incorrectly called `EditedMediaItem.Builder.setDurationUs()` with the
   clipped range duration. Media3 defines this value as the original pre-clipping source duration;
   Transformer can obtain intrinsic duration from encoded media, so the incorrect override is
   removed.

## Fix contract

- Untouched blur/logo time windows follow the current Trim whenever Clips changes.
- As soon as the user explicitly edits an overlay time slider, that range becomes manual and no
  longer follows Trim automatically.
- Old saved state created before this intent flag existed migrates to Trim-linked behavior.
- Final export projects absolute source overlay windows into every selected clipped range.
- Each item's overlay shader subtracts the Media3 composition offset before evaluating its local
  projected time window.
- Freeze keeps fixed-source-time semantics.
- Blur and logo remain live preview effects; no intermediate MP4 is rendered.
- Final export remains the single authoritative `EditPlan -> Media3 Composition -> Transformer`
  pass.

## Source preflight

```bash
bash scripts/verify_phase6f2_6_1c_source.sh
```

## AndroidIDE build

```bash
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --no-daemon --max-workers=2
```

## Device acceptance matrix

Use the same long portrait source that reproduced the failure.

1. Import the source and keep Trim at the full source range.
2. Enable Overlay -> Source subtitle blur without manually shortening the blur time slider.
3. Scrub the live source preview near 25%, 50%, 75%, and 95% of the source. Blur must remain active.
4. Change Clips Trim to roughly the first half, return to Overlay, then expand Clips back to full.
   The untouched blur time range must automatically expand back to the full Trim.
5. Export 1080p once. Inspect frames near 25%, 50%, 75%, and 95% of the final video. The same blur
   region must remain active wherever the selected source range contains subtitles.
6. Enable reviewed Adaptive Cuts with at least three separated source ranges spanning early, middle,
   and late source time. Export once. Blur must remain active in every selected range, including the
   final selected range.
7. Repeat with Intro Freeze enabled. The freeze frame uses the source frame's fixed blur state, and
   later clips must still remain blurred.
8. Repeat with 2x and 0.5x speed. Time-gated blur must not expire early because of sequence offsets.
9. Set a deliberate manual blur range (for example 01:00-02:00), then change Trim. The manual range
   must remain manual and only blur its intended source window.
10. Enable a logo with the default full-range time window and repeat the multi-range test. Logo time
    must follow the same corrected timeline semantics.
11. Confirm no render starts while editing Clips/Transform/Audio/Overlay. Only the final Export action
    may enter the render state or create an output MP4.

## PASS gate

Phase 6F.2.6.1C passes when the owner sample no longer loses blur in the latter half, untouched
full-range overlays track Trim changes, manual time windows remain intentional, and one-shot
720p/1080p export preserves blur/logo timing across single-range and multi-range compositions.
