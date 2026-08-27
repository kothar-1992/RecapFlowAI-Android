# Phase 6E.2 — Static Image / Logo Overlay

## Goal

Add one reversible local still-image overlay that is visible before render and compiles to
the same 720p/1080p result. The operation must remain safe on the owner's Android device:
geometry is controlled only by sliders and presets in this gate.

## Functional scope

- Add a default-Off `Image / logo overlay` operation below the Overlay master switch.
- Pick one PNG, JPEG, or WebP through Android `ACTION_OPEN_DOCUMENT`.
- Copy and validate the asset in `cacheDir/image_overlays`; enforce 20 MB and 8192-pixel
  source limits and decode at no more than 2048 pixels per side for the GPU texture.
- Preserve PNG alpha; use the first static frame for supported WebP input.
- Expose Replace, Remove, Reset, Top-left, Top-right, Center, Bottom-left, and Bottom-right.
- Expose Horizontal/Vertical center, 8–80% frame-width size, 10–100% opacity, and absolute
  source-time range controls.
- Preserve image aspect ratio against the post-Transform frame and clamp the complete image
  inside the frame.
- Apply Transform → source subtitle blur → image/logo for both preview and export.
- Respect adaptive clipped ranges and the fixed intro-freeze source timestamp.
- Restore the validated private asset/settings after Activity recreation and delete it when
  the user removes it or replaces the source project.

## Required disabled behavior

- Overlay master Off omits both source blur and image operations.
- Image item Off remembers its settings but omits the image operation.
- Image item On without a valid asset blocks render with a clear validation message.

## Deliberate non-goals

- No direct preview drag/resize listener (`Phase 6E.2.2` deferred).
- No animation loop (`Phase 6E.2.3` remains a later gate after static parity).
- No video overlay, image rotation, keyframes, multiple image layers, Gemini, Export tab,
  Telegram Bot API, VPS processing, or project database.

## Acceptance

1. A transparent PNG appears at the chosen position without a black rectangle.
2. Playing and paused preview changes match both 720p and 1080p output.
3. Original, 9:16, 16:9, 1:1, Fit, and Fill preserve the logo's proportions.
4. Time range, adaptive ranges, and intro freeze use absolute source time consistently.
5. Off/Remove/source replacement omit the texture and do not leave a stale private file.
6. Repeated slider changes, rotation, render cancellation, and source replacement do not crash.

The owner-device stale preset/slider preview issue discovered after initial import is tracked and
fixed in `PHASE6E2_1_REALTIME_LOGO_CONTROLS.md`.
