#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"

bash "$project_dir/scripts/verify_phase6f2_2_source.sh"

require_marker() {
  local marker="$1"
  local file="$2"
  if ! grep -Fq "$marker" "$file"; then
    echo "FAIL: missing '$marker' in $file" >&2
    exit 1
  fi
}

require_absent() {
  local marker="$1"
  local file="$2"
  if grep -Fq "$marker" "$file"; then
    echo "FAIL: forbidden '$marker' found in $file" >&2
    exit 1
  fi
}

preset_file="app/src/main/kotlin/com/recapflow/ai/media/render/RenderPreset.kt"
activity_file="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
state_file="app/src/main/kotlin/com/recapflow/ai/media/render/RenderUiState.kt"
render_file="app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
validation_file="app/src/main/kotlin/com/recapflow/ai/media/render/RenderedOutputValidation.kt"
layout_file="app/src/main/res/layout/view_editor_destination.xml"
preferences_file="app/src/main/kotlin/com/recapflow/ai/preferences/EditorPreferencesStore.kt"

require_marker 'rootProject.name = "RecapFlowAI_Phase6F2_6_2"' settings.gradle.kts
require_marker 'versionName = "1.0-phase6f2.6.2"' app/build.gradle.kts
require_marker 'HD_720P(' "$preset_file"
require_marker 'FULL_HD_1080P(' "$preset_file"
require_marker 'QHD_2K(' "$preset_file"
require_marker 'shortSidePixels = 1440' "$preset_file"
require_marker 'minimumVideoBitrate = 45_000_000' "$preset_file"
require_marker 'maximumVideoBitrate = 60_000_000' "$preset_file"
require_marker 'val DEFAULT = FULL_HD_1080P' "$preset_file"
require_marker 'exportQuality720Button' "$layout_file"
require_marker 'exportQuality1080Button' "$layout_file"
require_marker 'exportQuality2kButton' "$layout_file"
require_marker 'val renderPreset: RenderPreset = RenderPreset.DEFAULT' \
  app/src/main/kotlin/com/recapflow/ai/preferences/EditorPreferences.kt
require_marker '.putString(key(prefix, "export.preset"), snapshot.renderPreset.name)' \
  "$preferences_file"
require_marker 'const val SCHEMA_VERSION = 2' "$preferences_file"
require_marker 'val preset = selectedRenderPreset' "$activity_file"
require_marker 'Preview exported video (optional)' app/src/main/res/values/strings.xml
require_marker 'RenderedOutputInspector.inspect(output)' "$render_file"
require_marker 'RenderedOutputValidationPolicy.validate(' "$render_file"
require_marker 'minOf(displayWidth, displayHeight) != preset.shortSidePixels' "$validation_file"
require_marker 'val outputWidth: Int' "$state_file"
require_marker 'wrongShortSideIsRejectedInsteadOfSilentlyFallingBack' \
  app/src/test/kotlin/com/recapflow/ai/media/render/RenderedOutputValidationPolicyTest.kt

require_absent 'TEST_720P' "$preset_file"
require_absent 'TEST_1080P' "$preset_file"
require_absent 'playbackVerified' "$state_file"
require_absent 'markPlaybackVerified' "$render_file"
require_absent 'pendingPlaybackVerificationPath' "$activity_file"
require_absent 'UHD_4K' "$preset_file"
require_absent 'exportQuality4kButton' "$layout_file"

python3 - "$project_dir" <<'PY'
import pathlib
import sys
import xml.etree.ElementTree as ET

root = pathlib.Path(sys.argv[1])
for path in (root / "app/src/main/res").rglob("*.xml"):
    ET.parse(path)

activity = (root / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt").read_text()
start = activity.index("private fun startNextRender()")
play = activity.index("private fun playRenderedOutput()", start)
block = activity[start:play]
if block.count("renderCoordinator.start(mediaInfo, editPlan)") != 1:
    raise SystemExit("RENDER FLOW: expected exactly one selected-quality start path")
if "playback" in block.lower() or "FULL_HD_1080P" in block:
    raise SystemExit("RENDER FLOW: a playback/1080 unlock branch still controls rendering")

print("Phase 6F.2.3 exact-quality/single-render/post-validation checks: PASS")
PY

echo "PASS: RecapFlowAI Phase 6F.2.3 production render quality source is valid."
