# Phase 6E.2.1 — Realtime Logo Controls Hotfix

## Owner report

PNG logo import and composition work, and the position/size labels change. The preview logo itself
remains at the original top-right/default size when presets or sliders are adjusted.

## Root cause

The custom Media3 GL program captured one immutable `CompiledImageOverlay` when it was created.
On the target runtime that program can remain active while a replacement effect list is installed,
so the editor state changes but the retained shader continues using its initial geometry.

## Implementation

- Add `RealtimeImageOverlayState`, a thread-safe preview-only snapshot bridge.
- Update it before the throttled preview graph commit on every Overlay change.
- Read the matching snapshot inside `StaticImageOverlayShaderProgram.drawFrame()` and recompute
  aspect-preserving bounds for the current frame.
- Hide an old shader when the item/master is Off, removed, or replaced with a different asset.
- Pass the same bridge to normal, Trim, adaptive-candidate, and adaptive-sequence previews.
- Keep render/export immutable; `TransformVideoEffects.forRender()` does not receive the bridge.
- Keep direct screen-touch logo drag/resize disabled.

## Acceptance criteria

- TL, TR, Center, BL, and BR move the logo immediately.
- Horizontal/Vertical, Logo width, Opacity, and active-time bars affect playing preview frames and
  paused frames after the existing same-position redraw.
- Logo aspect ratio and composed-frame clamping remain correct.
- Off/Remove/replacement never leaves the prior logo visible.
- Adaptive preview uses the same reviewed logo state.
- 720p and 1080p exports match the reviewed static settings.
- No screen-touch listener is installed for the logo.

## Deferred

Direct screen-touch drag/resize and animation loops remain separate gates. Gemini, Telegram, and an
Export destination are also outside this hotfix.
