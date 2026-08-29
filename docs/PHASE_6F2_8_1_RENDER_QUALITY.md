# Phase 6F.2.8.1 — Render Quality Hotfix

Date: 2026-08-29
Branch: `hotfix/phase-6f2.8.1-render-quality`
Issue: #17

## Trigger

Owner-device testing showed that the Phase 6F.2.8 VBR policy could report a much lower average bitrate than requested on the tested MediaTek H.264 encoder. A 1080p / 30fps source at about 8.60 Mbps was rendered with a 10 Mbps request but only about 2.78 Mbps reported average bitrate, producing visibly soft output. The same testing also confirmed that a 250 ms duration-floor was too strict for normal MediaCodec/AAC/container finalization drift.

## Hotfix contract

- Preserve source-aware 24-60fps final export.
- Preserve the shared immutable EditPlan and CompositionPlayer preview architecture.
- Preserve exactly one final `Transformer.start(...)` call and no intermediate MP4 renders.
- Request H.264 CBR instead of VBR for the final export.
- Use quality-master bitrate targets:
  - 720p: 7.5 Mbps <=30fps / 10 Mbps 48-60fps
  - 1080p: 10 Mbps <=30fps / 15 Mbps 48-60fps
  - 1440p: 18 Mbps <=30fps / 28 Mbps 48-60fps
- Reject a finalized output when reported average video bitrate is below 80% of the CBR target.
- Warn when reported average bitrate is 80-90% of target.
- Warn, but do not false-fail, when the encoder does not report average video bitrate.
- Use a 350 ms duration floor, retain the 0.1% proportional rule and 750 ms hard cap, and keep drift above 250 ms visible as a warning.
- Remove stale Phase 6F.2.6/VBR-era presentation labels from the supported API range.

## Regression coverage

The test source includes checks for:

- exact owner case `277000 -> 277315 ms` passes;
- an 800 ms mismatch still fails;
- 1080p 30fps target is 10 Mbps;
- 1080p 60fps target is 15 Mbps;
- 720p target is 7.5/10 Mbps;
- 1440p target is 18/28 Mbps;
- a 2.78 Mbps average against a 10 Mbps target fails the CBR quality gate;
- exactly 80% of target passes with a warning;
- source-aware frame-rate validation remains active.

## Device acceptance

Do not merge or close #17 until a build from this branch is owner-tested. Required evidence:

- 1080p render is visually publishable and substantially sharper than the 2.78 Mbps VBR sample;
- 720p output no longer shows the reported soft/blurry regression;
- actual bitrate is at least 80% of the requested CBR target when telemetry is available;
- geometry, H.264/AAC, FPS, A/V sync, duration and Gallery export remain sane;
- no intermediate-render regression.
