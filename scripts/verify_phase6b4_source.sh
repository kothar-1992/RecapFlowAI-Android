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

require_file "app/src/main/kotlin/com/recapflow/ai/media/edit/ColorCompiler.kt"
require_file "app/src/test/kotlin/com/recapflow/ai/media/edit/ColorCompilerTest.kt"
require_marker 'rootProject.name = "RecapFlowAI_Phase6B4"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6b4"' "app/build.gradle.kts"
require_marker 'val color: ColorSettings = ColorSettings()' "app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlan.kt"
require_marker 'if (!settings.enabled || !color.enabled || !color.isValid() || color.isNeutral())' "app/src/main/kotlin/com/recapflow/ai/media/edit/ColorCompiler.kt"
require_marker 'android:id="@+id/colorEnabledSwitch"' "$layout_file"
require_marker 'android:id="@+id/colorBrightnessSlider"' "$layout_file"
require_marker 'android:id="@+id/colorContrastSlider"' "$layout_file"
require_marker 'android:id="@+id/colorSaturationSlider"' "$layout_file"
require_marker 'android:id="@+id/colorTemperatureSlider"' "$layout_file"
require_marker 'android:id="@+id/colorResetButton"' "$layout_file"
require_marker 'ColorCompiler.compile(settings)' "$effects_file"
require_marker 'Brightness(color.brightness)' "$effects_file"
require_marker 'Contrast(color.contrast)' "$effects_file"
require_marker 'HslAdjustment.Builder()' "$effects_file"
require_marker 'RgbAdjustment.Builder()' "$effects_file"
require_marker 'previewPlayer.setVideoEffects(' "$main_file"
require_marker 'color = ColorSettings(' "$main_file"
require_marker 'outState.putBoolean(KEY_COLOR_ENABLED, colorEnabled)' "$main_file"
require_marker 'TransformVideoEffects.forRender(' "app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
reject_marker 'zoomEnabledSwitch' "$layout_file"
reject_marker 'speedEnabledSwitch' "$layout_file"
reject_marker 'freezeEnabledSwitch' "$layout_file"
reject_marker 'transitionEnabledSwitch' "$layout_file"

crop_line="$(grep -n -m1 'Crop(' "$project_dir/$effects_file" | cut -d: -f1)"
mirror_line="$(grep -n -m1 'ScaleAndRotateTransformation.Builder' "$project_dir/$effects_file" | cut -d: -f1)"
brightness_line="$(grep -n -m1 'Brightness(color.brightness)' "$project_dir/$effects_file" | cut -d: -f1)"
contrast_line="$(grep -n -m1 'Contrast(color.contrast)' "$project_dir/$effects_file" | cut -d: -f1)"
saturation_line="$(grep -n -m1 'HslAdjustment.Builder' "$project_dir/$effects_file" | cut -d: -f1)"
temperature_line="$(grep -n -m1 'RgbAdjustment.Builder' "$project_dir/$effects_file" | cut -d: -f1)"
presentation_line="$(grep -n -m1 'Presentation.createForWidthAndHeight' "$project_dir/$effects_file" | cut -d: -f1)"
if (( crop_line >= mirror_line || mirror_line >= brightness_line || brightness_line >= contrast_line || contrast_line >= saturation_line || saturation_line >= temperature_line || temperature_line >= presentation_line )); then
  echo "WRONG EFFECT ORDER: expected Crop -> Mirror -> Brightness -> Contrast -> Saturation -> Temperature -> Presentation" >&2
  exit 1
fi

echo "PASS: RecapFlowAI Phase 6B.4 Color source markers are present."
