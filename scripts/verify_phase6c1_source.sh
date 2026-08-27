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

main_file="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
layout_file="app/src/main/res/layout/view_editor_destination.xml"
render_file="app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
edit_file="app/src/main/kotlin/com/recapflow/ai/media/edit/AdaptiveCutCompiler.kt"

require_marker 'rootProject.name = "RecapFlowAI_Phase6C1_1"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6c1.1"' "app/build.gradle.kts"
require_marker 'android:id="@+id/adaptivePresetGroup"' "$layout_file"
require_marker 'android:id="@+id/adaptiveApplySwitch"' "$layout_file"
require_marker 'AdaptiveCutDraftEngine.generate' "$main_file"
require_marker 'adaptiveCuts = AdaptiveCutSettings(' "$main_file"
require_marker 'object AdaptiveCutCompiler' "$edit_file"
require_marker 'object AdaptiveCutDraftEngine' "$edit_file"
require_marker 'EditedMediaItemSequence.Builder()' "$render_file"
require_marker 'selectedRanges(editPlan).map' "$render_file"
require_marker 'ADAPTIVE_TRANSITION_CONFLICT' \
  "app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlanValidator.kt"
require_marker 'class AdaptiveCutCompilerTest' \
  "app/src/test/kotlin/com/recapflow/ai/media/edit/AdaptiveCutCompilerTest.kt"

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

echo "PASS: RecapFlowAI Phase 6C.1.1 markers and app color references are valid."
