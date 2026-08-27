# Phase 6E.1.5 — Fit Preview Aspect Parity

## Device evidence

A 576 × 1024 portrait source remains proportional in 9:16 Fit, but selecting 16:9 Fit
expands the preview card to landscape and stretches the portrait pixels horizontally. The
selected mode summary still says Fit, so the visible result violates the established
Phase 6B.1 contract.

## Root cause

Media3 `Presentation` already owns source-to-output mapping when a non-Original aspect is
selected. `LAYOUT_SCALE_TO_FIT` creates the requested output aspect while retaining every
source pixel and adding black pillarbox/letterbox space when required. PlayerView was still
configured to perform its own Fit calculation from the decoded source aspect. On the target
device, that second aspect interpretation distorted the effect output inside the dynamically
resized card.

## Contract

- There is exactly one aspect owner for source preview at a time.
- With no geometry effect, PlayerView uses `RESIZE_MODE_FIT` and preserves source aspect.
- With Presentation or custom Crop, the video effect produces the complete output frame and
  PlayerView uses `RESIZE_MODE_FILL` only to place that frame in the same-aspect card.
- 9:16 → 16:9 Fit shows the complete portrait frame centered with black side bars.
- 9:16 → 16:9 Fill center-crops source content to landscape without stretching.
- If live effects fail, the preview returns to source dimensions and PlayerView Fit.
- The Transformer export graph remains the same shared Crop → Mirror → Color → Zoom →
  Presentation → Overlay sequence.
- Preview move/resize/reset and the Phase 6E.1.4 direct-blur-touch rollback remain intact.

## Non-goals

This patch does not change render dimensions, add background fill styles, re-enable direct
blur gestures, add image/video overlays, or open the Export/Download/TG delivery gate.
