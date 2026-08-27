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

main_file="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
layout_file="app/src/main/res/layout/view_editor_destination.xml"
coordinator_file="app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
speed_effects_file="app/src/main/kotlin/com/recapflow/ai/media/render/TransformSpeedEffects.kt"

require_file "app/src/main/kotlin/com/recapflow/ai/media/edit/SpeedCompiler.kt"
require_file "$speed_effects_file"
require_file "app/src/test/kotlin/com/recapflow/ai/media/edit/SpeedCompilerTest.kt"
require_marker 'rootProject.name = "RecapFlowAI_Phase6B6"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6b6"' "app/build.gradle.kts"
require_marker 'android:id="@+id/speedEnabledSwitch"' "$layout_file"
require_marker 'android:id="@+id/speedModeGroup"' "$layout_file"
require_marker 'android:id="@+id/speed05Button"' "$layout_file"
require_marker 'android:id="@+id/speed20Button"' "$layout_file"
require_marker 'if (!settings.enabled || !settings.speedEnabled) return null' "app/src/main/kotlin/com/recapflow/ai/media/edit/SpeedCompiler.kt"
require_marker 'if (settings.speed == NEUTRAL_SPEED) return null' "app/src/main/kotlin/com/recapflow/ai/media/edit/SpeedCompiler.kt"
require_marker 'Effects.createExperimentalSpeedChangingEffect(' "$speed_effects_file"
require_marker 'ConstantSpeedProvider(compiled.multiplier)' "$speed_effects_file"
require_marker 'videoEffect = SpeedChangeEffect(compiled.multiplier)' "$speed_effects_file"
require_marker 'override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET' "$speed_effects_file"
require_marker 'speedEffects?.audioProcessor?.let(::listOf).orEmpty()' "$coordinator_file"
require_marker 'speedEffect = speedEffects?.videoEffect' "$coordinator_file"
require_marker 'previewPlayer.setPlaybackSpeed(if (isSourcePreview) currentPreviewSpeed() else 1f)' "$main_file"
require_marker 'outState.putBoolean(KEY_SPEED_ENABLED, speedEnabled)' "$main_file"
require_marker 'SpeedCompiler.compile(transform)' "app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlan.kt"
reject_marker 'freezeEnabledSwitch' "$layout_file"
reject_marker 'transitionEnabledSwitch' "$layout_file"

speed_line="$(grep -n -m1 'speedEffect?.let(::add)' "$project_dir/app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt" | cut -d: -f1)"
frame_drop_line="$(grep -n -m1 'FrameDropEffect.createDefaultFrameDropEffect' "$project_dir/app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt" | cut -d: -f1)"
if (( speed_line >= frame_drop_line )); then
  echo "WRONG EFFECT ORDER: expected Speed before FrameDrop" >&2
  exit 1
fi

echo "PASS: RecapFlowAI Phase 6B.6 Speed source markers are present."
