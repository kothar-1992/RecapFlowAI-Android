#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

MODEL="app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlan.kt"
POLICY="app/src/main/kotlin/com/recapflow/ai/media/edit/ImageOverlayAnimationPolicy.kt"
COMPILER="app/src/main/kotlin/com/recapflow/ai/media/edit/OverlayCompiler.kt"
PREVIEW="app/src/main/kotlin/com/recapflow/ai/media/render/CompositionPreviewTimelinePolicy.kt"
TEST="app/src/test/kotlin/com/recapflow/ai/media/edit/ImageOverlayAnimationPolicyTest.kt"
SPEED_TEST="app/src/test/kotlin/com/recapflow/ai/media/render/ImageOverlayAnimationSpeedProjectionTest.kt"

for path in "$MODEL" "$POLICY" "$COMPILER" "$PREVIEW" "$TEST" "$SPEED_TEST"; do
  [[ -f "$path" ]] || { echo "FAIL: missing $path" >&2; exit 1; }
done

grep -q 'enum class ImageOverlayAnimationPreset' "$MODEL" || {
  echo "FAIL: animation preset model missing" >&2; exit 1;
}
grep -q 'val animation: ImageOverlayAnimationSettings' "$MODEL" || {
  echo "FAIL: ImageOverlaySettings is not carrying animation semantics" >&2; exit 1;
}
grep -q 'phaseOffsetMs' "$COMPILER" || {
  echo "FAIL: clip projection does not preserve logo animation phase" >&2; exit 1;
}
grep -q 'durationMs = scaled(overlays.image.animation.durationMs)' "$PREVIEW" || {
  echo "FAIL: CompositionPlayer Speed projection does not scale animation duration" >&2; exit 1;
}
grep -q 'periodMs = scaled(overlays.image.animation.periodMs)' "$PREVIEW" || {
  echo "FAIL: CompositionPlayer Speed projection does not scale loop period" >&2; exit 1;
}
grep -q 'phaseOffsetMs = scaled(overlays.image.animation.phaseOffsetMs)' "$PREVIEW" || {
  echo "FAIL: CompositionPlayer Speed projection does not scale loop phase offset" >&2; exit 1;
}

echo "PASS: Phase 6H.2 animation foundation source contract is present."
