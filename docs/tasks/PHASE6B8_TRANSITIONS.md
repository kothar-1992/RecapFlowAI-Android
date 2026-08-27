# Task: Phase 6B.8 Single-clip Visual Fade

Date: 2026-08-24

## Outcome

Add a reversible, default-Off visual fade to the selected moving clip with
realtime preview and export parity.

## Included

- `Fade In`, `Fade Out`, and `In + Out` modes.
- `0.5 sec`, `1 sec`, and `1.5 sec` presets.
- A typed `TransitionSettings` model and deterministic compiler.
- A timestamp-specific Media3 RGB matrix shared by ExoPlayer preview and export.
- Speed compensation so the chosen wall/output fade duration stays constant.
- Short-clip and unsupported-duration validation.
- Remembered state when Transition or master Transform is Off.
- Activity recreation state and the existing collapsible Transform presentation.

## Defined behavior

- Fade changes video RGB gain toward black; it does not change timeline duration.
- Fade is visual only. Source audio is unchanged.
- Fade targets the selected moving clip after Trim.
- An enabled Intro Freeze is a separate still segment and is not faded.
- `Fade Out` can be inspected by seeking near the selected Trim end.

## Deferred

- Crossfade or any two-clip transition: Phase 6C multi-clip timeline.
- Audio fade controls.
- Adaptive Edit, Overlay, subtitles, Gemini, or any remote AI operation.

## Acceptance

- Off is a compiler no-op in preview and export.
- Preview changes immediately after mode/duration changes.
- 720p and 1080p output visually match preview at Trim boundaries.
- Speed presets preserve the selected visible fade duration.
- Existing Trim/Transform/Freeze/cancellation/playback unlock behavior remains intact.
