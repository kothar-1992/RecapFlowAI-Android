# Phase 6B.1 Transform — AndroidIDE Verification

## Source identity

- Project: `RecapFlowAI_Phase6B1`
- Version: `1.0-phase6b1`
- Preflight: `bash scripts/verify_phase6b1_source.sh`

## Scope

- Transform master switch: Off by default.
- Review Editor tabs: Clips and Transform (one panel visible at a time).
- Aspect: Original, 9:16, 16:9, 1:1.
- Scale: Fit (whole frame) or Fill (center crop).
- 720p targets: 720×1280, 1280×720, or 720×720.
- 1080p targets: 1080×1920, 1920×1080, or 1080×1080.
- Source preview remains unchanged; completed-output playback uses output aspect.

## Device verification

1. Clean-import the project and run the preflight script.
2. With Transform Off, render the known Trim plan and confirm behavior matches
   Phase 6A.3.1.
3. Open the Transform tab, turn Transform On, choose 9:16 + Fit, render 720p,
   play it, then render 1080p.
4. Repeat 9:16 + Fill and confirm frame edges crop without stretching.
5. Smoke-test 16:9 and 1:1 at 720p.
6. Turn Transform Off and confirm the last aspect/Fit-Fill selections remain
   visible but disabled; render must return to source aspect.
7. Rotate/recreate the Activity and confirm Transform settings restore.
8. Confirm Home/Editor/Settings, Trim, cancel, playback unlock, and source
   preservation still work.

Fit can add black bars. Fill can crop top/bottom or left/right. This gate does
not include custom crop, zoom, mirror, color, speed, audio, overlays, or Gemini.
