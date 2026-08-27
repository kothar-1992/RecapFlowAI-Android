# Phase 6D.2 AndroidIDE / Device Verification

## Build

From the project root:

```bash
bash scripts/verify_phase6d2_source.sh
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

Confirm project identity `RecapFlowAI_Phase6D2` and version `1.0-phase6d2` before
installing the debug APK.

## Device matrix

Use a source with speech/music and an obvious lip-sync or impact cue.

- Audio Off: Volume controls are hidden and source preview/export remain unchanged.
- Audio On + Keep Original: test 0, 25, 50, 75, and 100%; each slider change must be
  audible before render in ordinary source preview.
- Candidate and full cut-sequence previews must use the same selected level.
- Probe 0% output: AAC audio track remains present even though samples are silent.
- Probe Mute output: audio track is absent.
- Compare 25/50/75/100% exports at the same device volume; loudness should increase
  monotonically with no duration change.
- Repeat at 720p and 1080p with Trim, Adaptive Apply On, Speed 0.5× and 2×, and Intro
  Freeze. Check lip-sync/impact alignment at the beginning, middle, and end.
- After a completed render, change Volume and confirm stale output is invalidated.
- Start a render and confirm Audio master, policy, Volume, and Reset are locked.
- Rotate/recreate with Audio selected; master, policy, and Volume must restore.
- Cancel an intermediate render; partial output is removed and source remains intact.

## Evidence

Record source/output duration, selected percentage, video/audio codecs, audio-track
presence, output size, playback result, A/V sync observation, and render elapsed time.
Do not treat silence alone as proof of Mute; probe the output track layout.
