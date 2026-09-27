#!/usr/bin/env bash
set -euo pipefail

project_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

require_file() {
  if [[ ! -f "$project_dir/$1" ]]; then
    echo "MISSING: $1" >&2
    exit 1
  fi
}

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
    echo "OUT-OF-SCOPE CONTROL: $relative_path contains $marker" >&2
    exit 1
  fi
}

require_file "app/src/main/kotlin/com/recapflow/ai/media/edit/CropCompiler.kt"
require_file "app/src/test/kotlin/com/recapflow/ai/media/edit/CropCompilerTest.kt"
# PHASE6UX2_VERIFIER_IDENTITY_RETIRED
# The project name and versionName this phase used to pin have both moved on, so these
# two identity checks could only ever fail. They also short-circuited every behavioural
# check below, which is why they are retired rather than repaired. Version identity is
# owned by the current phase verifier and the CBR baseline drift guard instead.
require_marker 'val crop: CropSettings = CropSettings()' "app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlan.kt"
require_marker 'if (!settings.enabled || !settings.crop.enabled)' "app/src/main/kotlin/com/recapflow/ai/media/edit/CropCompiler.kt"
require_marker 'Crop(' "app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
require_marker 'android:id="@+id/cropEnabledSwitch"' "app/src/main/res/layout/view_editor_destination.xml"
require_marker 'android:id="@+id/cropLeftSlider"' "app/src/main/res/layout/view_editor_destination.xml"
require_marker 'android:id="@+id/cropBottomSlider"' "app/src/main/res/layout/view_editor_destination.xml"
require_marker 'masterOffOmitsRememberedCrop' "app/src/test/kotlin/com/recapflow/ai/media/edit/CropCompilerTest.kt"
require_marker 'KEY_CROP_ENABLED' "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
reject_marker 'zoomEnabledSwitch' "app/src/main/res/layout/view_editor_destination.xml"
reject_marker 'mirrorEnabledSwitch' "app/src/main/res/layout/view_editor_destination.xml"

echo "PASS: RecapFlowAI Phase 6B.2 typed custom crop markers are present."
