# Phase 6F.2.3 — Production Render Quality

## Goal

Let the user choose one final output quality in Review Editor → Export and produce exactly that
resolution without the inherited 720p → playback → 1080p test sequence.

## Product contract

| Choice | Exact short side | Standard portrait | Standard landscape | H.264 target band |
|---|---:|---:|---:|---:|
| 720p | 720 px | 720×1280 | 1280×720 | 25–30 Mbps |
| 1080p | 1080 px | 1080×1920 | 1920×1080 | 30–45 Mbps |
| 2K | 1440 px | 1440×2560 | 2560×1440 | 45–60 Mbps |

`2K` means QHD/1440p in this app. 4K is deliberately absent. Square output is 720×720,
1080×1080, or 1440×1440. Original aspect keeps the source/crop aspect while enforcing the exact
selected short side.

The selected resolution is exact. A source below the target is upscaled because the user asked
for that output size, but the UI warns that scaling cannot recreate missing source detail. The
bitrate is a hardware-encoder request, not a promise that every device will hit the exact average;
RecapFlowAI reports the actual bitrate.

## State and persistence

- `RenderPreset` contains only `HD_720P`, `FULL_HD_1080P`, and `QHD_2K`.
- The default is `FULL_HD_1080P`.
- `EditorPreferencesSnapshot.renderPreset` persists last-session and saved-preset selection.
- Preference schema 2 writes `export.preset`; schema 1 remains readable and migrates to 1080p.
- Activity recreation also preserves the selected enum independently.
- Changing quality invalidates the private completed state for the next render but never deletes an
  already finalized public Gallery file.

## Render flow

1. The user reviews edits and opens Export.
2. The user selects 720p, 1080p, or 2K.
3. `startNextRender()` takes exactly `selectedRenderPreset` and creates one immutable `EditPlan`.
4. Media3 Presentation enforces the selected short side or exact aspect-preset dimensions.
5. Transformer requests H.264 plus AAC when the plan contains audio.
6. `RenderedOutputInspector` reads the finalized local MP4 tracks off the UI thread.
7. `RenderedOutputValidationPolicy` verifies short side, even dimensions, H.264, AAC/mute policy,
   and duration using the bounded Phase 6F.2.5 policy (250 ms floor, 0.1% long-file allowance,
   750 ms cap).
8. Only a valid file enters `RenderUiState.Completed` and automatic public export.
9. Optional output preview uses ExoPlayer but does not change render state or unlock a quality.

If an encoder silently emits the wrong resolution, codec, or audio policy, the private mismatched
file is removed and the UI shows a typed failure. It is not published or labelled as the selected
quality.

## Scope exclusions

- 4K/UHD
- H.265/HEVC final encoding
- AI upscaling, sharpening, or denoising
- software x264 fallback
- background render job/service
- proxy render generation
- bitrate-mode or frame-rate controls

## Source verification

Run:

```bash
bash scripts/verify_phase6f2_3_source.sh
```

The gate chains every retained Phase 6E.3B–6F.2.2 source preflight, parses all Android XML, checks
the three-only quality model and persistence, rejects the old playback unlock, and proves the
single selected-quality start path and post-render validator markers.
