# Phase 6F.2.6 Task — Shared Media3 Composition Workflow

Date: 2026-08-27

## Problem

The editor stores one immutable `EditPlan`, but final export previously had two execution shapes:
a single `EditedMediaItem` fast path and a `Composition` path for multiple clips, freeze, or external
audio. That split makes a later CompositionPlayer preview susceptible to interpreting the same edit
plan differently from export.

## Outcome

Compile every validated edit plan into one Media3-independent topology and then one explicit Media3
`Composition`. Transformer always consumes that composition. The topology owns reviewed ranges,
freeze placement, source/replacement audio policy, item/sequence count, and planned duration.

## Work items

- [x] Introduce `Media3CompositionPlanCompiler` with no Android or Media3 runtime dependency.
- [x] Introduce `Media3CompositionCompiler` as the only builder of Media3 export sequences/items.
- [x] Give every composition item an explicit duration for future CompositionPlayer compatibility.
- [x] Preserve the existing Transform, Speed, Overlay, source blur, audio gain/mix, and freeze effect
  factories.
- [x] Route all Transformer exports through the compiled `Composition`.
- [x] Log the compiled graph topology when render starts.
- [x] Add pure unit tests and a retained source verifier.
- [ ] Run the AndroidIDE/owner-device matrix.

## Acceptance criteria

1. A plain one-range render uses a one-item, one-sequence Composition—never the old direct start.
2. Adaptive Cuts preserve reviewed order; Intro Freeze is the first video item.
3. Replace/Mix adds exactly one looping audio sequence; Mute produces video-only output.
4. Freeze with retained source audio forces a continuous source-audio track.
5. Compiled planned duration remains `EditPlan.plannedDurationMs`.
6. Existing 720p/1080p/2K H.264/AAC validation and MediaStore finalization remain unchanged.

## Non-goals

- CompositionPlayer activation or removal of the ExoPlayer fallback
- Timeline thumbnails/range handles
- 4K, H.265, background rendering, or cloud processing
- FFmpeg dependency/build changes
- New editing effects or automatic timeline mutation

## Next task

Phase 6F.2.7: feed this shared topology into CompositionPlayer behind a feature/capability gate,
retain source-only ExoPlayer recovery, and prove preview/export effect parity on the owner device.
