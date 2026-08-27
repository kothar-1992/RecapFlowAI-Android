# Phase 6D.4 Mix Audio — AndroidIDE / Device Verification

## Build

From the project root:

```bash
bash scripts/verify_phase6d4_source.sh
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

Confirm project identity `RecapFlowAI_Phase6D4` and version `1.0-phase6d4` before
installing the debug APK.

## Policy and picker

1. Open Editor → Audio, enable Audio, and confirm Keep Original, Mute, Replace, and Mix.
2. Select Mix without an audio asset; render must remain blocked.
3. Cancel the picker once; the source and existing edits must remain unchanged.
4. Select MP3, M4A/AAC, mono, and stereo files in separate runs.
5. Confirm name, duration, and size; rotate/recreate and confirm Mix, both gains, and the
   private selected asset restore.
6. Switch Replace → Mix → Replace. The same valid selected asset should remain selected,
   but each policy must use its own remembered volume behavior.
7. Choose another source video. The old external asset must be cleared.
8. Test a source with no audio. Mix must be blocked with guidance to use Replace.

## Realtime two-track preview

Use source speech with obvious lip movement and added music with a strong beat.

- At Reset Balance, confirm original speech is audible at 70% and added audio at 30%.
- Change Original and Added controls independently to 0/30/50/70/100%.
- Confirm 0% on one track silences only that track; it must not switch policy to Mute.
- Play, pause, seek, skip forward/backward, and wait through buffering. The added track
  should return to the corresponding edited-output position.
- Change Trim start, preview an Adaptive candidate, and preview the full cut sequence.
- Test Speed at 0.5×, 1×, and 2×. Added audio remains normal speed while following the
  speed-adjusted video duration.
- Test 1/2/3-second Intro Freeze. Added audio begins during the still frame; original
  source audio begins when motion starts.
- Leave/re-enter Editor and background/foreground the app. No overlapping or orphaned
  audio player should remain.

## Export and format matrix

- Test mono source + stereo added audio, stereo source + mono added audio, and
  stereo + stereo. Output must contain one AAC track without a channel-format failure.
- Short added track: verify repeat until the final frame.
- Long added track: verify truncation at the final frame.
- Compare preview and rendered balance at the same device volume.
- Probe outputs for H.264 video + one AAC audio track and expected edited duration.
- Repeat 720p then playback-unlocked 1080p with Trim, Adaptive Apply, Speed, Fade, and
  Intro Freeze combinations.
- Listen for clipping at loud settings. The gate permits 0–100% per track but does not
  claim automatic loudness normalization or dynamic limiting.

## Safety and regression

- During render confirm Audio master, policy, picker, clear, and both sliders are locked.
- Cancel at an intermediate percentage; incomplete MP4 is removed while source and
  external assets remain reusable.
- Recheck Audio Off, Keep Original, Mute, and Replace at 720p/1080p.
- Repeat render twice and check for duplicate playback, progressive memory growth, or
  stale output after changing either Mix volume.

Record device/API, source and added-audio codec/channel count/duration, selected gains,
edited duration, output codecs/duration/size, observed sync, and encoder diagnostics.
