# Phase 6E.1.1 — Blur Drag/Resize Preview Stability

## Outcome

The user can move and resize the manual source-subtitle blur guide without terminating
the Activity. The guide stays visually responsive, while Media3 receives a bounded number
of immutable live-effect updates and the final released position is committed immediately.

## Contract

- Direct guide motion updates only geometry labels, geometry sliders, and guide layout.
- Geometry, strength, and time-range changes share a 140 ms coalescing gate.
- A pending drag or resize is flushed on `ACTION_UP` and `ACTION_CANCEL`.
- Master switches and Reset still apply immediately.
- The latest typed `OverlaySettings` remains the render source of truth.
- A preview failure activates the existing source-only fallback; it does not disable export.
- Queued callbacks cannot outlive Activity/player teardown or a source replacement.
- Logs use `RecapFlowPreview` and `RecapFlowBlur`; no media path or user content is logged.
- LogWire remains an Android Code Studio development receiver and is not bundled into the
  RecapFlow release dependency graph.

## Acceptance

1. Drag the guide continuously for at least 20 seconds while playing and paused; no crash.
2. Resize from minimum to maximum repeatedly; guide remains inside the preview.
3. The guide follows the finger continuously and the rendered blur catches up within the
   bounded update interval; releasing commits the exact final rectangle.
4. Sweep Horizontal, Vertical, Width, Height, Strength, and both time-range thumbs; no crash.
5. App Logs show bounded `RecapFlowPreview` application records instead of one update for
   every raw touch event.
6. Any Player error includes its code and throwable under `RecapFlowPreview`, then restores
   source-only preview without losing the planned export settings.
7. Rotate/background/foreground, replace the source, cancel render, and close the Activity;
   no stale callback accesses a destroyed binding/player.
8. Phase 6E.1 localized pixels, time mapping, 720p/1080p export, and Off-state behavior do
   not change.

Stop after this stability gate. Multiple regions, tracking, image/video overlay assets,
Gemini, Export/Download, Telegram, and release crash telemetry remain separate gates.
