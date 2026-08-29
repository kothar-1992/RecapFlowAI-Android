# Phase 6F.2.8.1 Owner Test Checklist

Use the same or similarly detailed source material for before/after comparison.

## 1080p / 30fps

- [ ] UI reports Phase 6F.2.8.1 and CBR, not stale Phase 6F.2.6/VBR copy.
- [ ] requested bitrate is 10.00 Mbps.
- [ ] reported average bitrate is >= 8.00 Mbps when telemetry is available.
- [ ] output is 1080 x 1920 for the portrait test source.
- [ ] output frame rate remains approximately 30fps.
- [ ] H.264 video and expected AAC/audio policy pass.
- [ ] duration drift remains inside bounded validation tolerance.
- [ ] visual detail is materially sharper than the prior 2.78 Mbps VBR output.
- [ ] Gallery copy opens and plays correctly.

## 720p / 30fps

- [ ] requested bitrate is 7.50 Mbps.
- [ ] reported average bitrate is >= 6.00 Mbps when telemetry is available.
- [ ] exact 720p short-side geometry passes.
- [ ] output is not visibly soft/blurry compared with the prior regression.
- [ ] FPS, A/V sync, duration and Gallery export remain sane.

## Architecture regression

- [ ] Clips, Transform, Audio and Overlay can still be edited before export without intermediate MP4 renders.
- [ ] CompositionPlayer/ExoPlayer preview fallback behavior remains usable.
- [ ] final export still starts Transformer exactly once.

Do not merge the hotfix solely from source/build success. Both owner-device visual-quality checks are required.
