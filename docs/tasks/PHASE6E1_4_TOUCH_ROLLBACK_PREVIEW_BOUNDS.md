# Phase 6E.1.4 — Touch Rollback + Preview Bounds Recovery

## Device evidence

The source-blur rectangle can be moved during a direct gesture, but releasing the finger
still terminates the Activity. The same rectangle remains stable when edited through the
Horizontal, Vertical, Width, and Height sliders. Separately, changing a portrait source to
9:16 or 16:9 can leave the PlayerView controller inside the preview while the video surface
is mostly black or offset beyond the card.

## Outcome

Ship the stable slider workflow and remove the unsafe touch entry point from the UI. Keep
the touch implementation dormant behind a default-false source flag for later diagnosis.
Embed the playback pixels in the movable/resizable view hierarchy and refresh their bounds
whenever output-aspect geometry changes.

## Contract

- `SOURCE_BLUR_DIRECT_TOUCH_ENABLED` remains `false` for this release.
- No touch listener is installed on the blur guide or its resize handle.
- The guide is non-clickable/non-focusable, remains visible, and follows slider values.
- The resize handle is hidden; Reset and all blur sliders remain available.
- Preview and 720p/1080p export continue to compile the same typed blur rectangle.
- PlayerView uses `texture_view` because the preview is moved, resized, clipped, and placed
  above scrolling content.
- Preview card, content frame, and PlayerView clip video pixels to the card.
- Aspect/crop layout changes clamp card bounds and request PlayerView/TextureView layout on
  the next animation frame.
- No source media, URI, filename, coordinate, API key, or token is added to logs.

## Re-enable gate

Direct blur touch may return only in a separate task after a complete `FATAL EXCEPTION`,
native fatal-signal, or reproducible framework/Media3 trace identifies the release crash and
automated/device regression checks cover repeated releases while playing and paused.

## Non-goals

This patch does not change shader pixels, typed export settings, preview-card move/resize,
Transform semantics, multiple blur regions, tracking, image/video overlays, Gemini,
Export/Download, Telegram, or background rendering.
