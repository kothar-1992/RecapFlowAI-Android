#!/usr/bin/env bash
set -euo pipefail

project_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

require_marker() {
  local marker="$1"
  local relative_path="$2"
  if ! grep -Fq "$marker" "$project_dir/$relative_path"; then
    echo "OLD OR WRONG SOURCE: $relative_path lacks $marker" >&2
    exit 1
  fi
}

reject_marker() {
  local marker="$1"
  local relative_path="$2"
  if grep -Fq "$marker" "$project_dir/$relative_path"; then
    echo "STALE OR CONFLICTING SOURCE: $relative_path still contains $marker" >&2
    exit 1
  fi
}

main_file="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
layout_file="app/src/main/res/layout/view_editor_destination.xml"
render_file="app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
validator_file="app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlanValidator.kt"
validator_test="app/src/test/kotlin/com/recapflow/ai/media/edit/EditPlanValidatorTest.kt"

require_marker 'rootProject.name = "RecapFlowAI_Phase6C2"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6c2"' "app/build.gradle.kts"
require_marker 'android:id="@+id/adaptiveSequencePreviewButton"' "$layout_file"
require_marker 'private fun previewAdaptiveSequence()' "$main_file"
require_marker 'previewPlayer.setMediaItems(mediaItems, true)' "$main_file"
require_marker 'override fun onMediaItemTransition' "$main_file"
require_marker 'applyAdaptiveSequenceEffects(rangeIndex)' "$main_file"
require_marker 'R.string.transition_adaptive_summary' "$main_file"
require_marker 'selectedRanges(editPlan).map' "$render_file"
require_marker 'adaptivePlanAllowsPerClipFadeWhenEveryRangeIsLongEnough' "$validator_test"
require_marker 'adaptivePlanRejectsFadeWhenOneRangeIsTooShort' "$validator_test"
reject_marker 'ADAPTIVE_TRANSITION_CONFLICT' "$validator_file"
reject_marker '@color/rf_outline_variant' "$layout_file"

# Catch unresolved app-owned color references before AAPT2 resource linking.
while IFS= read -r color_name; do
  if ! grep -Rqs "<color name=\"$color_name\"" \
    "$project_dir/app/src/main/res/values" \
    "$project_dir/app/src/main/res/values-night"; then
    echo "MISSING RESOURCE: @color/$color_name has no <color> definition" >&2
    exit 1
  fi
done < <(
  grep -Rho '@color/[A-Za-z0-9_]*' "$project_dir/app/src/main/res" |
    sed 's#@color/##' |
    sort -u
)

echo "PASS: RecapFlowAI Phase 6C.2 markers and app color references are valid."
