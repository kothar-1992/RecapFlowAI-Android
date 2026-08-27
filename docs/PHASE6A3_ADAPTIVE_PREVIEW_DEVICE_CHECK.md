# Phase 6A.3 Adaptive Preview and Device Check

## Source identity

- Project: `RecapFlowAI_Phase6A3`
- Version: `1.0-phase6a3`
- Preflight: `bash scripts/verify_phase6a3_source.sh`

## What changed

- The sticky preview now preserves the rotation-aware source aspect ratio and
  sizes itself against the live Editor viewport.
- The scrolling sheet receives a progressive dim scrim under the preview. On
  Android 12+ a very small blur is added for Balanced/High devices; Light devices
  use the cheaper dim-only path.
- Settings shows a read-only local device profile: model/type, Android API,
  screen px/dp, CPU cores/ABI, available/total RAM, app-volume storage, network
  transport/status, and capability tier.

## Device verification

1. Clean-import this project and run the source preflight.
2. Confirm the Home subtitle ends in `Phase 6A.3`.
3. Import one portrait source and confirm the preview has no unused black half.
4. Import one landscape source and confirm the preview remains within the screen.
5. Swipe the Editor sheet upward and confirm text dims/softens behind the preview.
6. Open Settings and confirm every device-profile row is populated.
7. Rotate the device and repeat preview-fit and swipe-under checks.
8. Regression-test Trim → 720p → playback verification → 1080p.

The network row is not a speed test. It reports only Android's active transport
and validated-connectivity status; no telemetry is uploaded.
