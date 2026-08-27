# Task: Phase 6B.3 Horizontal Mirror

## Outcome

Let the user turn a horizontal Mirror on or off and see the exact result in the
floating Editor preview before spending time on a 720p or 1080p render.

## Scope

- Add a default-Off Mirror switch inside Review Editor → Transform.
- Retain the user's Mirror choice while the master Transform switch is off.
- Compile enabled Mirror to a horizontal axis flip.
- Build preview and export from the same Crop → Mirror → Presentation effect
  list so the result cannot drift between playback and the rendered MP4.
- Refresh the current paused frame without resetting the playback position.
- Persist Mirror state across Activity recreation.
- Lock Mirror during an active render and invalidate stale completed output
  after a later Mirror change.
- Add unit-test source for all Off/On compiler paths.

## Non-goals

- Color, Zoom, Speed, Freeze, or Transitions
- Keyframed or time-ranged Mirror
- Adaptive Edit, Gemini, Audio, or Overlay
- Changes to the verified trim, 720p playback-unlock, or 1080p sequence

## Acceptance

- Transform Off omits Mirror even if its remembered switch is checked.
- Mirror Off produces ordinary source orientation.
- Mirror On flips a recognizable left/right subject immediately while playing
  and while paused.
- Crop, Mirror, and aspect conversion work together in deterministic order.
- The 720p and 1080p outputs match the live preview orientation and framing.
- Opening a completed output does not apply Mirror a second time.
- Activity recreation restores the switch and its effect.
- A live-effects fallback keeps the selected render plan unchanged.

## Preflight

```bash
bash scripts/verify_phase6b3_source.sh
```
