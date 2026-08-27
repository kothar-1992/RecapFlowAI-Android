# Task: Phase 6C.2 Continuous Cut Preview and Per-Clip Transitions

Date: 2026-08-24

## Outcome

Let the user watch the complete reviewed cut sequence before Apply/render, while
keeping realtime preview and export on the same transform and transition settings.

## Included

- One `Preview full cut sequence` / `Stop sequence preview` action in Clips review.
- An on-device ExoPlayer playlist built from the reviewed clipping ranges in source order.
- Candidate selection that follows the currently playing playlist item.
- Crop, Mirror, Color, Zoom, Aspect, Speed, and Visual Fade on every preview item.
- Fade In, Fade Out, or In + Out applied independently to each rendered range.
- Per-range transition validation after Speed is accounted for.
- Normal continuous Trim preview/render while Apply remains Off.

## Defined behavior

- Sequence preview does not create a temporary render and does not use the network.
- Stopping, completing, failing, navigating away, or changing the edit restores the
  ordinary source preview.
- Fade In + Out creates a fade-through-black boundary between adjacent kept ranges.
- Media items remain sequential and do not overlap, so this is not a dissolve crossfade.
- Visual transitions do not alter planned duration and do not fade source audio.
- Intro Freeze keeps its existing separate preview and first-exported-segment behavior.

## Deferred

- Overlapping dissolve/crossfade and audio fade.
- Editable cut handles and waveform/timeline UI.
- Scene-aware/Gemini decisions, Audio controls, Overlay, and subtitles.

## Acceptance

- A reviewed sequence plays continuously in chronological order before Apply/render.
- The candidate indicator follows each boundary without leaving stale preview state.
- Preview and 720p/1080p export show matching transform/fade choices per range.
- A too-short reviewed range blocks the selected transition before rendering.
- Apply Off, cancellation, lifecycle, fallback, and the existing Phase 6C.1 export path remain safe.
