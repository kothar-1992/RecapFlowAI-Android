# Phase 6E.1 — Manual Source Subtitle Blur

GitHub task: [`ZeusOwner/recapflow-ai#21`](https://github.com/ZeusOwner/recapflow-ai/issues/21)

## Outcome

The Review Editor exposes an Overlay tab where the user can manually cover existing
source-video captions with one localized blur region and verify it before rendering.

## Contract

- Overlay and Source subtitle blur are independently default-Off and remembered.
- Either Off state is a true compiler no-op; the render graph contains no blur effect.
- The blur rectangle uses normalized final-preview coordinates from the top-left.
- The user can drag/resize the marked preview region or use precise position/size sliders.
- Strength is limited to 4–32 pixels at a 720-pixel short-side reference and scales
  proportionally at other resolutions.
- The active time range is stored against the absolute source timeline and must remain
  inside the source for at least 250 ms.
- Realtime ExoPlayer preview and Media3 export use the same typed compiler and GPU shader.
- Media3 1.8 loads the two GLSL programs from packaged `assets/shaders/` string paths.
- Only the selected rectangle is blurred; a feathered edge blends into unchanged pixels.
- Clipped Adaptive preview/export items map local timestamps back to the source timeline.
- Intro Freeze uses the selected source frame's fixed time to decide whether blur applies.
- No source media or edit setting is uploaded.

## Acceptance

1. Overlay Off and Source blur Off both preserve the existing preview/export pixels.
2. Drag, resize, precise sliders, Reset, strength, and time range update paused and playing
   source preview before render.
3. Text inside the rectangle is obscured while nearby pixels outside it remain sharp.
4. 720p and playback-unlocked 1080p outputs match the selected preview geometry/time.
5. Trim, Adaptive candidates/full sequence, Transform, Freeze, and Audio retain behavior.
6. Rotation/recreation restores tab, switches, rectangle, strength, time, and collapse state.
7. Changing any blur value invalidates stale completed output and controls lock during render.

Stop after this gate. Multiple regions, keyframes/tracking, image/video assets, text rendering,
automatic subtitle detection, Gemini, Export/Download delivery, and Telegram are separate gates.
