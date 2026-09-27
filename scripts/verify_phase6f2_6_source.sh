#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"

bash "$project_dir/scripts/verify_phase6f2_5_source.sh"

require_marker() {
  local marker="$1"
  local file="$2"
  if ! grep -Fq "$marker" "$file"; then
    echo "FAIL: missing '$marker' in $file" >&2
    exit 1
  fi
}

plan_file="app/src/main/kotlin/com/recapflow/ai/media/render/Media3CompositionPlan.kt"
compiler_file="app/src/main/kotlin/com/recapflow/ai/media/render/Media3CompositionCompiler.kt"
coordinator_file="app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
test_file="app/src/test/kotlin/com/recapflow/ai/media/render/Media3CompositionPlanCompilerTest.kt"

# PHASE6UX2_VERIFIER_IDENTITY_RETIRED
# The project name and versionName this phase used to pin have both moved on, so these
# two identity checks could only ever fail. They also short-circuited every behavioural
# check below, which is why they are retired rather than repaired. Version identity is
# owned by the current phase verifier and the CBR baseline drift guard instead.
require_marker 'object Media3CompositionPlanCompiler' "$plan_file"
require_marker 'val selectedRanges: List<TrimRange>' "$plan_file"
require_marker 'val plannedDurationMs: Long' "$plan_file"
require_marker 'object Media3CompositionCompiler' "$compiler_file"
require_marker 'Composition.Builder(sequences).build()' "$compiler_file"
require_marker 'transformer?.start(compiledComposition.composition, output.absolutePath)' "$coordinator_file"
require_marker 'composition=${compiledComposition.plan.summary}' "$coordinator_file"
require_marker 'adaptiveFreezeAndMixCompileOneAuthoritativeTopology' "$test_file"
require_marker 'muteOnSilentSourceProducesVideoOnlyComposition' "$test_file"
require_marker 'Phase 6F.2.6 — Shared Media3 Composition workflow' PLAN.md

python3 - "$project_dir" <<'PY'
import pathlib
import sys
import xml.etree.ElementTree as ET

root = pathlib.Path(sys.argv[1])
for path in (root / "app/src/main/res").rglob("*.xml"):
    ET.parse(path)

coordinator = (root / "app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt").read_text()
if "transformer?.start(editedMediaItems.single()" in coordinator:
    raise SystemExit("COMPOSITION FLOW: direct EditedMediaItem Transformer branch remains")
if coordinator.count("transformer?.start(") != 1:
    raise SystemExit("COMPOSITION FLOW: Transformer must have exactly one authoritative start path")

compiler = (root / "app/src/main/kotlin/com/recapflow/ai/media/render/Media3CompositionCompiler.kt").read_text()
if ".setDurationUs(range.durationMs * 1_000L)" in compiler:
    raise SystemExit("COMPOSITION FLOW: encoded source duration must remain intrinsic/pre-clipping")
if "experimentalSetForceAudioTrack(true)" not in compiler:
    raise SystemExit("COMPOSITION FLOW: freeze/source-audio continuity guard is missing")

print("Phase 6F.2.6 shared Media3 Composition checks: PASS")
PY

echo "PASS: RecapFlowAI Phase 6F.2.6 shared Composition workflow source is valid."
