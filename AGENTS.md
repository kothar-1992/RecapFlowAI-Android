# AGENTS.md — RecapFlowAI-Android

Android video editor (single-module `:app`, namespace `com.recapflow.ai`). Focus: Myanmar localization + Media3-based video editing with optional native FFmpeg/JNI bridge.

## Current state

- **Branch**: `feature/phase-6ux2-side-menu` (builds on Phase 6F.2.8.1 CBR baseline)
- **Version**: `1.0-phase6ux2` ( versionName in `app/build.gradle.kts` )
- `app/src/main/kotlin/com/recapflow/ai/MainActivity.kt` is ~6400 lines, monolithic — phase changes are applied programmatically, not hand-edited.
- Phase label copy lives in `app/src/main/res/values-v28/phase_6ux2_strings.xml` and must stay
  in sync with `versionName`. `verify_phase6f2_8_1_source.sh` enforces this; never change one
  without the other.

## Toolchain

| Tool | Version |
|---|---|
| Gradle | 9.0.0 (wrapper) |
| AGP | 8.13.0 |
| Kotlin | 2.1.0 |
| JVM target | 17 |
| compileSdk | 36 |
| minSdk / targetSdk | 28 / 34 |
| NDK | 24.0.8215888 |
| CMake | 3.18.1 |
| Media3 | 1.10.0 |
| ABI | `arm64-v8a` only |

Version catalog: `gradle/libs.versions.toml`.

## Build & test commands

```bash
# Run the current phase source verifier first (grep-based invariants)
bash scripts/verify_phase6ux2a_side_menu.sh

# Unit tests (pure JVM, no emulator needed)
./gradlew :app:testDebugUnitTest

# Debug APK
./gradlew :app:assembleDebug
```

Windows uses `gradlew.bat`; Unix uses `./gradlew`.

### Termux / AndroidIDE (owner device)

On-device builds use a system Gradle instead of the wrapper and require an `aapt2` override (the bundled wrapper aapt2 can crash on Android):

```bash
AAPT2_BIN="$PREFIX/bin/aapt2"
"$HOME/.local/opt/gradle-9.0.0/bin/gradle" :app:testDebugUnitTest \
  -Precapflow.ffmpeg.enabled=true \
  -Pandroid.aapt2FromMavenOverride="$AAPT2_BIN" \
  --no-daemon --max-workers=2 --stacktrace

"$HOME/.local/opt/gradle-9.0.0/bin/gradle" :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true \
  -Pandroid.aapt2FromMavenOverride="$AAPT2_BIN" \
  --no-daemon --max-workers=2 --stacktrace
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Build flags (gradle.properties)

Defined in `gradle.properties`; optional ones have defaults in `app/build.gradle.kts`:

| Property | Default | Effect |
|---|---|---|
| `recapflow.ffmpeg.enabled` | `false` | CMake `RECAPFLOW_ENABLE_FFMPEG` — controls native FFmpeg linking |
| `recapflow.composition.preview.enabled` | `true` | BuildConfig `ENABLE_COMPOSITION_PLAYER_PREVIEW` |
| `recapflow.crossfade.runtime.enabled` | `false` | BuildConfig `ENABLE_CROSSFADE_RUNTIME_SPIKE` |

FFmpeg native libs are **gitignored** (`app/src/main/cpp/ffmpeg/prebuilt/arm64-v8a/lib/*.a` and `include/`). Building with `-Precapflow.ffmpeg.enabled=true` requires the prebuilt SDK to exist first (see Native build below).

## Source verifiers

`scripts/verify_phase*.sh` — grep-based invariant checks, **not** Gradle tests. Each phase has its own verifier. Run the current phase verifier before any build/test. Current phase:

```bash
bash scripts/verify_phase6ux2a_side_menu.sh
```

These verify structural contracts: required files exist, specific markers/grep patterns are present or absent (e.g., no VBR after the CBR hotfix, no auth/AdMob SDKs), exact version strings, and architecture invariants (e.g., exactly one `Transformer.start` call in final export).

Only the current phase verifier and the CBR baseline verifier are expected to pass. The older
per-phase scripts still pin superseded `settings.gradle.kts` project names and old `versionName`
values, so a full-suite run reports roughly 40 historical failures. That is known drift, not a
regression — judge a change by the current phase verifier, the CBR baseline, and the unit tests.

`.gitattributes` pins `*.sh` to `eol=lf`. A CRLF verifier fails at `set -euo pipefail` with
`set: pipefail: invalid option name`, which looks like a content failure but is not one. Keep
shell verifiers LF even on a Windows checkout with `core.autocrlf=true`.

## Device verification

The phase device target is an **LDPlayer Android 14 (API 34) emulator** reached over adb, not a
Mi Pad. Its 1280x720 @ 240 dpi screen is 853 dp wide, so it exercises `layout-sw600dp`. The image
is `x86_64` but advertises `arm64-v8a` in `ro.product.cpu.abilist`, so the `arm64-v8a`-only APK
installs and runs through the native bridge. Results and open items are recorded in
`docs/PHASE6UX2_LDPLAYER_VERIFICATION.md`.

```bash
adb devices -l
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell cmd locale set-app-locales com.recapflow.ai --user 0 --locales my
```

The `--user 0` flag is required; without it the locale command reports success but changes
nothing. An emulator is not a substitute for a physical arm64 tablet for render quality or
FFmpeg native behaviour.

## Apply scripts

`scripts/apply_phase*.py` — Python scripts that programmatically patch source files for a phase. They are idempotent: check for a phase marker and skip if already applied. Current:

```bash
python3 scripts/apply_phase6ux2a_side_menu.py
```

These scripts assert exactly one anchor match before replacing — they will fail loudly if the source has drifted.

## Native build

- CMake config: `app/src/main/cpp/CMakeLists.txt`
- NDK r24 requires explicit ELF 16 KB page alignment (`-Wl,-z,max-page-size=16384`).
- Native build staging dir is `~/.recapflow/cxx/{project}/{module}` — this avoids Android shared-storage timestamp rebuild loops.

### Building FFmpeg (only when native libs are missing)

```bash
./scripts/build_ffmpeg_android_arm64.sh /path/to/extracted-ffmpeg-source
```

This script:
- Requires NDK 24.0.8215888 at `$ANDROID_NDK_HOME` (or `$ANDROID_SDK_ROOT/ndk/24.0.8215888`).
- Requires `make` (install in AndroidIDE: `pkg install make`).
- Copies the source tree under `$HOME` first (Android shared storage doesn't preserve executable bits).
- Builds static libs (`avutil`, `avcodec`, `avformat`, `avfilter`, `swscale`, `swresample`) for `arm64-v8a`.
- Installs to `app/src/main/cpp/ffmpeg/prebuilt/arm64-v8a/{include,lib}`.

Then build with `-Precapflow.ffmpeg.enabled=true`.

## Project conventions

- **Localization**: `values/` (English) + `values-my/` (Myanmar). Myanmar strings must use ASCII digits 0-9, **not** Myanmar numerals (၀-၉). External URLs (Telegram, Facebook, email) must not be localized — they live in `side_menu_destinations.xml` and must be HTTPS.
- **Static copy with `%`**: escape it as `%%`. aapt2 warns on a bare `%`, and `getString(id, args)` on such a string throws at runtime. Positional `%1$s` is fine.
- **Version-specific strings** go in `values-v28/phase_*.xml`.
- **Android resource qualifiers**: locale has the highest precedence and platform version (`-v28`)
  the lowest, so `values-my` still overrides `values-v28`. A `-v28` override does not suppress
  localization.
- **UI decisions belong in a `*Policy` object** with JVM unit tests, not inline in a controller.
  `SideMenuPolicy` is the reference for the side menu; it uses `java.net.URI` rather than
  `android.net.Uri` so it stays testable off-device.
- **EditorPreferencesStore** must remain metadata-only — no source URIs, file paths, or tokens.
- **Final export** snapshots one immutable EditPlan and calls `Transformer.start` exactly once (verified by source checkers).
- **MainActivity back-press**: the side drawer must get priority — close it before navigating home.

## Worktrees

The repo contains an Agent Manager worktree at `.kilo/worktrees/rune-vault` (branch `rune-vault`). Work in the main working directory only.
