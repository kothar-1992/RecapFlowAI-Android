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

require_marker 'rootProject.name = "RecapFlowAI_Phase6B7_1"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6b7.1"' "app/build.gradle.kts"
require_marker 'android:id="@+id/transformVisibilityButton"' "$layout_file"
require_marker 'android:id="@+id/transformControlsGroup"' "$layout_file"
require_marker 'private var transformDetailsVisible = true' "$main_file"
require_marker 'transformDetailsVisible = !transformDetailsVisible' "$main_file"
require_marker 'editor.transformControlsGroup.isVisible = transformDetailsVisible' "$main_file"
require_marker 'outState.putBoolean(KEY_TRANSFORM_DETAILS_VISIBLE, transformDetailsVisible)' "$main_file"
require_marker 'private const val KEY_TRANSFORM_DETAILS_VISIBLE' "$main_file"
require_marker 'transform_hide_controls' "app/src/main/res/values/strings.xml"
require_marker 'transform_show_controls' "app/src/main/res/values/strings.xml"

# The UI refinement must preserve the Phase 6B.7 operation and keep Transitions hidden.
require_marker 'android:id="@+id/freezeEnabledSwitch"' "$layout_file"
if grep -Fq 'transitionEnabledSwitch' "$project_dir/$layout_file"; then
  echo "OUT-OF-SCOPE CONTROL: Transition UI is exposed" >&2
  exit 1
fi

echo "PASS: RecapFlowAI Phase 6B.7.1 collapsible Transform source markers are present."
