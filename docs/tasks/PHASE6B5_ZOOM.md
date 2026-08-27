# Task: Phase 6B.5 Zoom Modes

## Outcome

Let the user turn Zoom off or choose Zoom In, Zoom Out, or a smooth Alternate
cycle while watching the floating source preview, then export the same framing
at 720p or 1080p.

## Scope

- Add a default-Off Zoom switch inside Review Editor → Transform.
- Keep the selected mode remembered while Zoom or master Transform is off.
- Add Zoom In (`1.15×`), Zoom Out (`0.90×`), and Alternate (`0.90×`–`1.10×`).
- Make Alternate deterministic and repeat every four seconds.
- Keep the frame canvas unchanged; Zoom changes content scale only.
- Use one shared Crop → Mirror → Color → Zoom → Presentation effect builder.
- Normalize live Alternate timing against the selected Trim start.
- Preserve playback position and redraw a paused frame after every change.
- Persist Zoom state across Activity recreation.
- Lock controls during rendering and invalidate completed output after changes.
- Add unit-test source for Off, static scale, and Alternate-cycle semantics.

## Non-goals

- Pinch-to-zoom, pan position, keyframes, or subject tracking
- Speed, Freeze, or Transitions
- Adaptive Edit, Gemini, Audio, or Overlay
- Changes to the verified 720p playback-unlock and 1080p sequence

## Acceptance

- Transform Off omits remembered Zoom settings.
- Zoom Off leaves preview and export unchanged.
- Zoom In crops outer edges without changing the output canvas.
- Zoom Out reveals background around the full frame without resizing output.
- Alternate moves smoothly and repeats every four seconds.
- Every mode updates playing and paused previews before render.
- Crop, Mirror, Color, Zoom, and aspect conversion compose in documented order.
- 720p/1080p output framing matches the live preview.
- Rendered-output playback does not apply Zoom a second time.
- Activity recreation restores the switch and selected mode.

## Preflight

```bash
bash scripts/verify_phase6b5_source.sh
```
