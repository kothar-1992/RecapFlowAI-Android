# RecapFlowAI Phase 6F.2.6.2 — Project Integrity

Date: 2026-08-28

This package is the continuation of the owner-device-verified Phase 6F.2.6.1 hotfix line.
It does not add a second render pipeline. It strengthens the existing one-EditPlan / one-final-render
contract with a full combination regression gate before the project is frozen as a GitHub baseline.

- Root project: `RecapFlowAI_Phase6F2_6_2`
- Application ID: `com.recapflow.ai`
- Version: `1.0-phase6f2.6.2`
- FFmpeg baseline: 9.0.1 ARM64 static integration
- Final export: one Media3 `Composition` -> one `Transformer.start(...)`
- Current gate: Phase 6F.2.6.2 Full EditPlan Combination Regression
- Next milestone after owner-device PASS: freeze/tag verified baseline, then push the new GitHub repository and open queued Issues/PRs.

## Required invariants

1. Clips, Transform, Audio and Overlay settings remain editable without intermediate MP4 renders.
2. Final Export snapshots one immutable `EditPlan` and renders it once.
3. Master switches omit disabled operations without deleting remembered child settings.
4. Adaptive ranges, freeze, speed, audio policy, blur/logo windows and export geometry compile together.
5. 720p/1080p/2K validation remains exact and high-bitrate quality policy remains enabled.
6. Preview failure must not invalidate the saved EditPlan or block a valid final render.
