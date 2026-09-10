#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

bash scripts/verify_phase6h2_animation_foundation.sh

VISUAL="app/src/main/kotlin/com/recapflow/ai/media/edit/ImageOverlayAnimationVisualPolicy.kt"
GEOMETRY="app/src/main/kotlin/com/recapflow/ai/media/edit/ImageOverlayAnimationGeometryPolicy.kt"
EFFECT="app/src/main/kotlin/com/recapflow/ai/media/render/StaticImageOverlayEffect.kt"
SHADER="app/src/main/assets/shaders/fragment_shader_static_image_overlay_es2.glsl"
VISUAL_TEST="app/src/test/kotlin/com/recapflow/ai/media/edit/ImageOverlayAnimationVisualPolicyTest.kt"
GEOMETRY_TEST="app/src/test/kotlin/com/recapflow/ai/media/edit/ImageOverlayAnimationGeometryPolicyTest.kt"

for path in "$VISUAL" "$GEOMETRY" "$EFFECT" "$SHADER" "$VISUAL_TEST" "$GEOMETRY_TEST"; do
  [[ -f "$path" ]] || { echo "FAIL: missing $path" >&2; exit 1; }
done

for preset in FADE FADE_SCALE POP SLIDE PULSE FLOAT ROTATE BOUNCE; do
  grep -q "ImageOverlayAnimationPreset.$preset" "$VISUAL" || {
    echo "FAIL: visual curve missing for $preset" >&2; exit 1;
  }
done

grep -q 'ImageOverlayAnimationVisualPolicy.resolve' "$EFFECT" || {
  echo "FAIL: shared image-overlay effect is not consuming animation visual policy" >&2; exit 1;
}
grep -q 'ImageOverlayAnimationGeometryPolicy.resolve' "$EFFECT" || {
  echo "FAIL: shared image-overlay effect is not applying frame-safe animation geometry" >&2; exit 1;
}
grep -q 'uAnimationEnabled' "$EFFECT" || {
  echo "FAIL: GL effect does not select animated/static shader branches" >&2; exit 1;
}
grep -q 'if (uAnimationEnabled < 0.5)' "$SHADER" || {
  echo "FAIL: shader does not retain a dedicated static NONE branch" >&2; exit 1;
}
grep -q 'uAnimationRotationRadians' "$SHADER" || {
  echo "FAIL: shader rotation sampling is missing" >&2; exit 1;
}
grep -q 'overlayCoordinate.x < 0.0' "$SHADER" || {
  echo "FAIL: animated inverse sampling bounds check is missing" >&2; exit 1;
}

echo "PASS: Phase 6H.2 shared GL animation source contract is present."
