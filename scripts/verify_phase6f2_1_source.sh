#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"

bash "$project_dir/scripts/verify_phase6f2_source.sh"

require_marker() {
  local marker="$1"
  local file="$2"
  if ! grep -Fq "$marker" "$file"; then
    echo "FAIL: missing '$marker' in $file" >&2
    exit 1
  fi
}

require_absent() {
  local marker="$1"
  local file="$2"
  if grep -Fq "$marker" "$file"; then
    echo "FAIL: forbidden '$marker' found in $file" >&2
    exit 1
  fi
}

effect_file="app/src/main/kotlin/com/recapflow/ai/media/render/SourceSubtitleBlurEffect.kt"
policy_file="app/src/main/kotlin/com/recapflow/ai/media/render/SourceSubtitleBlurKernelPolicy.kt"
shader_file="app/src/main/assets/shaders/fragment_shader_source_subtitle_blur_es2.glsl"
test_file="app/src/test/kotlin/com/recapflow/ai/media/render/SourceSubtitleBlurKernelPolicyTest.kt"

require_marker 'rootProject.name = "RecapFlowAI_Phase6F2_6_2"' settings.gradle.kts
require_marker 'versionName = "1.0-phase6f2.6.2"' app/build.gradle.kts
require_marker 'object SourceSubtitleBlurKernelPolicy' "$policy_file"
require_marker '(strength / 2f).coerceIn(1f, MAX_RADIUS_PIXELS_AT_REFERENCE)' "$policy_file"
require_marker 'DENSE_9X9_NORMALIZED_KERNEL' "$shader_file"
require_marker 'REGION_CLAMP_PREVENTS_TILE_GHOSTS' "$shader_file"
require_marker 'for (int sampleY = -4; sampleY <= 4; sampleY++)' "$shader_file"
require_marker 'for (int sampleX = -4; sampleX <= 4; sampleX++)' "$shader_file"
require_marker 'blurred *= 1.0 / 81.0' "$shader_file"
require_marker 'uHorizontalStep' "$effect_file"
require_marker 'uVerticalStep' "$effect_file"
require_marker 'SourceSubtitleBlurKernelPolicyTest' "$test_file"
require_marker 'SOURCE_BLUR_DIRECT_TOUCH_ENABLED = false' app/src/main/kotlin/com/recapflow/ai/MainActivity.kt
require_marker 'SourceSubtitleBlurEffect(blur, sourceTimeOffsetUs, fixedSourceTimeUs)' \
  app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt

require_absent 'blurred = original * 0.20' "$shader_file"
require_absent '2.0 * y' "$shader_file"
require_absent 'uHorizontalOffset' "$shader_file"
require_absent 'uVerticalOffset' "$shader_file"

echo "PASS: RecapFlowAI Phase 6F.2.1 source-blur quality markers remain valid in Phase 6F.2.3."
