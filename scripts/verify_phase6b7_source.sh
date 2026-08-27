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
plan_file="app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlan.kt"
freeze_file="app/src/main/kotlin/com/recapflow/ai/media/edit/FreezeCompiler.kt"
asset_file="app/src/main/kotlin/com/recapflow/ai/media/render/FreezeFrameAssetFactory.kt"

require_file "$freeze_file"
require_file "$asset_file"
require_file "app/src/test/kotlin/com/recapflow/ai/media/edit/FreezeCompilerTest.kt"
require_marker 'rootProject.name = "RecapFlowAI_Phase6B7"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6b7"' "app/build.gradle.kts"
require_marker 'android:id="@+id/freezeEnabledSwitch"' "$layout_file"
require_marker 'android:id="@+id/freezeDurationGroup"' "$layout_file"
require_marker 'android:id="@+id/freezePreviewButton"' "$layout_file"
require_marker 'supportedDurationsMs = setOf(1_000L, 2_000L, 3_000L)' "$freeze_file"
require_marker 'if (!settings.enabled || !freeze.enabled) return null' "$freeze_file"
require_marker '(FreezeCompiler.compile(transform)?.durationMs ?: 0L)' "$plan_file"
require_marker 'freezePreviewHandler.postDelayed(freezePreviewCompletion, freezeDurationMs)' "$main_file"
require_marker 'outState.putLong(KEY_FREEZE_DURATION_MS, freezeDurationMs)' "$main_file"
require_marker 'FreezeFrameAssetFactory.create(' "$coordinator_file"
require_marker '.setImageDurationMs(freeze.durationMs)' "$coordinator_file"
require_marker 'EditedMediaItemSequence.Builder()' "$coordinator_file"
require_marker '.experimentalSetForceAudioTrack(true)' "$coordinator_file"
require_marker 'Composition.Builder(listOf(sequence)).build()' "$coordinator_file"
require_marker 'settings.zoom.copy(enabled = false)' "$coordinator_file"
require_marker 'deleteActiveFreezeFrame()' "$coordinator_file"
reject_marker 'transitionEnabledSwitch' "$layout_file"

echo "PASS: RecapFlowAI Phase 6B.7 Intro Freeze source markers are present."
