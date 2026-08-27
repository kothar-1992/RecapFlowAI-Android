# Phase 6E.3A — Platform and Media3 baseline stabilization

## Goal

Adopt the owner-tested Android build stack in the latest Phase 6E.2.1 source without
changing editor or export semantics.

## Accepted baseline

| Component | Version |
|---|---|
| minSdk | 28 |
| compileSdk / targetSdk | 36 / 34 |
| AGP / Gradle | 8.13.0 / 9.0.0 |
| Kotlin / JVM | 2.1.0 / 17 |
| Core KTX | 1.16.0 |
| AppCompat | 1.7.1 |
| Material | 1.13.0 |
| ConstraintLayout | 2.1.4 |
| Media3 | 1.10.0 |
| NDK / CMake | 24.0.8215888 / 3.18.1 |
| ABI | arm64-v8a |

## Included

- Update the version catalog and application minimum SDK.
- Give the source a Phase 6E.3A project/version identity.
- Remove the now-unreachable API 21–22 local-render rejection.
- Keep the Android 9 legacy media permission fallback.
- Keep all Phase 6E.2.1 source-blur, static-logo, preview, and export behavior.
- Keep direct preview-touch blur/logo editing disabled.
- Add exact-version and regression source checks.
- Document AndroidIDE and on-device acceptance checks.

## Excluded

- Preview-session architecture changes.
- Public MediaStore/Gallery output.
- Save/reset editor presets.
- Logo animation loops.
- Telegram, Gemini, or other network integrations.
- Direct screen-touch effect manipulation.

## Acceptance

- `scripts/verify_phase6e3a_source.sh` passes.
- Kotlin unit tests compile and pass.
- `:app:assembleDebug` succeeds with FFmpeg disabled and enabled in AndroidIDE.
- The installed app completes the Phase 6E.3A device matrix.
- No effect or render becomes enabled by default because of the upgrade.

