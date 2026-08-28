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
