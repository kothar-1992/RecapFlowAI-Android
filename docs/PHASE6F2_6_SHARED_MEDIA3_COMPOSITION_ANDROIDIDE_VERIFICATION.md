# Phase 6F.2.6 Shared Media3 Composition — AndroidIDE Verification

## 1. Source and build

```bash
bash scripts/verify_phase6f2_6_source.sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true
```

Confirm the installed app reports `1.0-phase6f2.6`, `RecapFlow Native 0.1.0`, and FFmpeg 9.0.1.

## 2. Render matrix

Use an original H.264/AAC source with visible motion and speech. For each case, inspect Logcat for
`composition=ranges=...` and verify the completed Gallery file:

| Case | Expected topology/result |
|---|---|
| Plain Trim | 1 range, 1 video item, 1 sequence, A/V synchronized |
| Adaptive 3 ranges | 3 ranges/items in reviewed order, no skipped/duplicated boundary |
| Intro Freeze + Trim | Freeze first, continuous original audio, exact planned duration within policy |
| Mute | No output audio track |
| Replace | 2 sequences, replacement AAC only |
| Mix | 2 sequences, original + replacement audible without clipping |
| 720p / 1080p / 2K | Exact selected dimensions, even width/height, H.264 and requested audio policy |

## 3. Safety and fallback

- Cancel during freeze preparation and during Transformer render; no partial Gallery entry remains.
- Force an invalid/missing replacement-audio path; render must fail before Transformer starts.
- Trigger the established live-preview fallback; editing stays usable and the final Composition
  still contains the saved effects.
- Recreate the activity, render again, and confirm the same topology and output duration.

## 4. Evidence to retain

- AndroidIDE build/test output
- One Logcat topology line per matrix case
- `ffprobe`/MediaInfo metadata for 720p, 1080p, and 2K outputs
- Screenshots for Completed, cancellation, and one expected failure

CompositionPlayer preview is intentionally not part of this gate.
