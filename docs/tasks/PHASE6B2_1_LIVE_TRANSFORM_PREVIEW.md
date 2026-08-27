# Task: Phase 6B.2.1 Live Transform Preview

## Outcome

Show the enabled Transform result in the floating Editor preview before export,
so the user can adjust aspect, Fit/Fill, and custom crop without repeatedly
rendering trial outputs.

## Scope

- Replace the legacy `VideoView` preview with Media3 `PlayerView` + `ExoPlayer`.
- Apply Crop and Presentation effects to source playback with
  `ExoPlayer.setVideoEffects`.
- Share one effect builder with the Transformer export path so effect order and
  control interpretation cannot drift.
- Update the preview card aspect immediately when the transform changes.
- Preserve the current playback position and playing/paused intent.
- Redraw the current frame after a control changes while playback is paused.
- Keep rendered-output playback free of a second live Transform pass.
- If a device cannot run live effects, restore the ordinary source preview,
  explain the fallback, and retain the user's export settings.

## Non-goals

- Mirror, Color, Zoom, Speed, Freeze, or Transitions
- Draggable crop handles or keyframed animation
- Adaptive Edit, Gemini, Audio, or Overlay
- Changes to the verified 720p/1080p export gate

## Acceptance

- Transform Off displays ordinary source playback.
- Original + Custom crop visibly updates all four source edges before render.
- 9:16, 16:9, and 1:1 update preview framing immediately.
- Fit shows the whole source and Fill crops the center without stretching.
- A paused preview redraws after a control adjustment.
- Play/pause position is not reset by normal live adjustments.
- Opening a rendered output does not apply the live source effects again.
- Export uses Crop → Presentation ordering from the same builder as preview.
- A preview-effect failure does not alter the EditPlan or block export.

## Preflight

```bash
bash scripts/verify_phase6b2_1_source.sh
```
