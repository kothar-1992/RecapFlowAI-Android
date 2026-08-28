# AndroidIDE Quick Build/Test — Phase 6F.2.7

Run these from the extracted project root.

```bash
bash scripts/verify_phase6f2_7_source.sh

./gradlew :app:testDebugUnitTest --no-daemon --max-workers=2

./gradlew :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true \
  -Precapflow.composition.preview.enabled=true \
  --no-daemon \
  --max-workers=2
```

If AndroidIDE reports a Gradle/Kotlin/Media3 compile error, send the first error block before any
follow-on errors. If the APK builds, follow
`docs/PHASE6F2_7_COMPOSITIONPLAYER_ANDROIDIDE_VERIFICATION.md` and do not merge PR #10 until the
device matrix passes.

## Recorded result

Owner reported **Phase 6F.2.7 PASS — 2026-08-28**.

The exact AndroidIDE-tested source was synchronized to
`feature/phase-6f2.7-compositionplayer-preview` as commit
`5042d7012ad309aea511d66661efcc4dd10b5522`.

Repository gate: PR #10 final diff review/merge. After merge, Phase 6F.2.8 becomes the active quality gate.
