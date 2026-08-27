# Phase 6D.2 — Audio Volume

## Outcome

The Audio tab adds a remembered source-volume control whose result is audible before
render and is compiled into every exported source range. The verified range is 0–100%
linear gain.

## Contract

- Audio Off ignores the remembered value and is a true no-op.
- Audio On + Keep Original + 100% is a true compiler no-op.
- Audio On + Keep Original below 100% uses one typed compiled gain for preview and
  export.
- Preview uses RecapFlowAI Player volume only; device/system volume is never changed.
- Export applies a constant-gain 16-bit PCM processor after Speed processing and before
  AAC encoding.
- Gain is bounded at unity, so this gate cannot introduce boost clipping.
- 0% keeps a silent AAC track; Mute continues to remove the audio track entirely.
- Changing volume invalidates stale output, and controls are locked during render.
- Replace, Mix, asset picking, waveform editing, audio fades, normalization, boost,
  Overlay, and Gemini remain outside this gate.

## Acceptance

1. 0/25/50/75/100% changes are audible immediately in source/candidate/sequence preview.
2. Export loudness follows the same setting while duration and A/V sync remain stable.
3. 100% matches the Phase 6D.1 Keep Original output path without an added processor.
4. 0% output probes with an AAC track; Mute output probes without an audio track.
5. Volume survives recreation, works with Trim/Adaptive/Speed/Freeze and 720p/1080p,
   invalidates a completed output, and cannot mutate an active render.

Stop after this gate. Phase 6D.3 Replace Audio requires a separate picker, URI
permission, duration policy, preview, and export verification slice.
