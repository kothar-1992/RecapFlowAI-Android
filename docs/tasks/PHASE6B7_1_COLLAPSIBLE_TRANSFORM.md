# Task: Phase 6B.7.1 Collapsible Transform Controls

Date: 2026-08-22

## Goal

Reduce the tall Transform card to a compact summary whenever the user does not
need to change individual controls.

## In scope

- `Hide controls` and `Show controls` action below the Transform summary
- Collapsing Aspect, Crop, Mirror, Color, Zoom, Speed, Freeze, and explanatory text
- Keeping badge, master switch, summary, and collapse action visible
- Remembering collapse state across Activity recreation
- No change to live preview, enabled values, `EditPlan`, or render output

## Out of scope

- Rearranging features, user-defined feature order, or permanent feature removal
- Separate collapse state for every individual feature
- Transitions, Adaptive Edit, Audio controls, Overlay, or Gemini

## Acceptance

- Tapping Hide immediately removes the long details group.
- The summary still reports every enabled feature and its important preset.
- Tapping Show restores the same controls and configured values.
- Hiding during preview or render does not reset, cancel, or alter media work.
- Rotation/recreation restores the last Show/Hide choice.

## Source preflight

```bash
bash scripts/verify_phase6b7_1_source.sh
```
