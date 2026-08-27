# RecapFlowAI Phase 6C.2.1 Implementation Status

Date: 2026-08-25

Patch identity: `RecapFlowAI_Phase6C2_1` / `1.0-phase6c2.1`.

## Implemented in source

- Dedicated Move and aspect-preserving resize handles above PlayerView.
- Dynamic resize from 55% of the adaptive default to the viewport maximum.
- Full-card screen-boundary clamping for drag, resize, rotation, and layout changes.
- Reset to one-third adaptive size and centered-top position.
- Activity-state restoration using normalized center coordinates and scale.
- Preview underlay mask synchronization with the exact moved/resized rectangle.
- Separation from `EditPlan`: preview placement never changes rendered output.

## Verification boundary

Static checks can run in this delivery workspace. Gradle compilation, installation,
gesture behavior, PlayerView layering, and device rendering are not claimed unless
those checks complete. Follow
`PHASE6C2_1_MOVABLE_PREVIEW_ANDROIDIDE_VERIFICATION.md` on the target device.

## Not included

Rendered Overlay items, opacity/keyframes, Audio controls, Gemini, and crossfade are
unchanged and remain outside this UI-only gate.
