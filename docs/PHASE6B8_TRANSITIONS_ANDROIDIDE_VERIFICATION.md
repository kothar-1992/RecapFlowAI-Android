# Phase 6B.8 Visual Fade — AndroidIDE and Device Verification

## 1. Source identity

Open the extracted project root and confirm:

- root project: `RecapFlowAI_Phase6B8`
- version: `1.0-phase6b8`

Run:

```bash
bash scripts/verify_phase6b8_source.sh
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true
```

## 2. UI and state

1. Import a video, open Editor → Transform, and enable master Transform.
2. Confirm Visual fade defaults Off.
3. Enable it and confirm Fade In, Fade Out, In + Out plus 0.5/1/1.5 sec.
4. Hide and show Transform controls; confirm the summary retains mode/duration.
5. Turn Fade Off/On and Transform Off/On; confirm the remembered choice returns.
6. Recreate/rotate the Activity; confirm enabled state, mode, duration, and collapse
   state are preserved.

## 3. Realtime preview

Use a clip longer than 6 seconds.

- Fade In: play from Trim start for each duration.
- Fade Out: seek near Trim end and play through the end.
- In + Out: verify both boundaries.
- While paused, change mode/duration and verify the frame redraws without rendering.
- Verify Fade Off restores the unchanged source preview.
- Repeat with Speed 0.5×, 1×, and 2×; measure the visible fade in wall time.
- If the device rejects live effects, confirm the explicit fallback message and that
  render settings are not lost.

## 4. Validation

- Select a 1-second clip and enable In + Out at 1.5 seconds.
- Confirm render is blocked with a short-clip message.
- Lengthen the Trim or reduce duration and confirm render becomes available.

## 5. Export matrix

Render and play both 720p and 1080p for:

| Case | Expected |
|---|---|
| Fade Off | No black fade |
| Fade In, 1 sec | Black to full image at Trim start |
| Fade Out, 1 sec | Full image to black at Trim end |
| In + Out, 0.5 sec | Both boundaries fade |
| In + Out, 1.5 sec + Speed 2× | Both visible fades remain about 1.5 sec |
| Fade + Crop/Color/Zoom | Combined visual graph matches preview |
| Fade + Intro Freeze | Freeze remains separate; moving clip begins its fade afterward |

Confirm output duration is unchanged by Fade, source audio is not faded, A/V stays
synchronized, and the original source remains untouched.

## 6. Regression and cancellation

- Cancel an active Fade render and confirm only the incomplete output is removed.
- Re-render after cancellation.
- Verify Home/Editor/Settings, preview overlay, Trim, 720p playback unlock, and 1080p
  test flow still work.

Record device model, Android/API, selected plan, output paths, duration, file size,
render elapsed time, playback result, and any visual mismatch.
