# Phase 6F.2.2 implementation status — 2026-08-27

## Source complete

- Advanced source identity to `RecapFlowAI_Phase6F2_2` / `1.0-phase6f2.2`.
- Replaced the source-video `OpenDocument` launcher with AndroidX `PickVisualMedia`.
- Restricted selection to `PickVisualMedia.VideoOnly`.
- Preserved URI cancellation, best-effort persistable read grants, private working-copy import,
  and local FFmpeg probing.
- Added the documented Google Play services Photo Picker backport declaration for eligible API
  28-29 devices.
- Kept AndroidX's automatic `ACTION_OPEN_DOCUMENT` fallback for devices where Photo Picker is not
  available.
- Added a source verifier and updated the Home import description.

## Preserved regressions

Phase 6F.2.1 dense source-subtitle blur, Phase 6F.2 editor preferences, Phase 6F.1 public export
and bitrate quality, realtime preview, audio, transform, overlay, cancellation, and source/project
lifecycle code are unchanged.

## Verification in this delivery environment

- Phase 6F.2.2 source verifier: PASS, including chained Phase 6E.3B, 6F.1, 6F.1.1.1,
  6F.2, and 6F.2.1 regression markers.
- Kotlin/Android assemble: attempted with
  `./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --offline`; the wrapper could not
  obtain the uncached Gradle 9.0.0 distribution because `services.gradle.org` is unreachable in
  this delivery environment. Compile/install therefore remains an AndroidIDE owner-device gate.
- Device picker behavior: pending owner-device verification.

## Owner device gate

Run the checklist in
[`PHASE6F2_2_VIDEO_GALLERY_PICKER_ANDROIDIDE_VERIFICATION.md`](PHASE6F2_2_VIDEO_GALLERY_PICKER_ANDROIDIDE_VERIFICATION.md).
