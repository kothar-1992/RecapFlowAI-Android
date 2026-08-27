#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

require_marker() {
  local marker="$1"
  local file="$2"
  if ! grep -Fq "$marker" "$file"; then
    echo "FAIL: missing marker '$marker' in $file" >&2
    exit 1
  fi
}

bash scripts/verify_phase6f2_6_1b_source.sh

activity='app/src/main/kotlin/com/recapflow/ai/MainActivity.kt'
overlay='app/src/main/kotlin/com/recapflow/ai/media/edit/OverlayCompiler.kt'
compiler='app/src/main/kotlin/com/recapflow/ai/media/render/Media3CompositionCompiler.kt'
policy='app/src/main/kotlin/com/recapflow/ai/media/render/CompositionOverlayTimelinePolicy.kt'
test_overlay='app/src/test/kotlin/com/recapflow/ai/media/edit/OverlayCompilerTest.kt'
test_policy='app/src/test/kotlin/com/recapflow/ai/media/render/CompositionOverlayTimelinePolicyTest.kt'

require_marker 'sourceSubtitleBlurRangeFollowsTrim' "$activity"
require_marker 'imageOverlayRangeFollowsTrim' "$activity"
require_marker 'ensureSourceBlurRange()' "$activity"
require_marker 'ensureImageOverlayRange()' "$activity"
require_marker 'projectToRange(settings: OverlaySettings, sourceRange: TrimRange)' "$overlay"
require_marker 'val localOverlays = OverlayCompiler.projectToRange(editPlan.overlays, range)' "$compiler"
require_marker 'CompositionOverlayTimelinePolicy.localEffectTimeOffsetUs' "$compiler"
require_marker 'object CompositionOverlayTimelinePolicy' "$policy"
require_marker 'projectToRangeKeepsFullBlurActiveAcrossLateClippedItem' "$test_overlay"
require_marker 'laterSequenceItemSubtractsItsCompositionOffset' "$test_policy"
require_marker 'Phase 6F.2.6.1C' PLAN.md

python3 - <<'PY2'
from pathlib import Path
compiler = Path('app/src/main/kotlin/com/recapflow/ai/media/render/Media3CompositionCompiler.kt').read_text()
activity = Path('app/src/main/kotlin/com/recapflow/ai/MainActivity.kt').read_text()

block_start = compiler.index('private fun buildEditedVideoItem(')
block_end = compiler.index('private fun buildFreezeItem(', block_start)
block = compiler[block_start:block_end]
if '.setDurationUs(range.durationMs * 1_000L)' in block:
    raise SystemExit('FAIL: encoded source item still lies to Media3 about pre-clipping duration')
if 'OverlayCompiler.projectToRange(editPlan.overlays, range)' not in block:
    raise SystemExit('FAIL: source overlay window is not projected to each selected clip')
if 'CompositionOverlayTimelinePolicy.localEffectTimeOffsetUs' not in block:
    raise SystemExit('FAIL: later sequence item offset is not removed before overlay time gating')

trim_start = activity.index('private fun onUserChangedTrim()')
trim_end = activity.index('private fun onUserChangedTransform()', trim_start)
trim_block = activity[trim_start:trim_end]
for marker in ('ensureSourceBlurRange()', 'ensureImageOverlayRange()', 'renderOverlayControls()'):
    if marker not in trim_block:
        raise SystemExit(f'FAIL: Trim change does not refresh default overlay time window: {marker}')
if 'renderCoordinator.start(' in trim_block:
    raise SystemExit('FAIL: Trim-linked overlay refresh must never start a render')
print('Phase 6F.2.6.1C static blur timeline checks: PASS')
PY2

if command -v kotlinc >/dev/null 2>&1; then
  tmpdir="$(mktemp -d)"
  cat > "$tmpdir/Smoke.kt" <<'KOT'
import com.recapflow.ai.media.edit.*
import com.recapflow.ai.media.render.CompositionOverlayTimelinePolicy

fun main() {
    val settings = OverlaySettings(
        enabled = true,
        sourceSubtitleBlur = SourceSubtitleBlurSettings(
            enabled = true,
            startMs = 0L,
            endMs = 307_000L,
        ),
    )
    val range = TrimRange(180_000L, 240_000L)
    val projected = OverlayCompiler.projectToRange(settings, range)
    val blur = requireNotNull(OverlayCompiler.compile(projected))
    check(blur.startMs == 0L && blur.endMs == 60_000L)
    val sequenceOffsetUs = 150_000_000L
    val localUs = sequenceOffsetUs + 15_000_000L +
        CompositionOverlayTimelinePolicy.localEffectTimeOffsetUs(sequenceOffsetUs)
    check(localUs == 15_000_000L)
    val speed = TransformSettings(enabled = true, speedEnabled = true, speed = 2f)
    check(CompositionOverlayTimelinePolicy.presentationDurationUs(speed, range) == 30_000_000L)
}
KOT
  kotlinc \
    app/src/main/kotlin/com/recapflow/ai/media/edit/*.kt \
    app/src/main/kotlin/com/recapflow/ai/media/render/RenderPreset.kt \
    app/src/main/kotlin/com/recapflow/ai/media/render/CompositionOverlayTimelinePolicy.kt \
    "$tmpdir/Smoke.kt" \
    -include-runtime -d "$tmpdir/smoke.jar"
  java -jar "$tmpdir/smoke.jar"
  rm -rf "$tmpdir"
fi

echo 'PASS: RecapFlowAI Phase 6F.2.6.1C source hotfix is valid.'
