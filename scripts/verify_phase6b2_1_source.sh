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

require_file "app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt"
require_marker 'rootProject.name = "RecapFlowAI_Phase6B2_1"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6b2.1"' "app/build.gradle.kts"
require_marker 'implementation(libs.androidx.media3.exoplayer)' "app/build.gradle.kts"
require_marker 'implementation(libs.androidx.media3.ui)' "app/build.gradle.kts"
require_marker '<androidx.media3.ui.PlayerView' "app/src/main/res/layout/view_editor_destination.xml"
require_marker 'fun forPreview(' "app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt"
require_marker 'fun forRender(' "app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt"
require_marker 'Crop(' "app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt"
require_marker 'Presentation.createForWidthAndHeight' "app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt"
require_marker 'TransformVideoEffects.forRender(' "app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
require_marker 'previewPlayer.setVideoEffects(' "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
require_marker 'previewFallbackActive' "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
require_marker 'private const val PREVIEW_FRAME_MS = 500L' "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
require_marker 'Live preview updates before render.' "app/src/main/res/values/strings.xml"
reject_marker 'Crop(' "app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
reject_marker 'Presentation.createForWidthAndHeight' "app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
reject_marker 'zoomEnabledSwitch' "app/src/main/res/layout/view_editor_destination.xml"
reject_marker 'mirrorEnabledSwitch' "app/src/main/res/layout/view_editor_destination.xml"

crop_line="$(grep -n -m1 'Crop(' "$project_dir/app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt" | cut -d: -f1)"
presentation_line="$(grep -n -m1 'Presentation.createForWidthAndHeight' "$project_dir/app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt" | cut -d: -f1)"
if (( crop_line >= presentation_line )); then
  echo "WRONG EFFECT ORDER: Crop must be added before Presentation" >&2
  exit 1
fi

echo "PASS: RecapFlowAI Phase 6B.2.1 live Transform preview markers are present."
