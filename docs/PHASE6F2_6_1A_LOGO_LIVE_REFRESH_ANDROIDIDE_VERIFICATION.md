# Phase 6F.2.6.1A — Logo Live Refresh AndroidIDE Verification

This follow-up hotfix keeps the Phase 6F.2.6.1 project/version identity. It fixes a preview-only
invalidation bug: while the video is paused, image/logo position state could update internally but
the already-presented ExoPlayer frame stayed unchanged until the floating preview surface was
resized or playback produced another frame.

## Source preflight

```bash
bash scripts/verify_phase6f2_6_1a_source.sh
```

The verifier must report PASS. The normal Phase 6F.2.6.1 verifier is also executed to prove that
exact 720p/1080p final-render validation and the non-destructive one-final-render workflow remain
intact.

## Device acceptance test

1. Import a video and open **Editor → Overlay**.
2. Upload a PNG/JPEG/WebP logo and enable **Image / logo overlay**.
3. Pause the video on a visible frame.
4. Drag **Horizontal center** continuously. The logo must visibly move during editing without
   resizing or moving the floating preview window and without pressing Play.
5. Drag **Vertical center**. The same immediate paused-frame refresh is required.
6. Change **Logo width** and **Opacity**. The same retained preview must update without a render.
7. Tap TL/TR/Center/BL/BR presets. The logo must jump to the selected location immediately.
8. Start playback and repeat X/Y changes. Natural video frames must show the new logo state without
   decoder rebuild or final render.
9. Stop touching the controls. The preview playhead may pulse by about two source frames internally,
   then must settle back to the original timestamp. It must not accumulate timeline drift.
10. Scrub the timeline intentionally while a refresh is pending. The settle callback must not snap
    the user back to the old position.
11. Export once at 1080p. The final logo position/size/opacity must match the live preview.

## Expected architecture

```text
Image controls
    -> immutable OverlaySettings/EditPlan update
    -> RealtimeImageOverlayState update
    -> retained StaticImageOverlayEffect shader
    -> paused-frame refresh pulse only when playback is paused

Final Export
    -> shared Media3 Composition
    -> Transformer once
```

The refresh pulse is not a render, does not generate an intermediate MP4, and does not rebuild the
Media3 effect graph. It exists only to make the retained shader execute `drawFrame()` while the
source preview is paused.
