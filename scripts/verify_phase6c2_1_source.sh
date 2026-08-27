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

require_marker 'rootProject.name = "RecapFlowAI_Phase6C2_1"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6c2.1"' "app/build.gradle.kts"
require_marker 'android:id="@+id/previewDragHandle"' "$layout_file"
require_marker 'android:id="@+id/previewResizeHandle"' "$layout_file"
require_marker 'android:id="@+id/previewResetButton"' "$layout_file"
require_marker 'private fun bindPreviewOverlayControls()' "$main_file"
require_marker 'private fun applyPreviewOverlayLayout()' "$main_file"
require_marker 'private fun applyPreviewOverlayFrame(' "$main_file"
require_marker 'KEY_PREVIEW_OVERLAY_SCALE' "$main_file"
require_marker 'R.dimen.rf_editor_preview_edge_margin' "$main_file"
require_marker 'previewBackdrop.layoutParams' "$main_file"

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

echo "PASS: RecapFlowAI Phase 6C.2.1 preview-overlay markers and app colors are valid."
