# Phase 6F.2.6.1 — Final Render Geometry + Non-Destructive Live Preview Hotfix

## Owner-reported failures

1. A portrait 1080p render was rejected with `Expected exact 1080x1920, but received 1920x1080`.
2. Enabling an edit operation could make the preview unavailable, which made the imported source look broken and made the editor feel as if each tab needed its own render.

## Root cause 1 — coded geometry was mistaken for display geometry

Android/MP4 can store portrait video as coded `1920x1080` plus a 90/270 degree track rotation. The old inspector read `KEY_WIDTH`/`KEY_HEIGHT` only, while validation compared those coded values directly with portrait display targets. The valid output was therefore deleted as a false mismatch.

The fix reads `MediaFormat.KEY_ROTATION`, normalizes it, derives display width/height, and validates user-selected 720p/1080p/2K against display geometry. Coded dimensions remain authoritative for the H.264 even-dimension check.

## Root cause 2 — retained live GPU graph could reject topology changes

The editor already compiles all enabled settings into one immutable `EditPlan`, and final export already starts Transformer once from one Media3 `Composition`. Control changes do not render media. The failure was in preview: adding/removing an Effect on a retained ExoPlayer graph can be rejected by a device GPU/decoder stack. After recovery, the UI could fall back to source-only or unavailable preview and make the workflow look destructive.

The hotfix keeps parameter-only effect updates on the retained player. When the list of Effect classes changes, it recreates only ExoPlayer and installs the complete current effect graph before `prepare()`, restoring position, play state, audio preview and current settings. If a retained update is synchronously rejected, the same clean graph rebuild is attempted before source-only fallback.

No output video is produced by this preview rebuild.

## Final workflow contract

```text
Import/probe source
    -> user edits Clips
    -> user edits Transform
    -> user edits Audio
    -> user edits Overlay
    -> user chooses Export quality
    -> currentEditPlan(preset)
    -> Media3CompositionPlanCompiler
    -> Media3CompositionCompiler
    -> Transformer.start(composition, output)
    -> inspect coded geometry + rotation
    -> validate display geometry / codec / audio / duration
    -> publish one final MP4
```

The user may revisit any editor tab before final export. A completed render becomes stale if settings are changed afterward, but that does not mean a render is required between tabs; it only means the next final export must represent the newly edited plan.

## Files changed

- `MainActivity.kt`
- `RenderedOutputInspector.kt`
- `RenderedOutputValidation.kt`
- `LocalRenderCoordinator.kt`
- `RenderedOutputValidationPolicyTest.kt`
- `PLAN.md`
- Phase verification docs/script
