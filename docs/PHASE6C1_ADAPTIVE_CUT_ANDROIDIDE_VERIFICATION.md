# Phase 6C.1 Adaptive Cut Draft — AndroidIDE and Device Verification

## 1. Source identity and build

Confirm root project `RecapFlowAI_Phase6C1_1` and version `1.0-phase6c1.1`, then run:

```bash
bash scripts/verify_phase6c1_source.sh
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true
```

## 2. Review workflow

1. Import a video longer than 30 seconds and set a non-full Trim.
2. Generate Gentle, Balanced, and Compact drafts; confirm candidate count/keep/remove
   summaries change and all ranges stay inside Trim.
3. Use Previous, Preview clip, and Next. Each preview must start/end at the shown range.
4. Leave Apply Off, render 720p, and confirm the normal continuous Trim is exported.
5. Turn Apply On, render 720p, and confirm only reviewed ranges appear in order.
6. Clear the draft and confirm normal Trim behavior returns.

## 3. State and conflict checks

- Apply cuts, change Trim, and confirm the stale draft is cleared.
- Import another source and confirm the old draft is cleared.
- Recreate/rotate after generating a valid draft; confirm preset, ranges, candidate,
  and Apply state restore.
- Apply cuts and confirm Visual Fade is disabled with an explanatory Phase 6C.2 note.
- Confirm master Transform and all other per-feature Off states still omit their effects.

## 4. Export matrix

Render/play 720p and 1080p for: Apply Off; each preset Apply On; Speed 0.5×/2×;
Mirror/Color/Zoom; Intro Freeze; nonzero Trim start; and a source with HEVC input.
Verify output duration against the applied kept duration after Speed plus Freeze.

Check chronological order, no missing first/last candidate, source audio continuity,
A/V sync at every cut, H.264/AAC playback, and unchanged source media.

## 5. Reliability

- Cancel an active multi-range render; only the incomplete output may be removed.
- Render again after cancellation.
- Background/restore during review and after a completed render.
- Verify Home/Editor/Settings, overlay preview, Trim, 720p playback unlock, and 1080p flow.

Record device/API, source codec/audio, preset, ranges, expected/actual duration,
output path/size, elapsed time, playback result, and any A/V discontinuity.
