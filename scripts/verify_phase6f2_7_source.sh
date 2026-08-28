#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

require_marker() {
  local marker="$1"
  local file="$2"
  grep -Fq "$marker" "$file" || { echo "FAIL: missing '$marker' in $file" >&2; exit 1; }
}

build='app/build.gradle.kts'
settings='settings.gradle.kts'
main='app/src/main/kotlin/com/recapflow/ai/MainActivity.kt'
compiler='app/src/main/kotlin/com/recapflow/ai/media/render/Media3CompositionCompiler.kt'
speed='app/src/main/kotlin/com/recapflow/ai/media/render/TransformSpeedEffects.kt'
effects='app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt'
session='app/src/main/kotlin/com/recapflow/ai/media/render/RealtimePreviewSession.kt'
policy='app/src/main/kotlin/com/recapflow/ai/media/render/CompositionPreviewTimelinePolicy.kt'
test='app/src/test/kotlin/com/recapflow/ai/media/render/CompositionPreviewTimelinePolicyTest.kt'
plan='PLAN.md'
full_regression='app/src/test/kotlin/com/recapflow/ai/media/render/FullEditPlanCombinationRegressionTest.kt'
render='app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt'

require_marker 'class FullEditPlanCombinationRegressionTest' "$full_regression"
require_marker 'allReviewedOperationsSurviveOneCombinedPlan' "$full_regression"
require_marker 'overlayWindowsProjectCorrectlyIntoEveryAdaptiveClip' "$full_regression"
require_marker 'masterSwitchesOffOmitRememberedChildOperationsWithoutDestroyingThem' "$full_regression"
require_marker 'transformer?.start(compiledComposition.composition, output.absolutePath)' "$render"

require_marker 'versionName = "1.0-phase6f2.7"' "$build"
require_marker 'rootProject.name = "RecapFlowAI_Phase6F2_7"' "$settings"
require_marker 'recapflow.composition.preview.enabled' "$build"
require_marker 'ENABLE_COMPOSITION_PLAYER_PREVIEW' "$build"
require_marker 'CompositionPlayer.Builder(this).build()' "$main"
require_marker 'prepareCompositionPreview' "$main"
require_marker 'fallbackFromCompositionPreview' "$main"
require_marker 'compositionPreviewBlockedPath' "$main"
require_marker 'compileForPreview' "$compiler"
require_marker 'setDurationUs(mediaInfo.durationMs * 1_000L)' "$compiler"
require_marker 'forCompositionPreview' "$speed"
require_marker 'Effects.createExperimentalSpeedChangingEffect' "$speed"
require_marker 'fun forCompositionPreview' "$effects"
require_marker 'speedEffect?.let(::add)' "$effects"
require_marker 'force: Boolean = false' "$session"
require_marker 'object CompositionPreviewTimelinePolicy' "$policy"
require_marker 'mapsAdaptiveTimelineWithTwoTimesSpeed' "$test"
require_marker 'Phase 6F.2.7' "$plan"
require_marker 'AndroidIDE/device verification pending' "$plan"

python3 - <<'PY'
from pathlib import Path
main = Path('app/src/main/kotlin/com/recapflow/ai/MainActivity.kt').read_text()
render = Path('app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt').read_text()
compiler = Path('app/src/main/kotlin/com/recapflow/ai/media/render/Media3CompositionCompiler.kt').read_text()

if 'Transformer.Builder' in main or 'Transformer.start' in main:
    raise SystemExit('FAIL: preview activity must not create/start Transformer')
if render.count('transformer?.start(compiledComposition.composition, output.absolutePath)') != 1:
    raise SystemExit('FAIL: final export must retain exactly one authoritative Transformer start')
preview_start = compiler.index('fun compileForPreview(')
preview_end = compiler.index('private fun compileInternal(', preview_start)
preview_block = compiler[preview_start:preview_end]
if '.mp4' in preview_block or 'Transformer' in preview_block:
    raise SystemExit('FAIL: compileForPreview must not create media output or start Transformer')
print('Phase 6F.2.7 preview/final-render separation: PASS')
PY

# Pure Kotlin timeline policy smoke test. Android/Media3 classes are deliberately excluded.
if command -v kotlinc >/dev/null 2>&1; then
  tmpdir="$(mktemp -d)"
  trap 'rm -rf "$tmpdir"' EXIT
  kotlinc \
    app/src/main/kotlin/com/recapflow/ai/media/edit/*.kt \
    app/src/main/kotlin/com/recapflow/ai/media/render/RenderPreset.kt \
    app/src/main/kotlin/com/recapflow/ai/media/render/CompositionPreviewTimelinePolicy.kt \
    -d "$tmpdir/policy.jar"
  cat > "$tmpdir/Smoke.kt" <<'KOTLIN'
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TrimRange
import com.recapflow.ai.media.render.CompositionPreviewTimelinePolicy

fun main() {
    val settings = TransformSettings(enabled = true, speedEnabled = true, speed = 2f)
    val ranges = listOf(TrimRange(10_000L, 20_000L), TrimRange(40_000L, 50_000L))
    check(CompositionPreviewTimelinePolicy.sourceToOutputMs(45_000L, ranges, settings) == 7_500L)
    check(CompositionPreviewTimelinePolicy.outputToSourceMs(7_500L, ranges, settings) == 45_000L)
}
KOTLIN
  kotlinc -classpath "$tmpdir/policy.jar" "$tmpdir/Smoke.kt" -include-runtime -d "$tmpdir/smoke.jar"
  java -cp "$tmpdir/smoke.jar:$tmpdir/policy.jar" SmokeKt
  echo 'Phase 6F.2.7 pure timeline smoke: PASS'
fi

echo 'PASS: RecapFlowAI Phase 6F.2.7 CompositionPlayer preview source contract is valid.'
