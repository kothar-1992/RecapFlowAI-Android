# Task: Phase 6C.2.1 Movable and Resizable Preview Overlay

Date: 2026-08-25

## Outcome

Let the user place and size the Editor's floating video preview without changing
the render plan or blocking normal PlayerView playback controls.

## Included

- A dedicated Move handle for one-finger dragging.
- A bottom-right resize handle that preserves video/output aspect ratio.
- Resize range from 55% of the adaptive default up to the largest fully visible
  card that fits the Editor viewport.
- Screen-edge clamping after every move, resize, and orientation/layout change.
- Reset to the device-adaptive one-third default at centered-top position.
- Normalized position/scale restoration through Activity state.
- Exact synchronization of the localized underlay mask with the preview rectangle.

## Defined behavior

- Gestures operate only through the visible Move/Resize handles, leaving the
  PlayerView controller, seek bar, and playback gestures available.
- Resizing changes only preview presentation; it never changes export dimensions.
- Moving the preview does not reflow the scrolling editor sheet.
- The default spacer remains based on the one-third adaptive size; enlarging or
  moving down intentionally allows the sheet to pass underneath the floating card.
- Reset does not change playback position, edit settings, or render state.

## Deferred

- Rendered image/video Overlay items and keyframes.
- Pinch-anywhere gestures, snapping presets, opacity, and persistent project preferences.
- Audio controls, Gemini, and overlapping dissolve crossfade.

## Acceptance

- The card remains fully reachable inside compact and tablet Editor viewports.
- Move, resize, reset, rotation, and recreation preserve a usable preview.
- Only content geometrically under the preview receives the existing dim mask.
- Player controls, continuous Adaptive preview, realtime transforms, and render flow regressions pass.
