# Phase 6D.3 — Replace Audio

## Outcome

The Audio tab can replace the selected video's entire source soundtrack with one
user-selected local audio file. The same selected asset, output-time mapping, loop/trim
policy, and 0–100% gain are used before render and in Media3 Transformer export.

## Contract

- Replace is reachable only through Audio On → Replace.
- The system document picker accepts `audio/*`; RecapFlowAI copies the selection into
  its private cache and never uploads it.
- Missing, empty, expired, or unreadable replacement assets block render with a
  user-facing validation message.
- Replace immediately silences source audio. A dedicated local preview player follows
  Trim seeks, play/pause, Speed-adjusted output time, Adaptive candidates and sequence,
  and the Intro Freeze interval.
- Audio shorter than the edited output loops. Audio longer than the edited output is
  truncated when the video sequence ends.
- Export removes audio from every source-video item and adds one looping audio-only
  Media3 sequence. The existing Volume gain is applied to that sequence before AAC
  encoding.
- Changing or clearing the asset invalidates stale output. Picking and clearing are
  disabled while render is active.
- Replacing the project video clears the old replacement asset so it cannot be applied
  accidentally to a different project.
- Keep Original, Mute, and Volume retain their Phase 6D.2 behavior. Mix remains blocked.

## Acceptance

1. MP3, M4A, and AAC picker inputs prepare locally and show name, duration, and size.
2. Replacement audio is audible before render and source audio is not audible.
3. Seek/play/pause drift corrects to within the 120 ms synchronization tolerance.
4. A short track loops without a gap; a long track ends exactly with the edited video.
5. 0/50/100% preview loudness matches export while 0% retains an AAC track.
6. Trim, Adaptive Apply, Speed, Intro Freeze, 720p/1080p, cancellation, recreation, and
   source replacement pass the target-device matrix.

Stop after this gate. Phase 6D.4 Mix needs an independent two-track gain, clipping,
ducking, and preview/export verification slice.
