#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

require_marker() {
  local marker="$1"
  local file="$2"
  grep -Fq "$marker" "$file" || { echo "FAIL: missing '$marker' in $file" >&2; exit 1; }
}

bash scripts/verify_phase6f2_6_1d_source.sh

build='app/build.gradle.kts'
settings='settings.gradle.kts'
render='app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt'
test='app/src/test/kotlin/com/recapflow/ai/media/render/FullEditPlanCombinationRegressionTest.kt'
plan='PLAN.md'

require_marker 'versionName = "1.0-phase6f2.6.2"' "$build"
require_marker 'rootProject.name = "RecapFlowAI_Phase6F2_6_2"' "$settings"
require_marker 'class FullEditPlanCombinationRegressionTest' "$test"
require_marker 'allReviewedOperationsSurviveOneCombinedPlan' "$test"
require_marker 'overlayWindowsProjectCorrectlyIntoEveryAdaptiveClip' "$test"
require_marker 'masterSwitchesOffOmitRememberedChildOperationsWithoutDestroyingThem' "$test"
require_marker 'exportPresetChangesOnlyFinalGeometryAndQualityBudget' "$test"
require_marker 'Phase 6F.2.6.2' "$plan"
require_marker 'Full EditPlan Combination Regression' "$plan"
require_marker 'transformer?.start(compiledComposition.composition, output.absolutePath)' "$render"

python3 - <<'PY'
from pathlib import Path
render = Path('app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt').read_text()
block_start = render.index('val compiledComposition = Media3CompositionCompiler.compile(')
block_end = render.index(')', block_start)
block = render[block_start:block_end]
if block.count('input = input') != 1:
    raise SystemExit('FAIL: Media3CompositionCompiler.compile must receive input exactly once')
if render.count('transformer?.start(compiledComposition.composition, output.absolutePath)') != 1:
    raise SystemExit('FAIL: expected exactly one authoritative Composition Transformer start')
if 'transformer?.start(editedMediaItem' in render or 'transformer?.start(mediaItem' in render:
    raise SystemExit('FAIL: legacy direct item Transformer start found')
print('Phase 6F.2.6.2 one-final-render source contract: PASS')
PY

echo 'PASS: RecapFlowAI Phase 6F.2.6.2 Full EditPlan Combination Regression source is valid.'
