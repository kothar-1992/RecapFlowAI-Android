# Task: Phase 6B.7 Intro Freeze

Date: 2026-08-22

## Goal

Let the user add a short, reversible still-frame introduction at the selected
Trim start, judge its timing before render, and export it locally.

## In scope

- Default-Off Intro Freeze switch under Review Editor → Transform
- Remembered `1 sec`, `2 sec`, and `3 sec` presets
- A preview action that pauses on the selected Trim-start frame, then plays
- Planned output duration equal to speed-adjusted source duration plus freeze
- Off-main-thread source-frame extraction to a bounded temporary JPEG
- Sequential Media3 image + clipped-video composition
- Silent audio during the freeze followed by the original synchronized audio
- Existing visual transforms on the freeze frame; Alternate Zoom holds its
  neutral cycle origin and Speed applies only to source content
- Cleanup on success, failure, cancellation, and coordinator shutdown
- Recreation state, render locking, validation, and compiler tests

## Out of scope

- Freeze at an arbitrary timeline point or multiple freeze segments
- User-selected freeze frame independent of Trim start
- Transitions, Adaptive Edit, Audio controls, Overlay, or Gemini

## Acceptance

- Off adds no image item and does not alter planned/output duration.
- `Preview intro freeze` visibly holds the Trim-start frame for 1/2/3 seconds
  before source playback begins.
- Export begins with the same frame, contains silence during the intro, and
  begins original audio when moving source content starts.
- Expected duration is `(selected duration / speed) + freeze duration`, within
  the device tolerance in the verification guide.
- Cancellation removes incomplete output and temporary frame assets while the
  imported source remains playable.

## Source preflight

```bash
bash scripts/verify_phase6b7_source.sh
```

Device steps are in
[`../PHASE6B7_FREEZE_ANDROIDIDE_VERIFICATION.md`](../PHASE6B7_FREEZE_ANDROIDIDE_VERIFICATION.md).
