# Phase 6F.2.4 — Preview capability and fallback separation

## Problem

On the reported 540×960 source, enabling Transform while Clips/Overlay state is active can make
Media3 reject or stall the realtime effect graph. The former workflow then reused the same player
for fallback and automatically re-enabled live effects after every edit. That produced repeated
`This device could not preview...` notices and could end at a misleading FFmpeg-metadata message.

FFmpeg is not the realtime preview renderer in this Android source. FFmpeg/JNI supplies metadata;
ExoPlayer plus Media3 Effects owns preview; Media3 Transformer owns final export. A preview failure
therefore must not weaken or clear the typed final export plan.

## Implemented workflow

1. `LiveEffects` plays the source through the bounded Media3 effect graph.
2. A synchronous graph error, player error, or 10-second readiness timeout claims one recovery for
   the current source generation.
3. Recovery recreates ExoPlayer and prepares the original source with no effects.
4. `SourceOnly` remains stable while the user changes Trim, Transform, Audio, or Overlay settings.
   Those settings still update the `EditPlan`; no automatic GPU retry occurs.
5. `Retry live effects` starts a new generation, recreates the player, and tries the current graph
   once. Another failure returns to source-only playback.
6. If even the new source-only player fails, `Unavailable` explains that the edit/export settings
   remain saved and retains the explicit retry action.

## Preview budget

Interactive preview and final export no longer share a resolution target. Preview uses the
source short side up to a 720-pixel ceiling, rounded to an even value. A 540×960 source therefore
previews at 540×960 instead of being upscaled to 720×1280. Final 720p, 1080p, and 2K exports still
use their exact selected short side and existing bitrate/validation policy.

## Diagnostics

`RecapFlowPreview` records Media3 error name and number, current graph topology, source dimensions,
codec, recovery reason, and explicit retry generation. A graph is marked applied only after the
player reports its first rendered frame.

## Non-goals

- No preview proxy generation or FFmpeg frame renderer.
- No change to final render effects, resolution, bitrate, validation, or MediaStore publication.
- No automatic per-device effect blacklist.
- No background render service.
- No claim of target-device success until the attached matrix is run.

## Acceptance

- At most one automatic source-only recovery occurs per source generation.
- Editing during `SourceOnly` never automatically retries the rejected effect graph.
- Source-only playback remains seekable/playable and effect settings remain selected.
- Retry is explicit and produces a new logged generation.
- A 540-pixel source is not upscaled for interactive preview.
- 720p/1080p/2K final output continues through the Phase 6F.2.3 validation path.
