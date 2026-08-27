# Phase 6F.1.1 — H.264 render-quality hotfix

## User-visible outcome

720p and 1080p exports no longer depend on an opaque device-selected H.264 bitrate. RecapFlowAI
requests a quality floor for each preset, reports the encoder's actual average bitrate after
completion, and warns when source resolution or repeated encoding limits recoverable detail.

## Implementation scope

- Request at least 25 Mbps for 720p and 30 Mbps for 1080p H.264 output.
- Preserve a higher probed source bitrate within conservative 12/45 Mbps preset caps.
- Keep Media3's supported encoder-settings fallback enabled through `setEnableFallback(true)` so
  an incompatible bitrate request can recover. Do not call `setEnableFormatFallback`, which is not
  available on the resolved Media3 `1.10.0` builder used by AndroidIDE.
- Capture `ExportResult.averageVideoBitrate` and show requested versus actual bitrate.
- Warn when the selected preset upscales the source short side.
- Warn when the input filename identifies an earlier RecapFlow export, because another generation
  of H.264 encoding can lose detail.
- Preserve the reviewed EditPlan, effects, audio, cancellation, private-first render, and
  byte-for-byte MediaStore publication behavior.

## Non-goals

- No AI upscaling, sharpening, denoising, HDR conversion, H.265 output, or software encoder.
- No promise that a 576-pixel source gains genuine 720p/1080p detail.
- No change to Gallery/Share copy bytes and no background render service.

## Definition of done

- Source preflight passes.
- Unit tests prove the 25/30 Mbps floors, preset caps, upscale detection, and repeated-export warning.
- AndroidIDE builds the compile-patched `1.0-phase6f1.1.1`.
- A real 720p and 1080p render reports an actual video bitrate near the requested target or exposes
  the device fallback explicitly.
- Output dimensions/codecs, A/V sync, cancellation, effects, Gallery/Open/Share, and source
  preservation regressions pass.
