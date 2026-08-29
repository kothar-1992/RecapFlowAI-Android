# Phase 6F.2.8.1 Current Gate

This companion gate file exists to keep the active hotfix contract explicit without rewriting the historical top-level `PLAN.md` during an unverified device-quality iteration.

- Branch: `hotfix/phase-6f2.8.1-render-quality`
- Issue: #17
- PR: #18 (draft)
- Bundled presentation cleanup: #13
- Status: hotfix is running on the owner device; 1080p CBR visual quality is improved versus the prior VBR sample, but final bitrate/metadata acceptance and 720p verification are still pending
- Final encoder request: H.264 CBR
- Final FPS: source-aware 24-60fps
- Targets: 720p 7.5/10 Mbps, 1080p 10/15 Mbps, 1440p 18/28 Mbps for normal/high-FPS classes
- Duration validator: warning above 250 ms; 350 ms floor; 0.1% proportional allowance; 750 ms cap
- Bitrate quality gate: <80% target fails when average telemetry is available; 80-90% warns
- Preview/export architecture unchanged: immutable EditPlan, CompositionPlayer/ExoPlayer preview fallbacks, exactly one final Transformer render, no intermediate MP4

## Latest owner-device evidence — 2026-08-29

- Phase 6F.2.8.1 APK is installed and actively rendering on-device.
- Export UI reports `Rendering 1080p` with `H.264 CBR quality target: 10.00 Mbps`.
- Owner reports the rendered-video visual quality is noticeably better than the prior soft/blurry VBR result.
- This is positive visual evidence only; do not declare the 1080p quality gate complete until the finalized result reports/validates actual average bitrate, geometry, FPS, duration, codec/audio, A/V sync and Gallery publication.
- The earlier local Termux unit failure set (169 pass / 3 fail) was traced to a stale local working copy and contract/test drift; a clean synced-branch re-test result still needs to be recorded explicitly before merge.

## Acceptance

- [ ] source verifier PASS on synced branch (explicit result not yet recorded)
- [ ] Termux unit test PASS on synced branch
- [ ] Termux/AndroidIDE assembleDebug PASS explicitly recorded for the synced branch
- [~] owner-device 1080p CBR output materially sharper than prior 2.78 Mbps VBR sample — visually improved; final telemetry gate pending
- [ ] owner-device 720p CBR output no longer soft/blurry
- [ ] actual bitrate >=80% target when reported
- [ ] geometry/FPS/H.264/AAC/A-V sync/duration/Gallery diagnostics sane
- [ ] no intermediate-render regression

After complete device PASS, fold this gate into the top-level `PLAN.md` completion history, merge PR #18, close #17 and #13, then unblock Phase 6G.1. Phase 6F.2.7 remains the verified preview baseline and is not reopened by this quality-only hotfix.
