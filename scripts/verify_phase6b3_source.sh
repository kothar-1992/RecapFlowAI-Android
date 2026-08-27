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

effects_file="app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt"
main_file="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
layout_file="app/src/main/res/layout/view_editor_destination.xml"

require_file "app/src/main/kotlin/com/recapflow/ai/media/edit/MirrorCompiler.kt"
require_file "app/src/test/kotlin/com/recapflow/ai/media/edit/MirrorCompilerTest.kt"
require_marker 'rootProject.name = "RecapFlowAI_Phase6B3"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6b3"' "app/build.gradle.kts"
require_marker 'mirrorEnabled: Boolean = false' "app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlan.kt"
require_marker 'if (!settings.enabled || !settings.mirrorEnabled) return null' "app/src/main/kotlin/com/recapflow/ai/media/edit/MirrorCompiler.kt"
require_marker '.setScale(mirror.scaleX, mirror.scaleY)' "$effects_file"
require_marker 'android:id="@+id/mirrorEnabledSwitch"' "$layout_file"
require_marker 'previewPlayer.setVideoEffects(' "$main_file"
require_marker 'mirrorEnabled = mirrorEnabled' "$main_file"
require_marker 'outState.putBoolean(KEY_MIRROR_ENABLED, mirrorEnabled)' "$main_file"
require_marker 'MirrorCompiler.compile(settings)' "app/src/test/kotlin/com/recapflow/ai/media/edit/MirrorCompilerTest.kt"
require_marker 'TransformVideoEffects.forRender(' "app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
reject_marker 'colorEnabledSwitch' "$layout_file"
reject_marker 'zoomEnabledSwitch' "$layout_file"
reject_marker 'speedEnabledSwitch' "$layout_file"
reject_marker 'freezeEnabledSwitch' "$layout_file"
reject_marker 'transitionEnabledSwitch' "$layout_file"

crop_line="$(grep -n -m1 'Crop(' "$project_dir/$effects_file" | cut -d: -f1)"
mirror_line="$(grep -n -m1 'ScaleAndRotateTransformation.Builder' "$project_dir/$effects_file" | cut -d: -f1)"
presentation_line="$(grep -n -m1 'Presentation.createForWidthAndHeight' "$project_dir/$effects_file" | cut -d: -f1)"
if (( crop_line >= mirror_line || mirror_line >= presentation_line )); then
  echo "WRONG EFFECT ORDER: expected Crop -> Mirror -> Presentation" >&2
  exit 1
fi

echo "PASS: RecapFlowAI Phase 6B.3 Mirror source markers are present."
