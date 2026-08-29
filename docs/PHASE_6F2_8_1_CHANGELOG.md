# Phase 6F.2.8.1 Change Summary

- Final H.264 encoder request: VBR -> CBR.
- 720p quality targets: 7.5 Mbps <=30fps / 10 Mbps high FPS.
- 1080p quality targets: 10 Mbps <=30fps / 15 Mbps high FPS.
- 1440p quality targets: 18 Mbps <=30fps / 28 Mbps high FPS.
- Reported average bitrate <80% target now fails finalized-output quality validation.
- 80-90% target produces a quality warning.
- Missing average bitrate telemetry warns without false failure.
- Duration validation floor: 350 ms; warning starts above 250 ms; 0.1% proportional allowance and 750 ms cap preserved.
- Stale Phase 6F.2.6 export presentation copy is overridden on the supported API range.
- Version marker updated to `1.0-phase6f2.8.1`.
- Source-aware 24-60fps, immutable EditPlan, CompositionPlayer preview and exactly one final Transformer render are preserved.
