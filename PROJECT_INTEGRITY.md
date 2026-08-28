# RecapFlowAI Phase 6F.2.8 — Project Integrity

Date: 2026-08-28

This AndroidIDE test package continues from the owner-confirmed Phase 6F.2.6.2 stable baseline merged to
`kothar-1992/RecapFlowAI-Android` at `7411b54ba922c49a28fde4ea7e0250b50d019900`.
It adds a feature-flagged CompositionPlayer preview path only; the final export pipeline remains the
verified one-EditPlan / one-Composition / one-Transformer path.

- Root project: `RecapFlowAI_Phase6F2_8`
- Application ID: `com.recapflow.ai`
- Version: `1.0-phase6f2.8`
- AndroidX Media3: `1.10.0`
- FFmpeg baseline: `9.0.1` ARM64 static integration
- Preview flag: `recapflow.composition.preview.enabled` (default `true`)
- Preferred preview: shared EditPlan -> Media3 Composition -> CompositionPlayer
- First fallback: ExoPlayer live effects
- Second fallback: ExoPlayer source-only preview
- Final export: one Media3 `Composition` -> one `Transformer.start(...)`
- Current gate: AndroidIDE compile/install + owner-device Phase 6F.2.8 matrix

## Required invariants

1. Clips, Transform, Audio and Overlay remain editable without intermediate MP4 renders.
2. CompositionPlayer failure must not mutate or discard the EditPlan.
3. CompositionPlayer failure falls back to ExoPlayer live effects before source-only mode.
4. Intro Freeze and adaptive candidate/sequence inspection remain explicit ExoPlayer preview paths in this gate.
5. Trim/Adaptive/Speed playhead mapping preserves semantic source position across Composition rebuilds.
6. Replace/Mix audio is owned by the Composition while CompositionPlayer is active; the legacy second audio player is disabled to prevent doubled audio.
7. Final Export snapshots one immutable EditPlan and starts Transformer exactly once.
8. 720p/1080p/2K exact geometry and the high-bitrate quality policy remain unchanged by this preview phase.


## Phase 6F.2.8 quality invariant
Final export uses source-aware 24-60fps targets and VBR social-upload bitrate targets. CompositionPlayer preview remains the verified Phase 6F.2.7 path and final export remains one Transformer start.
