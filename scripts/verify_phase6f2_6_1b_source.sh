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

bash scripts/verify_phase6f2_6_1a_source.sh

activity='app/src/main/kotlin/com/recapflow/ai/MainActivity.kt'
session='app/src/main/kotlin/com/recapflow/ai/media/render/RealtimePreviewSession.kt'
policy='app/src/main/kotlin/com/recapflow/ai/media/render/PreviewGeometryChangePolicy.kt'
test_file='app/src/test/kotlin/com/recapflow/ai/media/render/PreviewGeometryChangePolicyTest.kt'

require_marker 'object PreviewGeometryChangePolicy' "$policy"
require_marker 'requiresSurfaceRebind(' "$policy"
require_marker 'fun currentTransform(): TransformSettings?' "$session"
require_marker 'PREVIEW_GEOMETRY_REBIND' "$activity"
require_marker 'surface geometry change: $reason' "$activity"
require_marker 'postOnAnimation' "$activity"
require_marker 'switchingPortraitToLandscapeRequiresSurfaceRebind' "$test_file"
require_marker 'changingFitToFillRequiresSurfaceRebind' "$test_file"
require_marker 'Phase 6F.2.6.1B' PLAN.md

python3 - <<'PY2'
from pathlib import Path
activity = Path('app/src/main/kotlin/com/recapflow/ai/MainActivity.kt').read_text()
block_start = activity.index('private fun applyLiveTransformPreview(')
block_end = activity.index('private fun rebuildSourcePreviewGraph(', block_start)
block = activity[block_start:block_end]
if 'renderCoordinator.start(' in block:
    raise SystemExit('FAIL: aspect preview update must never start a render')
if 'configureSourcePreviewLayout(info) {' not in block:
    raise SystemExit('FAIL: geometry graph is not deferred until preview layout settles')
if block.index('configureSourcePreviewLayout(info) {') > block.index('rebuildSourcePreviewGraph(', block.index('geometryRebindRequired')):
    raise SystemExit('FAIL: preview graph rebuild happens before geometry layout scheduling')
print('Phase 6F.2.6.1B aspect-ratio live-preview checks: PASS')
PY2


if command -v kotlinc >/dev/null 2>&1; then
  tmpjar="$(mktemp --suffix=.jar)"
  kotlinc     app/src/main/kotlin/com/recapflow/ai/media/edit/*.kt     app/src/main/kotlin/com/recapflow/ai/media/render/RenderPreset.kt     app/src/main/kotlin/com/recapflow/ai/media/render/PreviewGeometryChangePolicy.kt     -d "$tmpjar"
  rm -f "$tmpjar"
fi

echo 'PASS: RecapFlowAI Phase 6F.2.6.1B source hotfix is valid.'
