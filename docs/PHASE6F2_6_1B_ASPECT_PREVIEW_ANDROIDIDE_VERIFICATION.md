# Phase 6F.2.6.1B — Aspect-ratio Live Preview AndroidIDE Verification

## Regression fixed

On an imported 540x960 HEVC portrait source, selecting 9:16 or switching to 16:9 could leave the decoded image clipped to the right side of the movable preview card. A subsequent Presentation update could then fall through preview recovery and show **Video preview could not be restored**.

The failure was a preview-surface sequencing issue, not source-media corruption. The Presentation graph could be rebound while the movable `PlayerView`/`TextureView` still owned the previous card bounds. Class-only effect signatures also treated 9:16 Presentation and 16:9 Presentation as the same topology even though their output geometry differed.

## Fix contract

- Aspect preset changes (Original / 9:16 / 16:9 / 1:1) are geometry-critical preview changes.
- FIT/FILL changes while Presentation is active are geometry-critical preview changes.
- The preview card is resized first.
- Two UI animation frames are allowed for the `TextureView` to consume the new bounds.
- Only then is the source preview decoder/effect graph rebuilt at the same playhead position.
- Color/logo/blur parameter-only changes still stay on the retained player and remain realtime.
- No intermediate MP4 is created and no export/render state is entered.

## AndroidIDE build

```bash
bash scripts/verify_phase6f2_6_1b_source.sh
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --no-daemon --max-workers=2
```

## Device matrix

Use the same 540x960 HEVC file that reproduced the bug.

1. Import source; confirm normal source preview fills the card correctly.
2. Enable Transform and Mirror.
3. Select **9:16 + Fit**.
   - Card remains 9:16.
   - Video is centered and fills the expected presentation frame; it must not collapse to a strip on the right.
   - No preview-unavailable message.
4. Switch **9:16 -> 16:9** while paused.
   - Card becomes 16:9 before the new Presentation graph is shown.
   - FIT preserves the whole portrait source with expected empty canvas/letterbox space.
   - No stale strip from the previous TextureView bounds.
5. Switch **16:9 Fit -> Fill**.
   - Preview changes to centered crop/fill without source loss or player failure.
6. Switch **16:9 -> 1:1 -> 9:16 -> Original** repeatedly, both paused and playing.
   - Each geometry remains centered and stable.
   - Controls remain usable.
7. Enable logo and move/resize it after each aspect switch.
   - Logo updates remain live.
8. Enable blur and verify the blur region remains usable.
9. Final 1080p export once.
   - Export must use the currently selected aspect/fit mode.
   - No intermediate render should have occurred during the switches.

## PASS gate

Phase 6F.2.6.1B passes when repeated aspect/FIT/FILL changes no longer offset the decoded video, never show `Video preview could not be restored`, and the final one-shot export matches the selected geometry.
