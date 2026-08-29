# Termux build — Phase 6F.2.8.1

The owner already proved the RecapFlowAI project can build in the official Termux environment using the migrated Android SDK/NDK and local Gradle 9.0.0 setup. This hotfix branch must be rebuilt after syncing the CBR source changes.

## Sync

```bash
cd /storage/emulated/0/AndroidIDEProjects/RecapFlowAI-Android
git status --short
git fetch origin
git switch hotfix/phase-6f2.8.1-render-quality
git pull --ff-only origin hotfix/phase-6f2.8.1-render-quality
```

Do not discard local changes automatically. If `git status --short` is non-empty and switching is blocked, preserve or stash those changes before continuing.

## Source verifier

```bash
bash scripts/verify_phase6f2_8_1_source.sh
```

## Unit tests

```bash
AAPT2_BIN="$PREFIX/bin/aapt2"
"$HOME/.local/opt/gradle-9.0.0/bin/gradle" :app:testDebugUnitTest \
  -Precapflow.ffmpeg.enabled=true \
  -Pandroid.aapt2FromMavenOverride="$AAPT2_BIN" \
  --no-daemon \
  --max-workers=2 \
  --stacktrace
```

## Debug APK

```bash
AAPT2_BIN="$PREFIX/bin/aapt2"
"$HOME/.local/opt/gradle-9.0.0/bin/gradle" :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true \
  -Pandroid.aapt2FromMavenOverride="$AAPT2_BIN" \
  --no-daemon \
  --max-workers=2 \
  --stacktrace
```

Expected APK path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

A successful build is not sufficient to close the hotfix. Owner-device 1080p and 720p CBR visual-quality evidence is still required.
