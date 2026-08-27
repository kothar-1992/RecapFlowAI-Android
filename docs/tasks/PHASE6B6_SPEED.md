# Task: Phase 6B.6 Speed

Date: 2026-08-22

## Goal

Give the user an explicit, reversible constant-speed control that can be judged
in realtime before render and exported with synchronized audio/video timing.

## In scope

- Default-Off Speed switch under Review Editor → Transform
- Remembered `0.5×`, `0.75×`, `1×`, `1.25×`, `1.5×`, and `2×` presets
- Immediate source-preview playback-speed updates while playing or paused
- `1×` and every disabled state as a true compiler no-op
- One shared constant Media3 `SpeedProvider` for export audio and video effects
- A direct video-only speed effect for sources without an audio track
- Planned/estimated output duration derived from the selected clip and speed
- Activity recreation, render locking, and stale-output invalidation
- Compiler, validator, resource, and source-marker checks

## Out of scope

- Variable speed curves, ramps, beat synchronization, or per-segment speed
- Freeze frames or transitions
- Adaptive Edit, Audio controls, Overlay, or Gemini

## Acceptance

- Off preserves original preview/export timing.
- Each non-neutral preset changes preview playback before render.
- Rendered audio and video use the same constant timing provider.
- Expected duration is `selected source duration / speed`, within the device
  tolerance specified in the verification guide.
- Rendered-output playback always runs at `1×` because speed is already baked in.
- Cancel leaves the source intact and removes incomplete output.

## Source preflight

```bash
bash scripts/verify_phase6b6_source.sh
```

Device steps are in
[`../PHASE6B6_SPEED_ANDROIDIDE_VERIFICATION.md`](../PHASE6B6_SPEED_ANDROIDIDE_VERIFICATION.md).
