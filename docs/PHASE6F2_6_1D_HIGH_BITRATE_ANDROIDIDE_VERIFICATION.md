# Phase 6F.2.6.1D AndroidIDE verification

## Goal
Verify that final export asks the device H.264 encoder for the new high-quality CBR bands without changing the one-final-render editor workflow.

## Source preflight
```sh
bash scripts/verify_phase6f2_6_1d_source.sh
```

## Device matrix
1. Use an original source whose short side is at least the selected preset when judging native detail.
2. Render 720p and confirm the UI requests at least 25 Mbps.
3. Render 1080p and confirm the UI requests at least 30 Mbps.
4. Render 2K and confirm the UI requests at least 45 Mbps.
5. Record the actual average bitrate reported by `ExportResult`, encoder name, output size, elapsed time and device temperature.
6. If actual bitrate is below 80% of the request, confirm an encoder quality note is shown. Below 50% must be labelled a severe quality shortfall.
7. Confirm exact display geometry: 720p short side 720, 1080p short side 1080, 2K short side 1440.
8. Confirm no intermediate render occurs while editing Clips, Transform, Audio or Overlay. Only Export starts Transformer.

## Visual quality checks
- Compare the exported file against the original at 100% zoom, not only inside the movable preview.
- Test a detailed/high-motion scene and a face/dialogue scene.
- Do not treat an upscale as native detail: a 540p/720p source exported to 1080p has 1080p dimensions but cannot recreate missing source pixels.
- Upload one 1080p sample to the target social platform and compare the platform-processed stream after processing completes. Social platforms re-encode uploaded media, so the local master should be clean before upload.

## Pass criteria
- High-quality CBR request is visible and actual bitrate is materially higher than the previous low-bitrate export on the same source/device.
- No resolution, duration, audio, blur/logo, aspect, Gallery, or preview regression.
