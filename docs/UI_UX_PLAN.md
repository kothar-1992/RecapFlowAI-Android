# RecapFlowAI Android — UI/UX implementation plan

- **Planning date:** 2026-08-19
- **UI technology:** Native Kotlin + XML + ViewBinding + Material 3
- **Primary device class:** Android tablets, beginning with Mi Pad
- **Secondary device class:** ARM64 Android phones
- **Current UX gate:** Render and open one trustworthy 720p local MP4

## 1. Product experience principles

1. **Local-first confidence** — clearly label local media operations as
   on-device and never imply that source video is uploaded to a server.
2. **One primary decision per screen** — avoid exposing the future ATS, AI,
   subtitle, and timeline controls before their prerequisite gates work.
3. **Progressive disclosure** — show essential video facts first; place detailed
   codecs, bitrate, and audio information behind an expandable section.
4. **State is always visible** — empty, selecting, copying, probing, ready,
   rendering, completed, failed, and cancelled states must have distinct UI.
5. **Recoverable errors** — every failure explains what happened and offers a
   specific next action such as Retry, Select another video, or Free storage.
6. **Tablet-first, phone-safe** — use available width productively without
   forcing phone layouts to stretch across a tablet.
7. **Accessible interaction** — minimum 48 dp touch targets, scalable text,
   sufficient contrast, content descriptions, and no color-only status meaning.
8. **No main-thread media work** — selection follow-up, copying, probe, and
   rendering must expose progress without freezing navigation.

## 2. Visual direction

Use a dark-first Material 3 editing workspace that matches the existing native
diagnostics screen while remaining fully usable in light mode.

- Brand accent: restrained violet/purple for primary actions and active state.
- Surfaces: near-black background with layered neutral cards in dark mode.
- Status colors: green for ready/completed, amber for warnings, red for errors;
  always pair the color with an icon and text label.
- Typography: clear hierarchy with compact metadata labels and readable values.
- Shape: medium rounded cards and buttons; avoid decorative shapes that reduce
  usable preview space.
- Motion: short state transitions and determinate progress only; no continuous
  decorative animation during CPU-intensive media work.

Final color, typography, spacing, and icon tokens must live in Android resource
files rather than being hard-coded in Activities or layouts.

## 3. Responsive layout strategy

### Compact width — phones

- Single-column screens.
- Video preview above controls.
- One sticky primary action near the bottom when appropriate.
- Long metadata groups collapse by default.

### Medium/expanded width — tablets

- Two-pane project workspace after a video is selected.
- Preview occupies the larger left/top region.
- Metadata and actions occupy a bounded right-side panel.
- Do not stretch text or buttons across the entire display.
- Preserve preview aspect ratio through orientation and multi-window changes.

Use resource-based width variants such as `layout-sw600dp` for the initial
tablet split. Introduce more granular window-size handling only when the
navigation shell requires it.

## 4. Information architecture

### Current milestone navigation

```text
Home
  └── Import video
        └── Video review / probe
              └── Render test (Phase 5)
                    └── Result / export
```

This milestone should not display empty placeholders for Script, Voice, ATS,
Timeline, or AI services. Those destinations are introduced only when their
functional gates begin.

### Future project workspace

```text
Project overview
  ├── Source
  ├── Script
  ├── Voice
  ├── Timeline
  ├── Preview
  └── Export
```

On tablets this becomes a navigation rail plus workspace. On compact phones it
becomes sequential destinations with a bottom bar only after three or more
destinations are actually usable.

## 5. Phase 4 screen specifications

### A. Home / empty state

Purpose: start the first local project without presenting unsupported features.

Required content:

- RecapFlowAI app identity.
- Primary `Import video` action.
- Short trust message: processing for this gate happens on the device.
- Native/FFmpeg readiness indicator in a diagnostics overflow or secondary
  details area, not as the main product screen.
- Recent-project area remains omitted until persistence exists.

States:

- Engine ready: import enabled.
- Engine unavailable: import disabled with diagnostic action.

### B. Android document picker

- Use the system document picker with `video/*` and persist URI permission only
  when the chosen contract and provider allow it.
- Keep Android 10+ on the permission-free document picker. On Android 6–9,
  request legacy read access only at import time with a clear system-picker
  fallback when denied.
- Handle cancellation without showing an error.
- Reject an inaccessible or unsupported selection with a specific recovery
  action.

### C. Preparing and probing

- Show the selected filename immediately.
- Use separate messages for `Preparing video` and `Analyzing video`.
- Display determinate copy progress when content length is known; otherwise use
  a restrained indeterminate indicator.
- Prevent duplicate import actions while a probe is active.
- Allow Back only after warning about cancelling current preparation when work
  is still active.

### D. Video review / metadata

Primary content:

- Android-native preview surface or thumbnail placeholder.
- Filename and formatted duration.
- Resolution, orientation, frame rate, and container.
- Video codec and whether audio is present.
- Expandable technical details for bitrate, audio codec, sample rate, and
  channels.
- `Choose another video` secondary action.
- `Continue to 720p test` primary action, disabled until the probe succeeds.

The UI receives a typed `MediaInfo` model. It must never parse FFmpeg console
text or construct FFmpeg command strings.

### E. Error state

Every probe error contains:

- Human-readable title.
- Short reason without raw stack traces.
- Stable internal error code in expandable diagnostics.
- One primary recovery action.
- Optional `Copy diagnostics` action with secrets and private paths redacted.

## 6. Phase 5 render UX

Add only after probing is reliable.

- Preset selector begins with `720p test`; `1080p` unlocks after the lightweight
  render gate passes.
- Render screen shows stage, percent, elapsed time, and output destination.
- Cancellation requires confirmation and removes partial output safely.
- Background/foreground behavior must match the render-job architecture rather
  than the Activity lifecycle.
- Completion screen offers Play, Save/Export, and Render again.
- Never claim success until Android can open the finalized output.

## 7. UI state model

Use one observable screen state rather than scattered boolean flags:

```text
ImportUiState
├── EngineChecking
├── Empty
├── Picking
├── Preparing(progress?)
├── Probing
├── Ready(mediaInfo)
└── Error(code, message, recoverable)
```

Render state remains separate:

```text
RenderUiState
├── Idle
├── Preparing
├── Rendering(progress, stage)
├── Finalizing
├── Completed(output)
├── Failed(error)
└── Cancelled
```

Do not put file copying, FFmpeg calls, or render ownership directly in a View or
Activity. The UI observes typed state from a controller/ViewModel boundary.

## 8. Implementation slices and gates

### UI Slice 1 — foundation

- [x] Define theme colors, typography, spacing, and reusable status styles.
- [x] Replace the centered diagnostics-only layout with Home/empty state.
- [x] Keep native/FFmpeg diagnostics reachable as secondary information.
- [x] Add compact and `sw600dp` layout resources.
- [ ] Verify portrait, landscape, dark, light, and large-font rendering.

### UI Slice 2 — import

- [x] Add system video picker.
- [x] Handle select, cancel, inaccessible URI, and process recreation in source.
- [x] Add safe temporary working-copy preparation with clear progress.

### UI Slice 3 — probe review

- [x] Add typed `MediaInfo` and probe result/error models.
- [x] Implement native probe off the main thread.
- [x] Display essential and expanded technical metadata.
- [x] Add loading, ready, and recoverable error states.

### UI Slice 4 — render test

- [x] Add 720p preset and render action after a successful probe.
- [x] Add typed stage/progress/cancel UI tied to the render coordinator.
- [x] Add Android in-app playback/open verification and result state in source.
- [x] Keep 1080p locked until the completed 720p output opens.
- [ ] Verify the full render and playback path on the target Mi Pad.

Each slice should be separately buildable and reviewable. Do not combine this
work with AI providers, authentication, full ATS, or the production timeline.

## 9. UX acceptance checklist

- [ ] First-time user can identify the primary Import action without guidance.
- [ ] App never appears frozen during copy, probe, or render.
- [ ] Back/cancel behavior never deletes the original selected media.
- [ ] Android 10+ import remains permission-free; Android 6–9 grant/deny paths
  both reach the system picker safely.
- [ ] All states remain understandable without relying only on color.
- [ ] Tablet layout uses two panes where useful and preserves preview aspect.
- [ ] Phone layout remains usable at compact width.
- [ ] Large text does not clip critical actions or metadata.
- [ ] Errors provide a specific safe recovery action.
- [ ] UI does not expose arbitrary FFmpeg commands.
- [ ] Source video and rendered output remain local unless the user explicitly
  invokes a later AI/network feature.

## 10. Explicit non-goals for the current UI gate

- Full production dashboard and recent-project persistence.
- Authentication or account UI.
- AI provider/API-key screens.
- Transcript, narrator-script, or TTS production screens.
- ATS timeline editor.
- Subtitle editor and thumbnail studio.
- Multi-project render queue.
- Multiple ABI management.

These are intentionally deferred so the first UI is backed by working local
media capabilities rather than placeholder screens.
