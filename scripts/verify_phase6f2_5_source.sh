#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"

bash "$project_dir/scripts/verify_phase6f2_4_source.sh"

require_marker() {
  local marker="$1"
  local file="$2"
  if ! grep -Fq "$marker" "$file"; then
    echo "FAIL: missing '$marker' in $file" >&2
    exit 1
  fi
}

advisor_file="app/src/main/kotlin/com/recapflow/ai/media/edit/DurationFitAdvisor.kt"
validation_file="app/src/main/kotlin/com/recapflow/ai/media/render/RenderedOutputValidation.kt"
activity_file="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
layout_file="app/src/main/res/layout/view_editor_destination.xml"
strings_file="app/src/main/res/values/strings.xml"

# PHASE6UX2_VERIFIER_IDENTITY_RETIRED
# The project name and versionName this phase used to pin have both moved on, so these
# two identity checks could only ever fail. They also short-circuited every behavioural
# check below, which is why they are retired rather than repaired. Version identity is
# owned by the current phase verifier and the CBR baseline drift guard instead.
require_marker 'object DurationFitAdvisor' "$advisor_file"
require_marker 'changing only the final selected source range' "$advisor_file"
require_marker 'updatedPlan.plannedDurationMs == targetDurationMs' "$advisor_file"
require_marker 'fun allowedDurationDriftMs(expectedDurationMs: Long)' "$validation_file"
require_marker 'BASE_DURATION_DRIFT_MS = 250L' "$validation_file"
require_marker 'MAX_DURATION_DRIFT_MS = 750L' "$validation_file"
require_marker 'durationDriftMs > allowedDurationDriftMs' "$validation_file"
require_marker 'exportDurationAdvisorDetail' "$layout_file"
require_marker 'exportApplyDurationButton' "$layout_file"
require_marker 'private fun applyDurationFitSuggestion()' "$activity_file"
require_marker 'DurationFitAdvisor.assess(currentEditPlan(selectedRenderPreset))' "$activity_file"
require_marker 'Final duration advisor' "$strings_file"
require_marker 'longRenderAcceptsBoundedFrameAndCodecDrift' \
  app/src/test/kotlin/com/recapflow/ai/media/render/RenderedOutputValidationPolicyTest.kt
require_marker 'adaptivePlanChangesOnlyTheFinalReviewedRange' \
  app/src/test/kotlin/com/recapflow/ai/media/edit/DurationFitAdvisorTest.kt

python3 - "$project_dir" <<'PY'
import pathlib
import sys
import xml.etree.ElementTree as ET

root = pathlib.Path(sys.argv[1])
for path in (root / "app/src/main/res").rglob("*.xml"):
    ET.parse(path)

validation = (root / "app/src/main/kotlin/com/recapflow/ai/media/render/RenderedOutputValidation.kt").read_text()
if "durationDriftMs > MAX_DURATION_DRIFT_MS" in validation:
    raise SystemExit("DURATION FLOW: fixed hard threshold bypasses the duration-aware policy")

activity = (root / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt").read_text()
apply_start = activity.index("private fun applyDurationFitSuggestion()")
apply_end = activity.find("\n    private fun ", apply_start + 1)
apply_block = activity[apply_start:apply_end]
if "update.reviewedRanges" not in apply_block or "update.trimRange" not in apply_block:
    raise SystemExit("DURATION FLOW: advisor update does not cover Trim and Adaptive Cuts")

print("Phase 6F.2.5 duration reconciliation checks: PASS")
PY

echo "PASS: RecapFlowAI Phase 6F.2.5 duration workflow source is valid."
