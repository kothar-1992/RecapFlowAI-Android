# Phase 6F.2.8.1 Current Gate

This companion gate file exists to keep the active hotfix contract explicit without rewriting the historical top-level `PLAN.md` during an unverified device-quality iteration.

- Branch: `hotfix/phase-6f2.8.1-render-quality`
- Issue: #17
- Bundled presentation cleanup: #13
- Status: source implemented; Termux branch build + owner-device CBR quality verification pending
- Final encoder request: H.264 CBR
- Final FPS: source-aware 24-60fps
- Targets: 720p 7.5/10 Mbps, 1080p 10/15 Mbps, 1440p 18/28 Mbps for normal/high-FPS classes
- Duration validator: warning above 250 ms; 350 ms floor; 0.1% proportional allowance; 750 ms cap
- Bitrate quality gate: <80% target fails when average telemetry is available; 80-90% warns
- Preview/export architecture unchanged: immutable EditPlan, CompositionPlayer/ExoPlayer preview fallbacks, exactly one final Transformer render, no intermediate MP4

## Acceptance

- [ ] source verifier PASS on synced branch
- [ ] Termux unit/build PASS
- [ ] owner-device 1080p CBR output materially sharper than prior 2.78 Mbps VBR sample
- [ ] owner-device 720p CBR output no longer soft/blurry
- [ ] actual bitrate >=80% target when reported
- [ ] geometry/FPS/H.264/AAC/A-V sync/duration/Gallery diagnostics sane
- [ ] no intermediate-render regression

After device PASS, fold this gate into the top-level `PLAN.md` completion history, merge the hotfix PR, close #17 and #13, then unblock Phase 6G.1.
