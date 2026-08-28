#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
require_marker(){ grep -Fq "$1" "$2" || { echo "FAIL: missing '$1' in $2" >&2; exit 1; }; }

build='app/build.gradle.kts'
settings='settings.gradle.kts'
main='app/src/main/kotlin/com/recapflow/ai/MainActivity.kt'
compiler='app/src/main/kotlin/com/recapflow/ai/media/render/Media3CompositionCompiler.kt'
render='app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt'
quality='app/src/main/kotlin/com/recapflow/ai/media/render/RenderQualityPolicy.kt'
preset='app/src/main/kotlin/com/recapflow/ai/media/render/RenderPreset.kt'
fps='app/src/main/kotlin/com/recapflow/ai/media/render/ExportFrameRatePolicy.kt'
inspector='app/src/main/kotlin/com/recapflow/ai/media/render/RenderedOutputInspector.kt'
validation='app/src/main/kotlin/com/recapflow/ai/media/render/RenderedOutputValidation.kt'
plan='PLAN.md'

require_marker 'versionName = "1.0-phase6f2.8"' "$build"
require_marker 'rootProject.name = "RecapFlowAI_Phase6F2_8"' "$settings"
require_marker 'CompositionPlayer.Builder(this).build()' "$main"
require_marker 'compileForPreview' "$compiler"
require_marker 'PREVIEW_FRAME_RATE = 30' "$compiler"
require_marker 'ExportFrameRatePolicy.forSource(mediaInfo.frameRate)' "$compiler"
require_marker 'BITRATE_MODE_VBR' "$render"
require_marker 'bitrateMode=VBR' "$render"
require_marker 'targetFrameRate' "$quality"
require_marker 'standardFrameRateVideoBitrate' "$preset"
require_marker 'HIGH_FRAME_RATE_THRESHOLD = 48' "$fps"
require_marker 'frameRate = format.frameRateOrZero()' "$inspector"
require_marker 'expectedFrameRate' "$validation"
require_marker 'VBR average bitrate' "$validation"
require_marker 'Phase 6F.2.8 SOURCE IMPLEMENTED' "$plan"
require_marker 'transformer?.start(compiledComposition.composition, output.absolutePath)' "$render"

if grep -R -Fq 'BITRATE_MODE_CBR' app/src/main/kotlin/com/recapflow/ai/media/render; then
  echo 'FAIL: CBR request remains in render runtime' >&2; exit 1
fi
if grep -Fq 'TARGET_FRAME_RATE = 30' "$compiler"; then
  echo 'FAIL: final export still exposes hardcoded TARGET_FRAME_RATE' >&2; exit 1
fi
python3 - <<'PY2'
from pathlib import Path
r=Path('app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt').read_text()
m=Path('app/src/main/kotlin/com/recapflow/ai/MainActivity.kt').read_text()
if r.count('transformer?.start(compiledComposition.composition, output.absolutePath)') != 1:
    raise SystemExit('FAIL: expected exactly one authoritative final Transformer start')
if 'Transformer.Builder' in m or 'Transformer.start' in m:
    raise SystemExit('FAIL: preview activity must not start Transformer')
print('Phase 6F.2.8 one-final-render + preview separation: PASS')
PY2

python3 - <<'PY3'
from pathlib import Path
fps=Path('app/src/main/kotlin/com/recapflow/ai/media/render/ExportFrameRatePolicy.kt').read_text()
preset=Path('app/src/main/kotlin/com/recapflow/ai/media/render/RenderPreset.kt').read_text()
for marker in ('commonFrameRates = intArrayOf(24, 25, 30, 48, 50, 60)', 'COMMON_RATE_TOLERANCE = 0.75', 'MAX_FRAME_RATE = 60'):
    if marker not in fps:
        raise SystemExit(f'FAIL: missing fps policy marker {marker}')
for marker in ('8_000_000', '12_000_000', '16_000_000', '24_000_000'):
    if marker not in preset:
        raise SystemExit(f'FAIL: missing bitrate marker {marker}')
print('Phase 6F.2.8 fps/bitrate source smoke: PASS')
PY3

echo 'PASS: RecapFlowAI Phase 6F.2.8 social export quality source contract is valid.'
