# Phase 6F.2.8.1 Status

Source implementation is complete on `hotfix/phase-6f2.8.1-render-quality`.

Verification status as of 2026-08-29:

- Phase 6F.2.7 remains the verified preview baseline; this hotfix changes final-export quality enforcement and bounded duration validation only.
- First synced-branch unit run: **169 passed / 3 failed** (172 total). The three failures were diagnosed as stale-local/contract-test drift rather than CBR encoder implementation failure:
  - Burmese export names lost combining Unicode marks in `PublicExportNamePolicy`; production sanitizer now preserves Mn/Mc/Me marks.
  - `FullEditPlanCombinationRegressionTest` still expected the historical 25/30/45 Mbps quality budget; expectations now match Phase 6F.2.8.1 30fps targets 7.5/10/18 Mbps.
  - `Media3CompositionPlanCompilerTest` requested an unsupported 1.5 s freeze while `FreezeCompiler` intentionally supports 1/2/3 s; the topology test now uses the supported 2 s choice.
- Local repository diagnostics proved the device checkout had been behind remote and contained local modifications; the recovery path is stash -> exact remote sync -> clean re-test rather than discarding local work.
- **720p owner-device CBR gate: PASS.** Finalized output reported 720x1280, H.264 CBR target 7.50 Mbps, actual 7.50 Mbps, AAC audio, 280 MB, and 268 ms duration drift inside the bounded 350 ms tolerance. Gallery publication succeeded.
- **1080p owner-device CBR gate: PASS for finalized encoder/quality telemetry.** Finalized output reported 1080x1920, H.264 CBR target 10.00 Mbps, actual 9.11 Mbps (91.1% of target), AAC audio, 195 MB, validated geometry/codec/audio/duration policy, and successful Gallery publication. Owner previously reported the CBR output is visibly better than the prior VBR/2.78 Mbps soft result.
- The 1080p sample source short side was 720 px, so the 1080p result is an upscale; CBR prevents additional compression collapse but cannot recreate detail absent from the 720p source.
- Source-aware FPS policy and one-final-render architecture remain unchanged by this hotfix.
- Remaining merge gates are procedural/technical verification only: record a clean synced-branch source-verifier PASS, clean unit-test PASS, and synced-branch `assembleDebug` PASS. A/V-sync sanity should be confirmed from playback evidence before final merge if not already observed.
- PR #18 remains draft; Issues #17 and #13 remain open until the remaining verification gates are recorded. Phase 6G.1 remains blocked until merge.
