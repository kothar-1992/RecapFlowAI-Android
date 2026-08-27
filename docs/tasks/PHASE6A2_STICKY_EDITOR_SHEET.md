# Task: Phase 6A.2 Sticky Preview and Scrolling Editor Sheet

## User-visible outcome

The Editor no longer uses the tablet-only preview-left/details-right layout.
The video preview remains centered at the top while the user swipes a single
vertical tools sheet upward. Sheet content is drawn beneath the preview.

## Scope

- Fixed top preview overlay with responsive phone/tablet dimensions.
- Compact file summary by default.
- Expandable full Video details.
- Metadata, Trim, render progress, and output actions in one scroll sequence.
- Shared ViewBinding contract across compact and `sw600dp` parent layouts.
- Existing Trim/render/navigation behavior preserved.

## Non-goals

- No rendered image/video overlay operation.
- No Transform, Adaptive, Audio, subtitle, or Gemini controls.
- No Media3, FFmpeg, EditPlan, render-coordinator, or storage changes.
- No background render-job migration.

## Acceptance

Complete `docs/PHASE6A2_STICKY_EDITOR_ANDROIDIDE_VERIFICATION.md` on the target
device and regression-check the Phase 6A local-render path.
