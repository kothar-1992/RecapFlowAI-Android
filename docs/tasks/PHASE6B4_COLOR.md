# Task: Phase 6B.4 Color Adjustments

## Outcome

Let the user adjust Brightness, Contrast, Saturation, and Warm/Cool temperature
while watching the floating source preview, then export the same appearance at
720p or 1080p.

## Scope

- Add a default-Off Color switch inside Review Editor → Transform.
- Collapse the four controls while Color or the master Transform is off.
- Add safe stepped ranges and a one-tap neutral Reset.
- Keep the user's Color choice and values remembered while disabled.
- Compile only valid, enabled, non-neutral Color adjustments.
- Use one shared preview/export effect builder with deterministic order.
- Preserve playback position and redraw a paused frame after every adjustment.
- Persist Color state across Activity recreation.
- Lock controls during rendering and invalidate completed output after changes.
- Add unit-test source for Off, neutral, mapping, and validation semantics.

## Non-goals

- LUT import, curves, auto-enhance, HDR grading, or keyframed color
- Zoom, Speed, Freeze, or Transitions
- Adaptive Edit, Gemini, Audio, or Overlay
- Changes to the verified 720p playback-unlock and 1080p sequence

## Acceptance

- Transform Off omits remembered Color settings.
- Color Off leaves preview and export unchanged.
- Color On with all values zero is a neutral no-op.
- Each slider updates a playing and paused preview without resetting time.
- Reset returns the preview to neutral immediately.
- Crop, Mirror, Color, and aspect conversion compose in documented order.
- 720p/1080p output color and framing match the live preview.
- Rendered-output playback does not apply Color a second time.
- Activity recreation restores the switch and all four values.

## Preflight

```bash
bash scripts/verify_phase6b4_source.sh
```
