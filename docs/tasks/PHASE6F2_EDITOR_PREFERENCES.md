# Phase 6F.2 — Save, restore, and reset editor preferences

## Goal

Reduce repeated setup without coupling mutable UI preferences to an immutable render job.

## Included

- Versioned, app-local SharedPreferences storage.
- Debounced last-session capture and optional launch-time restore.
- One explicit saved preset.
- Restore preset and restore last session actions.
- Reset the active Clips, Transform, Audio, or Overlay section.
- Confirmed Reset all action.
- Persistence for Transform, audio gains/policy, Overlay geometry/strength/opacity,
  Adaptive pacing preset, selected editor section, expanded panels, and preview-card layout.
- Validation/sanitization before restored values reach controls or Media3.

## Privacy and lifecycle boundary

The store must never contain source media paths/URIs, imported image/audio paths, trim ranges,
player position, adaptive reviewed ranges, render job state, output paths/URIs, API keys, tokens,
or credentials. Missing image/audio assets keep the matching restored operation disabled.

## Acceptance

- A process restart can restore safe editor preferences when Auto-restore is On.
- Rotation/process Bundle restoration remains higher priority than SharedPreferences.
- Save preset and both restore actions work without starting a render.
- Reset current section leaves other editor sections unchanged.
- Reset all removes the saved preset, restores safe defaults, and does not delete media.
- Active rendering disables persistence mutation controls.
- Existing Trim, realtime preview, 720p/1080p render, public export, and cancellation behavior
  remain unchanged.
