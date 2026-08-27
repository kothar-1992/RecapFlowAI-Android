# Phase 6F.2.1 implementation status — 2026-08-27

## Result

Source implementation is complete. AndroidIDE compilation and owner-device pixel verification are
pending.

## Changes

- Replaced the sparse 13-tap blur with a normalized dense 9×9 localized kernel.
- Added rectangle-clamped sampling to prevent wrap/mirror subtitle copies.
- Added `SourceSubtitleBlurKernelPolicy` with bounded server-parity strength mapping and
  region-aware scaling.
- Kept the shared realtime/export `SourceSubtitleBlurEffect`, retained-state bridge, active source
  time, immutable render snapshot, and Transform → Blur → Logo order unchanged.
- Added kernel-policy unit tests and `scripts/verify_phase6f2_1_source.sh`.
- Advanced source identity to `RecapFlowAI_Phase6F2_1` / `1.0-phase6f2.1`.

## Verification level

- Source preflight: PASS (`bash scripts/verify_phase6f2_1_source.sh`), including retained preview,
  public export, encoder quality, preferences, shader, resource, and Kotlin delimiter guards.
- Kotlin/JVM tests: not executed because the delivery host has no cached Gradle 9.0 distribution
  and cannot reach `services.gradle.org`; the new test source passed static structure checks.
- Android build/package: blocked on the delivery host because no Android SDK is configured and the
  Gradle distribution cannot be downloaded. AndroidIDE remains the decisive build environment.
- Runtime/media: requires the owner's device and the original subtitle-bearing source.
