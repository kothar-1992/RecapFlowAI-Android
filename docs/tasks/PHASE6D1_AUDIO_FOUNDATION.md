# Phase 6D.1 — Audio Foundation

## Outcome

The Review Editor exposes an Audio tab with explicit Off, Keep Original, and Mute
behavior. The user hears the selected policy before rendering, and the same typed
policy controls every rendered clip.

## Implementation contract

- Audio Off is a true no-op and keeps source audio.
- Audio On + Keep Original is also a compiler no-op.
- Audio On + Mute sets only the RecapFlow preview player's volume to zero.
- Mute compiles to `EditedMediaItem.Builder.setRemoveAudio(true)` on every moving
  source item.
- A muted Intro Freeze composition does not force an otherwise empty audio track.
- Replace, Mix, Volume, asset picking, waveform editing, and Gemini remain hidden.
- The Activity never constructs FFmpeg strings or changes system/device volume.

## Acceptance

1. Audio Off and Keep Original sound identical and export AAC source audio.
2. Mute is audible immediately in source/candidate/sequence preview.
3. A muted export has video and no audio track.
4. Mute survives Trim, Adaptive Cuts, Speed, Freeze, 720p/1080p, recreation, and
   cancellation without regressing the video plan.
5. Changing Audio invalidates stale completed output but cannot mutate an active job.

Stop after this gate. Phase 6D.2 Volume requires independent preview/export parity
and A/V sync verification.
