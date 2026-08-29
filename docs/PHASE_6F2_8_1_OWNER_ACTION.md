# Owner action

1. Sync `hotfix/phase-6f2.8.1-render-quality`.
2. Run `bash scripts/verify_phase6f2_8_1_source.sh`.
3. Run unit tests and `assembleDebug` with the verified Termux Gradle/AAPT2 environment.
4. Install the APK and test 1080p then 720p CBR output.
5. Report requested/actual bitrate, file size, duration drift and visual sharpness.
