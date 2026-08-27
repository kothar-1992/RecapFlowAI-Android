# Task: Phase 6A.1 Home / Editor / Settings Navigation

## Goal

Reduce the crowded single-screen workspace by introducing three top-level
destinations while preserving the working Phase 6A Trim and local-render path.

## Implemented scope

- `Home`: starts video import, displays the active local project, and continues
  to the Editor.
- `Editor`: owns preview, metadata, Clips/Trim, render progress, and output
  actions.
- `Settings`: reports current on-device processing and FFmpeg engine status.
- A single-Activity Material bottom navigation shell is used for phones and the
  current tablet layout.
- The selected destination is saved across Activity recreation.
- Back from Editor or Settings returns to Home before app exit.
- Import always opens Editor so copy/probe progress remains visible.
- Leaving Editor pauses video preview but does not cancel an active render.

## Explicit non-goals

- No Gemini API integration.
- No Transform, Adaptive, Audio, Overlay, subtitle, or AI controls.
- No Fragment migration or persistent multi-project library.
- No tablet Navigation Rail until the three-destination behavior is verified.

## Acceptance gate

Run `docs/PHASE6A1_NAVIGATION_ANDROIDIDE_VERIFICATION.md` on the target Android
device and regression-check the working Trim → 720p → playback unlock → 1080p
sequence before beginning Phase 6B.
