# Phase 6D.3 Replace Audio — AndroidIDE / Device Verification

## Build

From the project root:

```bash
bash scripts/verify_phase6d3_source.sh
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

Confirm project identity `RecapFlowAI_Phase6D3` and version `1.0-phase6d3` before
installing the debug APK.

## Picker and lifecycle

1. Open Editor → Audio, enable Audio, and select Replace.
2. Cancel the picker once; the source and existing edit state must remain unchanged.
3. Select an MP3, an M4A/AAC file, and a provider-backed document in separate runs.
4. Confirm filename, duration, and size appear; no broad storage permission is requested
   on modern Android.
5. Rotate/recreate the Activity. Confirm Audio On, Replace, Volume, and the private
   selected asset restore.
6. Choose another video. Confirm the old replacement asset is cleared and render is
   blocked until a new asset is selected.
7. Select an empty/corrupt file if available; confirm preparation fails without changing
   the previous valid asset.

## Realtime preview

Use source speech with obvious lip movement and replacement music with a strong beat.

- Confirm only replacement music is audible; original speech must be silent.
- Play, pause, seek forward/backward, use PlayerView skip controls, and wait through a
  buffer. Audio should resume at the matching edited-output position.
- Change Trim start, preview an Adaptive candidate, and preview the full applied cut
  sequence. Confirm replacement audio follows sequence output time rather than source
  timestamps.
- Test Speed at 0.5×, 1×, and 2×. Replacement music keeps normal pitch/speed while its
  position follows the speed-adjusted video duration.
- Enable 1/2/3-second Intro Freeze and tap its preview. Music begins during the still
  frame and continues into motion without restarting.
- Test Volume at 0, 50, and 100%; changes must be audible before render.
- Pause for ten seconds, resume, and perform repeated seeks. Record any drift beyond
  120 ms or audio restart.

## Export duration policy

Create one replacement track shorter than the edited output and one longer.

- Short: verify seamless looping until the final video frame.
- Long: verify the audio is truncated at the final video frame.
- Probe output: H.264 video + AAC audio must be present for Replace, including Volume
  0%. Mute must still produce no audio track.
- Compare 0/50/100% outputs at the same device volume; loudness must follow preview.
- Repeat 720p then playback-unlocked 1080p with Trim, Adaptive Apply, Speed, Fade, and
  Intro Freeze combinations.
- Confirm final duration equals the edited video plan and replacement audio does not
  extend it.

## Safety and regression

- Start a long render; confirm Audio master, policy, picker, clear, and Volume are
  locked. Cancel and confirm the incomplete MP4 is removed while source/audio assets
  remain usable.
- Background/foreground during preview and render; confirm no duplicate players,
  overlapping audio, or duplicate render.
- Switch Home/Editor/Settings. Playback leaving Editor must pause both video and
  replacement audio.
- Recheck Keep Original and Mute at 720p/1080p.

Record device/API, source and audio format/duration, selected gain, edited duration,
output codecs/duration/size, loop or trim result, observed sync, and encoder diagnostics.
