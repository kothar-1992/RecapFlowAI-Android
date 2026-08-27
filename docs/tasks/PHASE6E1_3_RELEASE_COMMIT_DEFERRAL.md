# Phase 6E.1.3 — Release Commit Deferral

## Device evidence

The Phase 6E.1.2 marked guide can be moved directly by touch without a crash. The
application still terminates when the finger is released. Slider-based geometry edits
remain stable. This isolates the remaining regression to work executed from the
`ACTION_UP`/`ACTION_CANCEL` callback, not continuous pointer movement.

## Outcome

Touch release schedules the final rectangle but does not mutate Android view layout,
Material sliders, typed overlay state, or Media3 effects from inside input dispatch.
The commit runs on the next animation frame and the GPU update runs later through the
bounded preview queue.

## Contract

- `ACTION_MOVE` remains compositor-only.
- `ACTION_UP`/`ACTION_CANCEL` releases interception and calls only the deferred scheduler.
- No `performClick()` runs on the non-clickable blur guide or resize handle.
- The guide stays translated/scaled at its release location until the scheduled frame.
- `postOnAnimation` restores identity transforms, validates source/editor state, and commits
  one normalized rectangle.
- Layout/slider synchronization happens only inside that deferred commit.
- Media3 receives the update through the existing 140 ms coalescing queue.
- A new gesture cancels an older uncommitted release.
- Player errors, source changes, `onStop`, and `onDestroy` cancel pending release work.
- Logs contain only bounded state labels and reasons—never raw coordinates or media paths.

## Non-goals

No shader change, second region, keyframe/tracking, image/video overlay, Gemini,
Export/Download, Telegram, or background-render change is included.
