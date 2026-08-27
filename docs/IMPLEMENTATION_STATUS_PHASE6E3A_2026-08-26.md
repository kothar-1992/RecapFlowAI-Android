# Phase 6E.3A implementation status — 2026-08-26

## Implemented

- Merged the owner-approved dependency stack into the latest Phase 6E.2.1 source.
- Raised minSdk from 21 to 28.
- Updated Core KTX to 1.16.0, AppCompat to 1.7.1, and Media3 to 1.10.0.
- Retained AGP 8.13.0, Kotlin 2.1.0, Material 1.13.0, ConstraintLayout 2.1.4,
  Gradle 9.0.0, compileSdk 36, targetSdk 34, JVM 17, NDK 24, and CMake 3.18.1.
- Removed the unreachable pre-API-23 render gate and retained the API 28 legacy picker
  permission path.
- Updated notices, the implementation plan, task scope, and source verification.

## Evidence boundary

The owner reported a successful AndroidIDE build after applying this version stack. That
confirms the versions are usable in the owner's environment. The merged latest-source
`1.0-phase6e3a` archive still requires the documented build/install and device regression
matrix before this gate can be marked fully verified.

The chained Phase 6E.1, Phase 6E.2, Phase 6E.2.1, and Phase 6E.3A source preflights pass in
the delivery workspace. A local `:app:testDebugUnitTest :app:assembleDebug` attempt could
not start because the Gradle 9 distribution was not cached and this workspace cannot reach
`services.gradle.org`; this is recorded as an environment blocker, not as a build failure in
the Android source.

## Next gate

Phase 6E.3B will address retained realtime-preview session stability and effect switching
without requiring an intermediate render. Public Gallery export and Save/Reset preferences
remain separate Phase 6F gates.
