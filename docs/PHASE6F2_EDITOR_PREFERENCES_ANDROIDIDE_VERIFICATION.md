# Phase 6F.2 AndroidIDE and device verification

## Source preflight

From the extracted project root run:

```bash
bash scripts/verify_phase6f2_source.sh
```

## Build

1. Open the project in AndroidIDE.
2. In the patched delivery, confirm project `RecapFlowAI_Phase6F2_1`, version
   `1.0-phase6f2.1`, minSdk 28, Media3 1.10.0.
3. Run `:app:assembleDebug` and install the APK.

## Preference matrix

1. Import an original source and set non-default Transform, Audio gains, Blur geometry,
   logo geometry, Adaptive preset, editor tab, collapsed panels, and preview-card layout.
2. Settings → Save current preset.
3. Change each value, then Restore saved preset. Confirm sliders/toggles and realtime preview
   update without rendering.
4. Turn Auto-restore On, change values, leave the Activity/app, relaunch, and confirm last-session
   choices restore.
5. Turn Auto-restore Off, relaunch, and confirm launch defaults remain in effect; manually use
   Restore last session to recover the saved values.
6. Remove the imported logo/replacement audio, relaunch/restore, and confirm their operations
   remain Off while geometry/gain choices are retained.
7. In each Clips/Transform/Audio/Overlay tab, use Reset current editor section and confirm only
   that section resets. Export reset must not delete Gallery/private outputs.
8. Use Reset all, confirm the dialog, then confirm safe defaults and no named preset.

## Regression matrix

- Switch 9:16, 16:9, 1:1 and Original while paused and playing; no stuck loading or stretched
  preview.
- Blur remains realtime and slider-controlled; direct blur screen touch remains disabled.
- Render original-source 720p and 1080p, verify requested/actual bitrate, dimensions, A/V sync,
  logo/blur parity, Gallery publication, Open, and Share.
- Start a render and confirm Save/Restore/Reset controls are disabled until completion/cancel.
- Rotate during editing and confirm Bundle state wins over persistent startup preferences.
