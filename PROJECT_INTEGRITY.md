# RecapFlowAI Phase 6F.2.8.1 — Project Integrity

Date: 2026-08-29

This hotfix continues from the owner-confirmed Phase 6F.2.6.2 stable baseline merged at
`7411b54ba922c49a28fde4ea7e0250b50d019900`, the Phase 6F.2.7 CompositionPlayer merge, and the
Phase 6F.2.8 social-export merge. It changes final-export quality enforcement only; the EditPlan,
preview topology, and one-final-render architecture remain intact.

- Branch: `hotfix/phase-6f2.8.1-render-quality`
- Application ID: `com.recapflow.ai`
- Version: `1.0-phase6f2.8.1`
- AndroidX Media3: `1.10.0`
- FFmpeg baseline: `9.0.1` ARM64 static integration
- Preview flag: `recapflow.composition.preview.enabled` (default `true`)
- Preferred preview: shared EditPlan -> Media3 Composition -> CompositionPlayer
- First fallback: ExoPlayer live effects
- Second fallback: ExoPlayer source-only preview
- Final export: one Media3 `Composition` -> one `Transformer.start(...)`
- Current gate: source implemented; owner-device CBR quality verification pending before merge

## Required invariants

1. Clips, Transform, Audio and Overlay remain editable without intermediate MP4 renders.
2. CompositionPlayer failure must not mutate or discard the EditPlan.
3. CompositionPlayer failure falls back to ExoPlayer live effects before source-only mode.
4. Intro Freeze and adaptive candidate/sequence inspection remain explicit ExoPlayer preview paths in this gate.
5. Trim/Adaptive/Speed playhead mapping preserves semantic source position across Composition rebuilds.
6. Replace/Mix audio is owned by the Composition while CompositionPlayer is active; the legacy second audio player is disabled to prevent doubled audio.
7. Final Export snapshots one immutable EditPlan and starts Transformer exactly once.
8. Final H.264 export preserves source-aware 24-60fps policy and exact 720p/1080p/2K geometry.
9. Final H.264 encoder request uses CBR for predictable owner-device visual quality.
10. CBR average bitrate below 80% of the requested target is a validation failure; 80-90% remains visible as a quality warning.
11. Duration validation uses a 350 ms floor, the existing 0.1% proportional allowance, and a 750 ms hard cap; drift above 250 ms remains visible as a warning.

## Phase 6F.2.8.1 quality targets

| Preset | <=30fps | 48-60fps |
|---|---:|---:|
| 720p | 7.5 Mbps | 10 Mbps |
| 1080p | 10 Mbps | 15 Mbps |
| 2K / 1440p | 18 Mbps | 28 Mbps |

## Owner-device evidence that triggered this hotfix

- 1080p source: 1080 x 1920, 30fps, approximately 8.60 Mbps.
- Prior VBR test requested 10 Mbps but reported only 2.78 Mbps average and produced visibly soft/blurry output.
- The duration hotfix independently demonstrated that a 268-315 ms finalization drift is realistic on the tested device and must not be mistaken for a timeline mismatch.

## Merge gate

Do not mark Phase 6F.2.8.1 complete until the new CBR test source is built and owner-tested at both 1080p and 720p. Acceptance requires publishable visual quality, sane requested/actual bitrate telemetry, preserved source-aware FPS, bounded duration/A-V sync, and no intermediate-render regression.
