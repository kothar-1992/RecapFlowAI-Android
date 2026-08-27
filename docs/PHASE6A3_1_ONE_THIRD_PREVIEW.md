# Phase 6A.3.1 One-third Preview

## Source identity

- Project: `RecapFlowAI_Phase6A3_1`
- Version: `1.0-phase6a3.1`
- Preflight: `bash scripts/verify_phase6a3_1_source.sh`

## Corrections

- Preview height is capped at one third of the live Editor viewport.
- The source aspect ratio and rotation remain preserved.
- The Editor sheet no longer receives whole-view blur or reduced alpha.
- The progressive dim mask has the same size, center position, and top margin as
  the preview, so content outside that rectangle stays visually unchanged.
- Device diagnostics and all media/render behavior are unchanged.

## Device checks

1. Import a portrait video and confirm the preview uses no more than one third
   of the Editor height.
2. Swipe upward and confirm text outside the preview rectangle remains sharp and
   at normal brightness.
3. Import a landscape video and confirm it stays inside the horizontal margins.
4. Rotate the device and repeat the two layout checks.
5. Confirm Settings still reports model, screen, CPU, RAM, storage, network, and
   capability profile.
6. Regression-test Trim → 720p → playback verification → 1080p.
