#!/usr/bin/env bash
set -euo pipefail

project_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

bash "$project_dir/scripts/verify_phase6f1_source.sh"

require_marker() {
  local marker="$1"
  local relative_path="$2"
  if ! grep -Fq "$marker" "$project_dir/$relative_path"; then
    echo "OLD OR INCOMPLETE SOURCE: $relative_path lacks $marker" >&2
    exit 1
  fi
}

reject_marker() {
  local marker="$1"
  local relative_path="$2"
  if grep -Fq "$marker" "$project_dir/$relative_path"; then
    echo "UNSUPPORTED MEDIA3 API: $relative_path contains $marker" >&2
    exit 1
  fi
}

quality_file="app/src/main/kotlin/com/recapflow/ai/media/render/RenderQualityPolicy.kt"
preset_file="app/src/main/kotlin/com/recapflow/ai/media/render/RenderPreset.kt"
render_file="app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
state_file="app/src/main/kotlin/com/recapflow/ai/media/render/RenderUiState.kt"
main_file="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
test_file="app/src/test/kotlin/com/recapflow/ai/media/render/RenderQualityPolicyTest.kt"

require_marker 'rootProject.name = "RecapFlowAI_Phase6F2_6_2"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6f2.6.2"' "app/build.gradle.kts"
require_marker 'minimumVideoBitrate = 25_000_000' "$preset_file"
require_marker 'minimumVideoBitrate = 30_000_000' "$preset_file"
require_marker 'maximumVideoBitrate = 45_000_000' "$preset_file"
require_marker 'object RenderQualityPolicy' "$quality_file"
require_marker 'isPreviousRecapFlowExport' "$quality_file"
require_marker 'VideoEncoderSettings.Builder()' "$render_file"
require_marker '.setBitrate(qualityRequest.requestedVideoBitrate)' "$render_file"
require_marker '.setEnableFallback(true)' "$render_file"
require_marker '.setEncoderFactory(encoderFactory)' "$render_file"
require_marker 'result.averageVideoBitrate.takeIf { it > 0 }' "$render_file"
require_marker 'val requestedVideoBitrate: Int' "$state_file"
require_marker 'R.string.render_quality_result' "$main_file"
require_marker 'lowBitrateSourceGetsTwentyFiveMegabit720pFloor' "$test_file"
require_marker 'previousRecapFlowOutputIsIdentifiedForGenerationLossWarning' "$test_file"
require_marker 'PHASE6F1_1_RENDER_QUALITY_HOTFIX.md' "PLAN.md"

reject_marker '.setEnableFormatFallback(' "$render_file"

python3 - "$project_dir" <<'PY'
import pathlib
import sys
import xml.etree.ElementTree as ET

root = pathlib.Path(sys.argv[1])
render = (root / "app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt").read_text()
factory = render.index("val encoderFactory = DefaultEncoderFactory.Builder(appContext)")
bitrate = render.index(".setBitrate(qualityRequest.requestedVideoBitrate)", factory)
inject = render.index(".setEncoderFactory(encoderFactory)", bitrate)
start = render.index("transformer?.start", inject)
if not factory < bitrate < inject < start:
    raise SystemExit("ENCODER QUALITY: requested bitrate must be injected before export starts")

for path in (root / "app/src/main/res").rglob("*.xml"):
    ET.parse(path)

print("Phase 6F.1.1.1 encoder-quality and Media3 API checks remain valid: PASS")
PY

echo "PASS: RecapFlowAI Phase 6F.1.1.1 source markers remain valid in Phase 6F.2.3."
