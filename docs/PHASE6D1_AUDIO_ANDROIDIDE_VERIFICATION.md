# Phase 6D.1 AndroidIDE / Device Verification

## Build

From the project root:

```bash
bash scripts/verify_phase6d1_source.sh
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

Confirm project identity `RecapFlowAI_Phase6D1` and version `1.0-phase6d1` before
installing the debug APK.

## Device matrix

Use one source with a clearly audible audio track.

- Audio Off: preview remains audible; 720p output contains synchronized audio.
- Audio On + Keep Original: preview remains audible; output contains synchronized audio.
- Audio On + Mute: source preview becomes silent immediately; switching back restores
  sound; output contains no audio track.
- Candidate preview and `Preview full cut sequence`: Mute follows every playlist item.
- Adaptive Cuts Apply On: every exported range is mute; order/duration are unchanged.
- Speed: muted output duration matches the same video-speed plan; no audio processor
  conflict occurs.
- Intro Freeze: muted output has no forced silent audio track; Keep Original retains
  the normal silent intro followed by source audio.
- 720p playback unlock and 1080p render both preserve the selected policy.
- Changing Audio after completion invalidates the stale output; changing it during a
  render is blocked.
- Rotate/recreate with Audio tab selected; On/Off and policy restore.
- Cancel at an intermediate percentage; partial MP4 is removed and source is preserved.

## Evidence

Record source/output duration, video codec, audio stream presence, audio codec when
present, playback A/V sync, output size, and whether Android opens the MP4. A silent
volume setting alone is not proof of a muted export: probe the output and confirm that
the audio stream is absent.
