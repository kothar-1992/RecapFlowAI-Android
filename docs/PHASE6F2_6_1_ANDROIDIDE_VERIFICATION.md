# Phase 6F.2.6.1 AndroidIDE verification

Run source preflight first:

```bash
bash scripts/verify_phase6f2_6_1_source.sh
```

Confirm `rootProject.name = "RecapFlowAI_Phase6F2_6_1"` and `versionName = "1.0-phase6f2.6.1"`, then build with the same FFmpeg-enabled AndroidIDE command used by the verified baseline.

## A. Rotation-aware quality regression

Use the owner sample that probes as coded `1920x1080` but displays portrait.

1. Choose 1080p.
2. Render once.
3. Expected: no false `1080x1920 vs 1920x1080` failure when the output track reports rotation 90/270.
4. Completed diagnostics should report display geometry `1080x1920`; logs may also report coded `1920x1080` and rotation.
5. Repeat 720p and expect display short side exactly 720.

A true landscape `1920x1080 rotation=0` must still fail an expected portrait `1080x1920` exact-aspect request.

## B. One-plan / one-final-render workflow

Do not render while editing.

1. Import once.
2. Configure Clips/Trim or reviewed Adaptive Cuts.
3. Enable Transform and adjust at least Mirror or Color plus an aspect mode.
4. Move to Audio; choose Keep/Mute or a prepared Replace/Mix asset.
5. Move to Overlay; enable blur and/or image overlay.
6. Revisit Clips and Transform and change values again.
7. Confirm source remains available and controls remain editable.
8. Confirm topology changes may briefly rebuild preview but do not create an output file or enter RenderUiState.Rendering.
9. Open Export, choose one quality, and render exactly once.
10. Confirm final output contains the combined reviewed plan.

## C. Preview failure boundary

If a device rejects a retained effect update, verify the log contains `rebuilding preview graph` and the preview is recreated at the prior position. Source-only fallback should occur only if the clean rebuild is also rejected. Saved settings must remain intact in either case.
