# Phase 6A Task — Typed EditPlan and User-Controlled Trim

- GitHub tracking issue: https://github.com/ZeusOwner/recapflow-ai/issues/20

## Goal

Let an Android user choose the exact source range to keep and render that range
locally from a validated, typed `EditPlan`.

## Current state

Phase 5 local 720p/1080p H.264 + AAC rendering is owner-confirmed complete. The
Phase 6A source adds a Clips/Trim Review Editor surface and compiles the selected
range with Media3 clipping while preserving the existing render sequence.

## Scope

- [x] Add typed edit/profile/transform/audio/overlay/subtitle settings.
- [x] Default every future optional transformative operation to Off.
- [x] Validate source and trim bounds with a one-second minimum.
- [x] Add a two-handle Trim selector and Reset action on phone/tablet layouts.
- [x] Save the trim range through Activity recreation.
- [x] Render the selected range at 720p and then 1080p after playback unlock.
- [x] Calculate realtime factor against selected clip duration.
- [ ] Build and install on the target Android device.
- [ ] Record trimmed-output duration and A/V sync evidence.

## Non-goals

- Zoom, mirror, crop, color, freeze, speed, transitions, or adaptive cuts.
- Overlay execution or an Overlay tab before its renderer is implemented.
- Gemini API integration or AI-generated edit decisions.
- Background render jobs and project persistence.

## Technical constraints

- Native Kotlin/XML/ViewBinding UI; compact and `sw600dp` IDs must match.
- UI code must never construct arbitrary FFmpeg command strings.
- Optional operation Off means the compiler omits that operation.
- Core media processing remains local; no VPS dependency.
- The source file must never be overwritten.

## Acceptance criteria

- [ ] Reset selects `0 ms..sourceDurationMs`.
- [ ] A selection shorter than one second cannot start rendering.
- [ ] 720p output duration differs from the plan by no more than
  `max(100 ms, 3 frames)`.
- [ ] Output video and audio start/end at the selected range.
- [ ] A/V sync error is at most 100 ms.
- [ ] Playing the 720p output unlocks the same-plan 1080p render.
- [ ] Changing Trim after a completed render invalidates the old unlock and
  requires a new 720p render.
- [ ] Cancel removes the incomplete output and preserves the source.

## Verification plan

- Build: `./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true`
- Unit: `./gradlew :app:testDebugUnitTest`
- Package: confirm ARM64 `libflowai.so` and FFmpeg-enabled runtime.
- Runtime: import → trim → 720p render → playback → 1080p on the Mi Pad.
- Media: probe output duration, streams, and A/V timestamps with FFmpeg.

## Dependencies and risks

- Requires the Phase 5 ARM64 FFmpeg prebuilts and Android-hosted toolchain.
- Media3 clipping may align to decoder timestamps; use the defined tolerance.
- Very short sources under one second are intentionally rejected in Phase 6A.
- Activity-bound rendering remains a Phase 7 lifecycle limitation.
