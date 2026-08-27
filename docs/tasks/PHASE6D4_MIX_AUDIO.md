# Phase 6D.4 — Mix Audio

## Outcome

The Audio tab can combine the source soundtrack with one user-selected local audio
asset. Realtime preview and Media3 export use the same edited-output timeline and two
independent constant gains.

## Contract

- Mix is reachable only through Audio On → Mix.
- Mix requires a source audio stream. A silent source is blocked with guidance to use
  Replace instead.
- The verified `audio/*` picker and private cached asset are shared by Replace and Mix;
  no media is uploaded.
- `Original volume` and `Added audio volume` are independently adjustable from 0–100%.
  Reset restores the conservative 70% + 30% balance.
- The source player uses Original volume while the synchronized external player uses
  Added audio volume before render.
- Export retains source audio, adds one looping audio-only sequence, and applies the
  same gains before AAC encoding.
- Both Mix sequences output signed 16-bit stereo PCM. Mono is duplicated, stereo is
  preserved, and uncommon multi-channel audio is conservatively folded to centered
  stereo so Media3 can mix a consistent channel layout.
- Short added audio loops; long audio is truncated when the non-looping edited video
  ends. The added track stays at normal speed while its position follows edited output
  time across Speed, Adaptive Cuts, and Intro Freeze.
- Audio Off, Keep Original, Mute, Replace, 720p/1080p, cancellation, and lifecycle
  behavior remain unchanged.

## Acceptance

1. Selecting Mix without an external asset blocks render with a clear message.
2. Selecting Mix on a source without audio blocks render and recommends Replace.
3. Both tracks are audible before render and respond independently at 0/30/50/70/100%.
4. Seek, pause/resume, Trim, Adaptive candidates/sequence, Speed, and Intro Freeze keep
   the added track aligned to edited-output time within the existing 120 ms tolerance.
5. Mono/stereo source and external-audio combinations export one readable AAC track.
6. Short-loop and long-trim behavior ends exactly with the edited video.
7. 720p and playback-unlocked 1080p output match preview balance and keep A/V sync.

Stop after this gate. Automatic ducking, fades, limiter/loudness normalization, boost,
waveform editing, multiple added tracks, Overlay, and Gemini are separate future work.
