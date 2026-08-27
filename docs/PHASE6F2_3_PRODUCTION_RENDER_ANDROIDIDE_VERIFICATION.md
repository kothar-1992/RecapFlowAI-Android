# Phase 6F.2.3 AndroidIDE and Device Verification

## 1. Source identity and preflight

Open the `RecapFlowAI_Phase6F2_3` root, then run:

```bash
bash scripts/verify_phase6f2_3_source.sh
```

Confirm the final line reports Phase 6F.2.3 PASS.

## 2. Build and install

Use the already verified AndroidIDE/Android Code Studio environment and FFmpeg artifacts:

```bash
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true
```

Expected identity:

- project: `RecapFlowAI_Phase6F2_3`
- version: `1.0-phase6f2.3`
- Home/Settings toolbar: `Phase 6F.2.3`

Install and launch the debug APK. Confirm native bridge and FFmpeg version still load.

## 3. Export UX

1. Import an original source with the video gallery picker.
2. Open Editor → Export.
3. Confirm exactly three controls: 720p, 1080p, 2K.
4. Confirm there is no 4K choice.
5. Restart/recreate the Activity and verify the selected quality returns.
6. Save an editor preset, choose another quality, restore the preset, and verify quality restores.
7. Reset the Export section and confirm 1080p.

Pass if one `Render and save <quality>` action is used. Output preview must be labelled optional and
must not unlock another button or change the selected quality.

## 4. Exact-dimension matrix

Use an original source with at least 2560×1440 or 1440×2560 resolution first.

| Aspect | 720p | 1080p | 2K |
|---|---:|---:|---:|
| 9:16 | 720×1280 | 1080×1920 | 1440×2560 |
| 16:9 | 1280×720 | 1920×1080 | 2560×1440 |
| 1:1 | 720×720 | 1080×1080 | 1440×1440 |
| Original | exact 720 px short side | exact 1080 px short side | exact 1440 px short side |

For every output confirm:

- Completed UI reports the expected exact dimensions.
- video codec is H.264/AVC.
- audio is AAC when Keep/Replace/Mix is selected.
- no audio track exists when Mute is selected.
- duration stays inside the Phase 6F.2.5 calculated window (250 ms floor, 0.1% allowance,
  750 ms cap), with the actual delta recorded.
- public Gallery copy opens and has the same byte size as the private finalized output evidence.

## 5. Bitrate and source-quality matrix

Record UI target and actual average bitrate:

- 720p target band: 25–30 Mbps.
- 1080p target band: 30–45 Mbps.
- 2K target band: 45–60 Mbps.

Repeat with a source whose short side is below the selected target. Confirm the upscaling warning is
shown and exact requested dimensions still result. Visually note that no detail is invented.

Repeat once with a previous `RecapFlow_` output and confirm the generation-loss warning.

## 6. Playback and A/V review

For each quality, use the optional exported-video preview and inspect start, middle, and end:

- no black/green frames;
- correct orientation/aspect and no stretching;
- reviewed Trim/Adaptive order;
- Transform/Blur/Logo parity;
- audio present/muted/replaced/mixed as planned;
- lip-sync/audio-video drift within 100 ms by observation or measurement.

Then return without starting another render. Confirm playback did not alter quality or unlock a
second-stage test.

## 7. Failure and cancellation

- Cancel at approximately 10%, 50%, and 90%; incomplete private/public files must be removed and
  the source must remain.
- If the device cannot encode 1440×2560 H.264, confirm 2K ends in a clear failure. It must not show
  Completed for a 1080p fallback.
- Test low storage. The completed previous Gallery output and source must remain safe.
- Deny Android 9 legacy write permission. The validated private output must remain; retry can save.
- Switch quality after a completed/public output. The next render state should reset while the
  already finalized Gallery file remains.

## 8. Regression

Confirm import/probe, retained realtime preview, Trim, Adaptive sequence, Transform, Audio,
source-subtitle blur, static logo, editor preferences, Gallery, Open, Share, and Activity recreation
remain operational for 720p and 1080p as well as the new 2K path.
