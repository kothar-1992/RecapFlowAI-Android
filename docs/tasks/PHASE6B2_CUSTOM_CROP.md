# Task: Phase 6B.2 Custom Crop Rectangle

## Outcome

Allow the Android user to enable a deterministic source-frame crop inside the
existing Transform tab, adjust each edge, disable the crop without losing the
remembered rectangle, and render locally at 720p or 1080p.

## Scope

- Add `CropSettings(enabled, rectangle)` to the typed `TransformSettings`.
- Use normalized top-left coordinates for `left`, `top`, `right`, and `bottom`.
- Add a Crop On/Off switch and four 0–40% edge sliders.
- Collapse edge sliders while Crop or Transform is off.
- Convert the rectangle to Media3 `Crop(left, right, bottom, top)` NDC values.
- Apply Crop before Presentation and frame-rate effects.
- Restore settings across Activity recreation.
- Keep controls disabled during an active render.
- Reset stale completed output after any crop change.

## Non-goals

- Draggable handles over the preview
- Animated crop/keyframes
- Zoom, Mirror, Color, Freeze, Speed, or transitions
- Adaptive Edit, Audio, Overlay, or Gemini
- VPS or cloud processing

## Acceptance

- Transform Off omits both aspect and Crop effects.
- Crop Off omits Crop but preserves the four remembered edges.
- Crop On with asymmetric edges visibly removes the selected source area.
- No stretching is introduced by the crop.
- Original, 9:16, 16:9, and 1:1 remain compatible with Fit/Fill.
- Trim, 720p playback unlock, and 1080p render continue to work.
- Source media is never modified.

## Preflight

```bash
bash scripts/verify_phase6b2_source.sh
```
