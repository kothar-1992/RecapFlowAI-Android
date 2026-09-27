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
current_ui="app/src/main/res/values-v28/phase_6ux2_strings.xml"
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

if ! grep -Eq 'versionName = "1\.0-phase' "$version_file"; then
  echo "FAIL: versionName must stay a phase-tagged 1.0-phase* string" >&2
  exit 1
fi
# The phase-labelled presentation copy moved to Phase 6UX.2. This baseline keeps only the
# unlabelled CBR telemetry copy, and the current phase owns the exact version label.
if grep -Fq "PHASE 6F.2.8.1" "$ui"; then
  echo "FAIL: phase-labelled strings must not be pinned to the superseded 6F.2.8.1 label" >&2
  exit 1
fi
require_text "H.264 CBR target" "$ui"
require_text "Phase 6F.2.8.1" "$integrity"

# Drift guard: the phase label shown in the toolbar/badges must match the declared versionName.
# versionName carries a lowercase, dot-free suffix (6ux2) while the UI label is title-cased and
# dotted (6UX.2), so both sides are normalized before comparison.
canonical_phase() {
  printf '%s' "$1" | sed 's/[._-]//g' | tr '[:lower:]' '[:upper:]'
}
declared_version="$(sed -nE 's/.*versionName = "1\.0-phase([^"]+)".*/\1/p' "$version_file" | head -n1)"
[[ -n "$declared_version" ]] || {
  echo "FAIL: could not read a phase suffix from versionName" >&2
  exit 1
}
declared_canonical="$(canonical_phase "$declared_version")"
found_label=0
while read -r line; do
  found_label=1
  label_canonical="$(canonical_phase "${line#* }")"
  if [[ "$label_canonical" != "$declared_canonical" ]]; then
    echo "FAIL: phase label '$line' in $current_ui does not match versionName phase '$declared_version'" >&2
    exit 1
  fi
done < <(grep -oE '<string name="[a-z_0-9]+">[^<]*' "$current_ui" | grep -oE '(Phase|PHASE) [0-9A-Za-z.]+' | sort -u)
if [[ "$found_label" -eq 0 ]]; then
  echo "FAIL: $current_ui declares no phase label" >&2
  exit 1
fi

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
