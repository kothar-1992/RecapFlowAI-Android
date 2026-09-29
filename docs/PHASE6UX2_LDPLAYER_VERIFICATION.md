# Phase 6UX.2A/B Side Menu — LDPlayer Device Verification

- Result: **PASS with four pre-existing defects found and fixed**
- Date: 2026-09-27, extended 2026-09-30 (see *Follow-up session*)
- Branch: `feature/phase-6ux2-side-menu`
- Verified commit: `7a9ae43` plus the Phase 6UX.2 close-out; follow-up verified at `4a0d161`
- Device: **LDPlayer Android 14 emulator (API 34)**, replacing the Mi Pad as the verification target
- Verifier: `scripts/verify_gate.sh` (PASS)

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

- Portrait, landscape, multi-window, dark mode, and large-font rendering. The new disabled-state
  tints are defined for both `values/` and `values-night/`, but only the light theme was seen.
- External intents actually leaving the app: `mailto:`, `https://t.me/`, and the Facebook share
  URL. The HTTPS gate and the `ActivityNotFoundException` fallback are unit-tested, but no
  handler app was confirmed on the emulator.
- The full render and playback path, which is the Phase 6F.2.8.1 owner gate and is unchanged here.

Two entries from the original list were closed in the follow-up session below: the compact
`layout/activity_main.xml` was exercised at a forced 411 dp, and the native FFmpeg path was
exercised on a build made with `-Precapflow.ffmpeg.enabled=true`.

## Follow-up session — 2026-09-30, verified at `4a0d161`

Three defects reported from the editor, plus one found while fixing them.

### Compact width was reachable after all

The original list said compact `layout/activity_main.xml` needed a sub-600 dp device. The
emulator's screen can be overridden, so the editor was re-verified at both real widths:

```bash
adb -s emulator-5554 shell wm size 1080x2340   # 411 dp wide at the default 240 dpi…
adb -s emulator-5554 shell wm density 420      # …or 411 dp at 420 dpi
adb -s emulator-5554 shell wm size reset
adb -s emulator-5554 shell wm density reset
```

The geometry change recreates the activity and drops the loaded project, so the media has to be
re-imported afterwards.

### 4. Editor tab labels collapsed to "..."

The five Clips / Transform / Audio / Overlay / Export tabs were one `MaterialButtonToggleGroup`
with five children at `layout_width` `0dp` and `weight` 1. At 411 dp each button received about
55 dp and every label ellipsized, so the tabs were unidentifiable. The tablet looked correct only
because it is wide enough, which is why the reports said tablet.

The group is now inside a `HorizontalScrollView` with `fillViewport`, and the buttons use
`wrap_content` plus weight. A `ScrollView` measures its child with an unspecified width, so the
group's natural width is the sum of the labels and the weight distributes only the leftover; on a
phone the labels need more than the screen and the row scrolls, while on a tablet `fillViewport`
stretches the group and the weights spread the buttons across the row as before.

### 5. A failed target duration could never be reconciled again

Reported as `အနီးဆုံးကြာချိန်ကို အခု မသုံးနိုင်တော့ပါ။` after setting a target duration and then speeding the video up.
Speeding up needs more kept source to fill the same output length, so with a 02:04 source and a
01:39 target at 2× the plan requires 198 s of source that does not exist and the planner correctly
returns null. The defect was what happened next.

`reconcileTargetDurationForTimingChange` cleared `adaptiveApplied` on failure while leaving
`targetDurationMs` set, and its own guard began with `if (!adaptiveApplied …) return`. Clearing the
signature to permit a retry was therefore pointless, because the guard returned first. Once a
timing change had failed, no later timing change was reconciled again, so reverting the speed did
not re-apply the target and the Export tab could not apply a suggested duration. The same failure
block was duplicated in `onUserChangedClipTransitions`.

The guard no longer depends on `adaptiveApplied`, the attempt is recorded before running so a
failure cannot spin, the requested target is left intact, and both call sites share
`onTargetDurationUnreachable`. When the reason is specifically `SPEED_NEEDS_MORE_SOURCE` the UI
names the real ceiling and offers to move the target there rather than changing it silently.
`TargetDurationClipPlanner` gains `maximumAchievableDurationMs` and `unreachableReason`, covered by
`TargetDurationReachabilityTest` with the reported numbers: 2× makes a 99 s target unreachable on a
124 s source, the ceiling is 62 s, slow motion raises it to 248 s, and the offered ceiling is
itself reachable.

`applyDurationFitSuggestion` also shared one message across two different situations and told the
user to review clips when the real problem was a declared-but-unapplied target. It now
distinguishes them, and the copy in both locales states the cause and the next action instead of
naming Trim or Adaptive Cuts.

### 6. Transform sub-groups looked like dead controls

Reported as "the zoom labels under the Speed section seem disabled". Two independent causes.

The dimming never applied. Every transform sub-group set its alpha from the same expression it
used for `isVisible`, so when the condition was false the group was already gone and the
`0.46f` branch was unreachable. `controlsEnabled` is `transformEnabled && !renderActive`, so the
only state a user could land in was a visible group with dead buttons rendered at full opacity —
indistinguishable from a live one. Alpha now follows the enabled state. `transformControlsGroup`
was left alone, because its `isVisible` uses `transformDetailsVisible` and its dimming was already
reachable.

The labels were also truncated: `zoomModeGroup` was still three buttons at `0dp` with weight 1, so
"Alternate" collapsed on a compact width. The speed group had already been converted to a scroll
view; the other nine had not. They now use the same shape. Verified on device at both widths:

| Control | 411 dp phone | 853 dp tablet |
|---|---|---|
| Five editor tabs | scrolls, all labels full | all five spread across the row |
| Zoom modes (3) | scrolls, `ချဲ့/ချုံ့ အလှည့်ကျ` full | all three spread across the row |
| Aspect ratio (4) | scrolls | `မူလ` / `9:16` / `16:9` / `1:1` full (was `9…` / `1…`) |

A hypothesis that zoom content was rendered under the Speed heading was raised during this work
and **disproved**: a `uiautomator` dump ordered by vertical position puts the zoom group at
y=335 and the speed section at y=661, so the layout order was always correct. What was actually
observed was truncation plus a dimming that never ran.

### Open items from the follow-up session

- The target-duration offer dialog was not confirmed visually, because the speed value selector
  was not reached on the compact layout. The behaviour is covered by eleven unit tests.
- At 411 dp the video preview overlay covers editor content as it scrolls, which makes reaching
  controls lower in the Transform tab awkward. Worth a layout pass.
- The external intents above were still not exercised.

## Reproducing

```bash
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.recapflow.ai.debug/com.recapflow.ai.MainActivity
```

`-Precapflow.ffmpeg.enabled=true` is required for anything that imports media:
`MediaImportCoordinator` calls `MediaEngine::Probe` → `ffmpeg::ProbeFile` with no fallback, so a
build without it cannot import a video at all. With the flag, the editor header reports
`RecapFlow Native 0.1.0 / FFmpeg 9.0.1`, which is the string C++ returns through JNI.

The debug variant carries `applicationIdSuffix = ".debug"`, so its package is
`com.recapflow.ai.debug` and it can sit alongside a release install of `com.recapflow.ai` on the
same device. The locale command needs the debug package name too:

```bash
adb -s emulator-5554 shell cmd locale set-app-locales com.recapflow.ai.debug --user 0 --locales my
```

The `--user 0` flag is required on this image; without it the command silently reports success and
leaves the locale at `[en]`.

On Windows, quote every `-P` argument before it reaches `gradlew.bat`, and set `ANDROID_HOME` in
each shell because `local.properties` is gitignored:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat :app:assembleDebug "-Precapflow.ffmpeg.enabled=true"
```
