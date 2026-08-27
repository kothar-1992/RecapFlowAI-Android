# Phase 6E.1.2 — Touch Gesture Isolation

## Observed device evidence

The owner can change Horizontal, Vertical, Width, Height, and strength with the
Material sliders without a crash. The app terminates only when the blur guide or its
resize handle is moved directly over the PlayerView. This isolates the regression to
the touch dispatch path rather than the typed blur model, shader, or ordinary slider
update path.

## Outcome

Dragging and resizing keep the marked guide responsive without changing layout or the
Media3 graph while that view owns an active pointer stream. The final rectangle is
committed once after release.

## Implementation contract

- `ACTION_DOWN` snapshots the last committed normalized rectangle.
- `ACTION_MOVE` calculates a pending rectangle and uses compositor-only translation or
  top-left scale for the guide outline.
- `ACTION_MOVE` must not update `layoutParams`, geometry sliders, `OverlaySettings`, or
  call `setVideoEffects`.
- `ACTION_UP` and `ACTION_CANCEL` restore identity transforms and atomically commit the
  latest valid pending rectangle.
- The release commit updates labels, sliders, guide layout, stale-output state, and one
  immediate live-preview graph.
- A caught runtime failure restores the guide and interception state and logs under
  `RecapFlowBlur` without logging media paths or user content.
- Slider edits keep the existing coalesced live-preview behavior.

## Acceptance

1. Perform 20 guide drags and 20 bottom-corner resizes while playing, then repeat paused.
2. The outline follows the pointer continuously and stays within preview bounds.
3. During the gesture, the existing blur pixels may remain at the last committed region;
   they move to the released rectangle immediately after release.
4. Horizontal, Vertical, Width, Height, time, and strength sliders remain realtime and stable.
5. LogWire shows release commits or a complete caught error; no `FATAL EXCEPTION` or fatal
   native signal occurs.
6. Rotate/recreate, switch tabs, replace source, render/cancel, and return to editing without
   stale translations or scales.
7. The released rectangle matches 720p and 1080p output.

Stop after this hotfix. Image/video assets, multiple blur regions, tracking, Gemini,
Export/Download, Telegram, and background jobs are separate gates.
