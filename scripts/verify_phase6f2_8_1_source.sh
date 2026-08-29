#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

render="app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
preset="app/src/main/kotlin/com/recapflow/ai/media/render/RenderPreset.kt"
validation="app/src/main/kotlin/com/recapflow/ai/media/render/RenderedOutputValidation.kt"
quality_test="app/src/test/kotlin/com/recapflow/ai/media/render/RenderQualityPolicyTest.kt"
validation_test="app/src/test/kotlin/com/recapflow/ai/media/render/RenderedOutputValidationPolicyTest.kt"
version_file="app/build.gradle.kts"
ui="app/src/main/res/values-v28/phase_6f2_8_1_strings.xml"
integrity="PROJECT_INTEGRITY.md"

require_text() {
  local needle="$1"
  local file="$2"
  if ! grep -Fq "$needle" "$file"; then
    echo "FAIL: missing '$needle' in $file" >&2
    exit 1
  fi
}

require_text "BITRATE_MODE_CBR" "$render"
if grep -Fq "BITRATE_MODE_VBR" "$render"; then
  echo "FAIL: VBR is still present in the final render coordinator" >&2
  exit 1
fi

require_text 'HD_720P(720, "720p", "HD 720p", 7_500_000, 10_000_000)' "$preset"
require_text 'FULL_HD_1080P(1080, "1080p", "Full HD 1080p", 10_000_000, 15_000_000)' "$preset"
require_text 'QHD_2K(1440, "2K", "2K QHD 1440p", 18_000_000, 28_000_000)' "$preset"

require_text "DURATION_DRIFT_WARNING_MS = 250L" "$validation"
require_text "BASE_DURATION_DRIFT_MS = 350L" "$validation"
require_text "MAX_DURATION_DRIFT_MS = 750L" "$validation"
require_text "MIN_CBR_ACCEPTANCE_PERCENT = 80L" "$validation"
require_text "CBR_WARNING_PERCENT = 90L" "$validation"

require_text "7_500_000" "$quality_test"
require_text "10_000_000" "$quality_test"
require_text "15_000_000" "$quality_test"
require_text "18_000_000" "$quality_test"
require_text "28_000_000" "$quality_test"

require_text "277_315L" "$validation_test"
require_text "277_800L" "$validation_test"
require_text "2_780_000" "$validation_test"
require_text "CBR average bitrate" "$validation_test"

require_text 'versionName = "1.0-phase6f2.8.1"' "$version_file"
require_text "PHASE 6F.2.8.1" "$ui"
require_text "H.264 CBR target" "$ui"
require_text "Phase 6F.2.8.1" "$integrity"

start_count="$(grep -F -c 'transformer?.start(' "$render" || true)"
if [[ "$start_count" -ne 1 ]]; then
  echo "FAIL: expected exactly one final Transformer.start call, found $start_count" >&2
  exit 1
fi

if grep -Fq "transformer.start(" "$render"; then
  echo "FAIL: unexpected additional non-null-safe Transformer.start call found" >&2
  exit 1
fi

echo "PASS: Phase 6F.2.8.1 CBR source invariants verified."
echo "  bitrate mode: CBR"
echo "  targets: 720p 7.5/10, 1080p 10/15, 1440p 18/28 Mbps"
echo "  duration: warning>250 ms, floor=350 ms, cap=750 ms"
echo "  quality gate: hard fail <80%, warning 80-90% when average bitrate is reported"
echo "  final Transformer.start count: 1"
