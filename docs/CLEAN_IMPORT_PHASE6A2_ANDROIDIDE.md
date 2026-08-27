# Clean Import: RecapFlowAI Phase 6A.2

The Phase 6A, Phase 6A.1, and Phase 6A.2 archives previously shared the same
top-level folder name. Extracting over an existing AndroidIDE project can leave
the IDE pointed at the older Phase 6A root even when a newer archive was
downloaded.

## Use the distinct project root

1. Close the currently open RecapFlowAI project in AndroidIDE.
2. Extract the corrected archive into a new location. Do not merge it into an
   existing `RecapFlowAI_Phase6` directory.
3. Open the folder named `RecapFlowAI_Phase6A2` that directly contains
   `settings.gradle.kts` and `app/`.
4. From that project root, run:

```bash
bash scripts/verify_phase6a2_source.sh
```

Expected:

```text
PASS: RecapFlowAI Phase 6A.2 source identity and UI markers are present.
```

## Clean build

```bash
./gradlew --stop
./gradlew clean :app:testDebugUnitTest :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true
```

Install the newly generated `app-debug.apk` as an update. A correct launch must
show:

- bottom navigation: `Home`, `Editor`, `Settings`;
- Home toolbar subtitle ending in `Phase 6A.2`;
- Editor preview centered at the top;
- a scroll sheet that moves beneath the preview;
- compact metadata with an expandable `Video details` action.

If the bottom navigation or Phase 6A.2 marker is absent, stop testing—the APK
was not built from this project root.
