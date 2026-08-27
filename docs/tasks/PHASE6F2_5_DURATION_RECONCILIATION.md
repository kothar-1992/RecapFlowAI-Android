# Phase 6F.2.5 — Final-duration reconciliation

## Problem

The production validator inherited one fixed 250 ms duration limit. On the owner sample, the
reviewed plan was 293154 ms and Media3 reported 293430 ms: a 276 ms difference. Dimensions,
H.264, AAC, and the selected 1080p profile could all be correct, yet the private render was deleted
because the timing result exceeded the fixed limit by only 26 ms.

## GitHub workflow comparison

The GitHub RecapFlowAI workflow keeps `target_duration_sec`, clip durations, transition overlap,
and `expected_final_duration_sec` in a resolved timing graph. Updating target duration rebuilds
the segment/clip plan. Before final validation, FFmpeg clamps an overlong result or pads a short
result to the expected duration, then probes the result again.

The native Android app has no ATS/voice timeline and deliberately avoids a second encode. Its
equivalent safe workflow is:

1. Compile the current user-reviewed `EditPlan`.
2. Calculate the nearest feasible whole second.
3. Show the exact delta before render.
4. Change only the last selected Trim/Adaptive range after explicit user confirmation.
5. Validate Media3 output with a bounded duration-aware tolerance while keeping quality, codec,
   and audio-policy validation exact.

## Implementation contract

- `DurationFitAdvisor` is pure Kotlin and never mutates editor state.
- Whole-second candidates are ordered by distance from the compiled plan.
- Speed and Intro Freeze are included in the calculation.
- Applying a recommendation changes only Trim end or the last reviewed Adaptive range end.
- A one-second minimum, source bounds, range ordering, and exact compiled target are required.
- The historical 250 ms drift remains the floor.
- Long outputs may use up to 0.1% drift, capped at 750 ms.
- A drift above 250 ms but inside the calculated limit is surfaced as a validation warning.
- Resolution, even dimensions, H.264, AAC/mute policy, and selected quality remain hard failures.

## Non-goals

- Silent automatic clip changes
- A second trim/pad transcode
- ATS, voiceover synchronization, or transition overlap redesign
- 4K, H.265, background rendering, or VPS processing

