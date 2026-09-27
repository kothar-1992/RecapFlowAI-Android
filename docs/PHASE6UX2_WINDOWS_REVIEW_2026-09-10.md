# Side-menu branch review — 2026-09-10

## Changes and source findings

PR #34 targets `feature/phase-6h1-transitions`. Its previously published head
`246d4e6` contained the drawer components and A/B actions but omitted the locally
applied MainActivity binding. This update includes that integration in source:
the toolbar opens the drawer and Activity Back closes it before normal navigation.
The existing bottom navigation and media pipeline remain the same.

Windows build helpers include the quoted FFmpeg property and a project-local
Java Unix-domain socket directory. The `.bat` entry point works with PowerShell
script execution disabled and restores process-local settings when finished.

## Verification

- `scripts/verify_phase6ux2a_side_menu.sh`: PASS using Git Bash.
- `scripts/verify_myanmar_localization.py`: PASS, 553 strings, ASCII digits.
- Windows Java 17 / Gradle 9.0.0 `:app:testDebugUnitTest` and `:app:assembleDebug`
  with FFmpeg and Crossfade runtime enabled: BUILD SUCCESSFUL in 54s.
- JUnit XML: 250 tests across 51 suites, 0 failures/errors/skips.
- Extended `.bat` helper with the same test/build arguments: BUILD SUCCESSFUL
  in 13s (incremental confirmation).
- Animated-logo foundation/GL/UI source verifiers: PASS.
- APK: `app/build/outputs/apk/debug/app-debug.apk` (ARM64).
- Source checks retain one final `Transformer.start(...)` render path.
- SDK metadata/CMake/deprecation warnings remain. Kotlin daemon compilation
  fell back to non-daemon compilation during the combined run and the run passed.
- ADB listed no attached devices; no new runtime acceptance is claimed.

Repeat the Windows gate from the repository root:

```powershell
.\scripts\build_debug.bat :app:testDebugUnitTest "-Precapflow.crossfade.runtime.enabled=true"
```

This Windows gate supplements the established Termux/device release gates.

## Device acceptance still required for B

1. Open the drawer on Home, Editor and Settings; confirm the scrim blocks editor
   touches and Back closes the drawer without navigating away first.
2. Contact developer opens an email draft with the configured address and subject.
   Do not send mail as part of this check.
3. Telegram and Facebook open their configured destinations.
4. On a device/profile without a suitable handler, confirm a friendly fallback
   and no crash; blank/invalid configured destinations must also fail gracefully.
5. Open App Policy, Privacy Policy, Terms and Open-source notices; check all text
   is accessible and dismiss each dialog.
6. Repeat in English and Myanmar; verify version code remains ASCII `(1)`.
7. Return to the editor and confirm the reviewed project state is preserved.

## GitHub and next-work reconciliation

- #30 / PR #31: Target-duration Clips already complete and merged.
- #21 / PR #32: animated-logo implementation accepted and merged as `599eb91b`;
  the issue and PR's old merge-preparation wording need completion updates.
- #33 / PR #34: A previously owner-accepted; B current source/build verified;
  B device acceptance pending. Keep Draft and keep Issue #33 open.
- PR #25: transition stack still Draft; Crossfade device/export/A-V gates remain.
- After B acceptance: 6UX.2C account model boundary, then the coordinated #4 SRT /
  #22 Narrator media work. Play Store, authentication and AdMob remain deferred.
