#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

require_marker() {
  local marker="$1"
  local file="$2"
  grep -Fq "$marker" "$file" || { echo "FAIL: missing '$marker' in $file" >&2; exit 1; }
}

bash scripts/verify_phase6f2_6_1c_source.sh

preset='app/src/main/kotlin/com/recapflow/ai/media/render/RenderPreset.kt'
render='app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt'
validation='app/src/main/kotlin/com/recapflow/ai/media/render/RenderedOutputValidation.kt'
strings='app/src/main/res/values/strings.xml'
quality_test='app/src/test/kotlin/com/recapflow/ai/media/render/RenderQualityPolicyTest.kt'
validation_test='app/src/test/kotlin/com/recapflow/ai/media/render/RenderedOutputValidationPolicyTest.kt'

require_marker 'minimumVideoBitrate = 25_000_000' "$preset"
require_marker 'maximumVideoBitrate = 30_000_000' "$preset"
require_marker 'minimumVideoBitrate = 30_000_000' "$preset"
require_marker 'maximumVideoBitrate = 45_000_000' "$preset"
require_marker 'minimumVideoBitrate = 45_000_000' "$preset"
require_marker 'maximumVideoBitrate = 60_000_000' "$preset"
require_marker 'BITRATE_MODE_CBR' "$render"
require_marker '.setBitrateMode(' "$render"
require_marker '.setEnableFallback(true)' "$render"
require_marker 'less than 80%' "$validation"
require_marker 'less than 50%' "$validation"
require_marker '25–30 Mbps H.264 CBR target' "$strings"
require_marker '30–45 Mbps H.264 CBR target' "$strings"
require_marker '45–60 Mbps H.264 CBR target' "$strings"
require_marker 'lowBitrateSourceGetsTwentyFiveMegabit720pFloor' "$quality_test"
require_marker 'lowBitrateSourceGetsThirtyMegabit1080pFloor' "$quality_test"
require_marker 'severeBitrateShortfallIsVisibleEvenWhenTechnicalOutputPasses' "$validation_test"
require_marker 'Phase 6F.2.6.1D' PLAN.md

python3 - <<'PY2'
from pathlib import Path
render = Path('app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt').read_text()
factory = render.index('val encoderFactory = DefaultEncoderFactory.Builder(appContext)')
bitrate = render.index('.setBitrate(qualityRequest.requestedVideoBitrate)', factory)
cbr = render.index('BITRATE_MODE_CBR', bitrate)
fallback = render.index('.setEnableFallback(true)', cbr)
start = render.index('transformer?.start', fallback)
if not factory < bitrate < cbr < fallback < start:
    raise SystemExit('FAIL: CBR quality settings are not applied before Transformer starts')
print('Phase 6F.2.6.1D CBR quality policy checks: PASS')
PY2

echo 'PASS: RecapFlowAI Phase 6F.2.6.1D high-bitrate export source is valid.'
