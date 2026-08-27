# Task: Phase 6C.1 Reviewed Adaptive Cut Draft

Date: 2026-08-24

## Outcome

Add transparent pacing suggestions that the user reviews and explicitly applies
before multiple source ranges are concatenated into one local export.

## Included

- Typed Adaptive Cut preset and reviewed-range state in `EditPlan`.
- Gentle, Balanced, and Compact deterministic pacing presets.
- Ordered, non-overlapping keep ranges constrained to the current Trim.
- Previous, candidate Preview, Next, Apply, and Clear controls.
- Normal Trim render while the draft remains unapplied.
- Media3 sequential concatenation after Apply.
- Planned-duration calculation across ranges, Speed, and Intro Freeze.
- Draft invalidation after Trim/source changes and Activity state restoration.

## Defined behavior

- Draft generation is local rule-based pacing, not AI or scene understanding.
- Preview plays only the selected candidate and stops at its end.
- Apply Off retains review state but omits Adaptive Cuts from render.
- Apply On concatenates reviewed ranges in original chronological order.
- Visual Fade cannot be enabled with applied cuts in this gate.

## Deferred

- Full continuous composition preview and editable range handles.
- Cross-clip fade/crossfade transitions (Phase 6C.2).
- Gemini, Audio controls, Overlay, subtitles, and remote processing.

## Acceptance

- Draft creation never changes export until Apply is On.
- Every candidate can be inspected before render.
- Applied 720p/1080p exports retain ordered content and original audio sync.
- Off states and invalid ranges are compiler no-ops or validation failures.
- Existing Trim, Transform, Freeze, cancellation, and playback unlock behavior remains intact.
