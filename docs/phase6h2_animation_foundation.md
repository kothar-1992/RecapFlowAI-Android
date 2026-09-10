# Phase 6H.2 Animation Foundation

Issue #21 starts from the accepted Target-duration Clips timeline.

This first slice intentionally adds no user-facing animation controls yet. It establishes the semantic and timing contract that later UI and OpenGL rendering must consume:

- `ImageOverlayAnimationPreset`: `NONE`, `FADE`, `FADE_SCALE`, `POP`, `SLIDE`, `PULSE`, `FLOAT`, `ROTATE`, `BOUNCE`.
- reviewed source-time settings: loop enabled, animation duration, loop period.
- compiler-only phase offset after source-range projection so animation does not restart at every reviewed clip boundary.
- deterministic 0..1 phase resolution with a settled interval between loop cycles.
- CompositionPlayer Speed projection scales overlay window, animation duration, loop period, and phase offset together.
- canonical validator rejects invalid source-time duration/period combinations.
- `NONE` remains the default and preserves the existing static-logo contract.

The next slice must consume this policy in the shared OpenGL image-overlay effect used by preview/export. It must not introduce temporary logo-video rendering or a second timeline authority.
