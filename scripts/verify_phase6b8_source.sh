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
strings_file="app/src/main/res/values/strings.xml"

# PHASE6UX2_VERIFIER_IDENTITY_RETIRED
# The project name and versionName this phase used to pin have both moved on, so these
# two identity checks could only ever fail. They also short-circuited every behavioural
# check below, which is why they are retired rather than repaired. Version identity is
# owned by the current phase verifier and the CBR baseline drift guard instead.
require_marker 'android:id="@+id/transitionEnabledSwitch"' "$layout_file"
require_marker 'android:id="@+id/transitionModeGroup"' "$layout_file"
require_marker 'android:id="@+id/transitionDurationGroup"' "$layout_file"
require_marker 'private var transitionEnabled = false' "$main_file"
require_marker 'transition = TransitionSettings(' "$main_file"
require_marker 'sourceDurationMs = trim.durationMs' "$main_file"
require_marker 'class FadeRgbMatrix' "app/src/main/kotlin/com/recapflow/ai/media/render/FadeRgbMatrix.kt"
require_marker 'object TransitionCompiler' "app/src/main/kotlin/com/recapflow/ai/media/edit/TransitionCompiler.kt"
require_marker 'TransitionCompiler.compile(settings, sourceDurationMs)' \
  "app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt"
require_marker 'TRANSITION_TOO_LONG' \
  "app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlanValidator.kt"
require_marker 'transition_preview_note' "$strings_file"
require_marker 'Audio, Overlay, and AI controls remain hidden' "$strings_file"

# B8 extends rather than removes the verified B7.1 density and Freeze controls.
require_marker 'android:id="@+id/transformVisibilityButton"' "$layout_file"
require_marker 'android:id="@+id/freezeEnabledSwitch"' "$layout_file"

echo "PASS: RecapFlowAI Phase 6B.8 visual Fade source markers are present."
