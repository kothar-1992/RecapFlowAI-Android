# Phase 6UX.2A/B Side Menu — LDPlayer Device Verification

- Result: **PASS with one pre-existing defect found and fixed**
- Date: 2026-09-27
- Branch: `feature/phase-6ux2-side-menu`
- Verified commit: `7a9ae43` plus the Phase 6UX.2 close-out in this change set
- Device: **LDPlayer Android 14 emulator (API 34)**, replacing the Mi Pad as the verification target
- Verifier: `scripts/verify_phase6ux2a_side_menu.sh` (PASS)

## Device configuration

| Property | Value |
|---|---|
| Android version | 14 (API 34) — matches `targetSdk` |
| Primary ABI | `x86_64` |
| `ro.product.cpu.abilist` | `x86_64, arm64-v8a, x86, armeabi-v7a, armeabi` |
| Screen | 1280×720 @ 240 dpi = **853 dp × 480 dp** |
| Smallest-width class | `sw600dp` → `layout-sw600dp/activity_main.xml` active |
| Build characteristics | `tablet` |
| Default locale | `en-US` |

The app ships `arm64-v8a` only. The install and launch succeeded because the LDPlayer image
advertises `arm64-v8a` in its `abilist`, so the package runs through the emulator's native bridge.
This confirms the APK is loadable on a non-arm64 host, but it does **not** replace a real
arm64 tablet run for performance or FFmpeg native behaviour.

## Verified

1. **Install and launch** — `adb install -r` succeeded; `MainActivity` started with an empty
   crash buffer and a live process.
2. **Tablet layout** — the 853 dp width selected `layout-sw600dp/activity_main.xml`. The drawer
   rendered at its `sw600dp` width (360 dp) and the root stayed `DrawerLayout` > `mainRoot`.
3. **Drawer open** — the toolbar hamburger opened the drawer; the scrim dimmed the content and
   the bottom navigation stayed mounted behind it.
4. **Drawer header** — app name, account name (`ဧည့်သည်` / Guest), user level
   (`အသုံးပြုသူအဆင့်: ဧည့်သည်`), and the version line all rendered.
5. **Version line** — the header and the *App version* dialog both showed
   **`Version 1.0-phase6ux2 (1)`**. Digits rendered as ASCII `0-9` under the Myanmar locale, with
   no Myanmar numerals, confirming the `versionName` bump and the `SideMenuPolicy` ASCII
   guarantee.
6. **Drawer auto-close** — selecting a menu item closed the drawer, matching the
   `if (handled) close()` contract.
7. **Back-press priority** — with the drawer open on the Settings destination, `KEYCODE_BACK`
   closed the drawer and left the destination on Settings. It did not navigate to Home. This is
   the priority order the side drawer requires.
8. **Myanmar localization** — switching to `my` from Settings → *App language* translated the
   whole shell: window title, home copy, bottom-navigation labels, every drawer section
   (account / community / legal / about), and all drawer item titles.
9. **Legal dialog** — *App policy* opened a `MaterialAlertDialog` with the Myanmar legal body copy
   rendering in full without truncation.

## Defects found and fixed during verification

**1. The toolbar and phase badges announced a stale phase.** `values-v28/phase_6f2_8_1_strings.xml`
hardcoded `Phase 6F.2.8.1` into seven strings, so the Home and Settings subtitles read
`Local media workspace • Phase 6F.2.8.1` and `On-device profile • Phase 6F.2.8.1` even though
`versionName` had moved on. The version label is a second, hand-maintained copy of the phase and
had already drifted from `versionName`.

Fixed by moving the phase-labelled copy into `values-v28/phase_6ux2_strings.xml` with the
`6UX.2` label, leaving only the unlabelled H.264 CBR telemetry strings in the 6F.2.8.1 file.
`verify_phase6f2_8_1_source.sh` now normalizes `versionName` and every phase label it finds and
fails when they disagree, so this cannot silently drift again. Re-verified on device: the subtitle
reads `Local media workspace • Phase 6UX.2`.

**2. The drawer hamburger always announced "Open app menu".** `R.string.drawer_close` was
defined in both `values/` and `values-my/` but never referenced, because the content description
was written once in `bind()` and never followed the drawer state. TalkBack therefore told users to
open a menu that was already open.

Fixed with a `DrawerLayout.SimpleDrawerListener` that swaps the label and rotates the
`DrawerArrowDrawable` between states. Re-verified with `uiautomator dump`: `content-desc="Open app
menu"` when closed and `content-desc="Close app menu"` when open.

**3. The disabled "Sign in (coming later)" row looked enabled.** `app:itemTextColor` and
`app:itemIconTint` on the `NavigationView` were plain colour resources, which replaces Material's
own disabled state list. The row was non-clickable but rendered in the same near-black as every
live action, breaking the "state is always visible" principle in `docs/UI_UX_PLAN.md`.

Fixed with `color/side_drawer_item_text.xml` and `color/side_drawer_item_icon.xml` state lists
that map `state_enabled="false"` to `rf_outline`. Re-verified on device: the row is visibly
muted while enabled rows are unchanged.

A hypothesis that `values-v28` would suppress the Myanmar translations was checked and
**disproved**: Android gives locale the highest resource-qualifier precedence, and the platform
version qualifier the lowest, so `values-my` still wins. No localization change was needed.

## Not verified on this device

These remain open and still need a physical arm64 tablet:

- Compact `layout/activity_main.xml` (needs a sub-600 dp width).
- Portrait, landscape, multi-window, dark mode, and large-font rendering. The new disabled-state
  tints are defined for both `values/` and `values-night/`, but only the light theme was seen.
- External intents actually leaving the app: `mailto:`, `https://t.me/`, and the Facebook share
  URL. The HTTPS gate and the `ActivityNotFoundException` fallback are unit-tested, but no
  handler app was confirmed on the emulator.
- The full render and playback path, which is the Phase 6F.2.8.1 owner gate and is unchanged here.
- Any native FFmpeg path, since this build ran without `-Precapflow.ffmpeg.enabled=true`.

## Reproducing

```bash
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew :app:assembleDebug
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.recapflow.ai/.MainActivity
```

Switch to Burmese with `adb -s emulator-5554 shell cmd locale set-app-locales com.recapflow.ai
--user 0 --locales my`. The `--user 0` flag is required on this image; without it the command
silently reports success and leaves the locale at `[en]`.
