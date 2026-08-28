# Phase 6F.2.8 — AndroidIDE / Device Verification

## Build gate

```bash
bash scripts/verify_phase6f2_8_source.sh
./gradlew :app:testDebugUnitTest --no-daemon --max-workers=2
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true -Precapflow.composition.preview.enabled=true --no-daemon --max-workers=2
```

## Device matrix

1. Import a real 1080p ~30fps source. Keep a representative combined EditPlan (Trim/Transform/Audio/Blur/Logo) and verify live preview still behaves like Phase 6F.2.7.
2. Export 1080p once. Expected request: H.264 VBR, ~8 Mbps, source-aware ~24/25/30fps. Confirm output geometry, audio policy, duration, A/V sync, requested/average bitrate and final FPS metadata/log.
3. If a real 48/50/60fps source is available, export 1080p once. Expected request: ~12 Mbps and preserved high-FPS class up to 60fps. A 30fps source must never be promoted to 60fps.
4. Verify no intermediate MP4 is created while editing and only Final Export starts Transformer.
5. If finalized track FPS metadata is absent, the app may warn rather than fail; record external/media-info FPS evidence for the device matrix.
6. Compare file size and visual quality against the former 30–45 Mbps policy.

Do not merge PR #12 / close #11 until AndroidIDE build and owner-device matrix pass.
