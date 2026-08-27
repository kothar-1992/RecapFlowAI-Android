#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"

bash "$project_dir/scripts/verify_phase6f2_6_source.sh"

require_marker() {
  local marker="$1"
  local file="$2"
  if ! grep -Fq "$marker" "$file"; then
    echo "FAIL: missing '$marker' in $file" >&2
    exit 1
  fi
}

validation_file="app/src/main/kotlin/com/recapflow/ai/media/render/RenderedOutputValidation.kt"
inspector_file="app/src/main/kotlin/com/recapflow/ai/media/render/RenderedOutputInspector.kt"
coordinator_file="app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
activity_file="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
test_file="app/src/test/kotlin/com/recapflow/ai/media/render/RenderedOutputValidationPolicyTest.kt"

require_marker 'val rotationDegrees: Int = 0' "$validation_file"
require_marker 'val displayWidth: Int' "$validation_file"
require_marker 'format.rotationDegreesOrZero()' "$inspector_file"
require_marker 'MediaFormat.KEY_ROTATION' "$inspector_file"
require_marker 'outputWidth = metadata.displayWidth' "$coordinator_file"
require_marker 'previewEffectSignature' "$activity_file"
require_marker 'rebuildSourcePreviewGraph(' "$activity_file"
require_marker 'Rebuilt live preview graph without render' "$activity_file"
require_marker 'rotatedPortraitTrackUsesDisplayGeometryForExact1080pValidation' "$test_file"
require_marker 'rotatedPortraitTrackUsesDisplayShortSideWhenAspectIsOriginal' "$test_file"
require_marker 'Phase 6F.2.6.1 — Final-render geometry + non-destructive live-preview hotfix' PLAN.md

python3 - "$project_dir" <<'PY'
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
validation = (root / "app/src/main/kotlin/com/recapflow/ai/media/render/RenderedOutputValidation.kt").read_text()
activity = (root / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt").read_text()
coordinator = (root / "app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt").read_text()

if "metadata.width != expectedWidth || metadata.height != expectedHeight" in validation:
    raise SystemExit("ROTATION FLOW: coded dimensions are still used for exact display validation")
if coordinator.count("transformer?.start(") != 1:
    raise SystemExit("FINAL RENDER FLOW: Transformer must keep exactly one authoritative start path")
if "renderCoordinator.start(mediaInfo, editPlan)" not in activity:
    raise SystemExit("FINAL RENDER FLOW: editor no longer sends one current EditPlan to render coordinator")
for fn in ("onUserChangedTransform", "onUserChangedAudio", "onUserChangedOverlay", "onUserChangedTrim"):
    start = activity.index(f"private fun {fn}")
    end = activity.find("\n    private fun ", start + 1)
    block = activity[start:end if end != -1 else None]
    if "renderCoordinator.start(" in block:
        raise SystemExit(f"NON-DESTRUCTIVE EDIT FLOW: {fn} starts a render")

print("Phase 6F.2.6.1 final-render + preview hotfix checks: PASS")
PY

echo "PASS: RecapFlowAI Phase 6F.2.6.1 source hotfix is valid."
