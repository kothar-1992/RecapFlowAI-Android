#!/usr/bin/env bash
set -euo pipefail

project_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

require_marker() {
  local marker="$1"
  local relative_path="$2"
  if ! grep -Fq "$marker" "$project_dir/$relative_path"; then
    echo "OLD OR INCOMPLETE SOURCE: $relative_path lacks $marker" >&2
    exit 1
  fi
}

bash "$project_dir/scripts/verify_phase6e2_source.sh"

main_file="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
state_file="app/src/main/kotlin/com/recapflow/ai/media/render/RealtimeImageOverlayState.kt"
effect_file="app/src/main/kotlin/com/recapflow/ai/media/render/StaticImageOverlayEffect.kt"
chain_file="app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt"
test_file="app/src/test/kotlin/com/recapflow/ai/media/render/RealtimeImageOverlayStateTest.kt"

require_marker 'rootProject.name = "RecapFlowAI_Phase6E3A"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6e3a"' "app/build.gradle.kts"
require_marker 'class RealtimeImageOverlayState' "$state_file"
require_marker '@Volatile' "$state_file"
require_marker 'snapshotFor(workingFilePath: String)' "$state_file"
require_marker 'PREVIEW_LOGO_LIVE_STATE' "$effect_file"
require_marker '?.snapshotFor(image.asset.workingFilePath)' "$effect_file"
require_marker 'ImageOverlayLayoutPolicy.resolve(' "$effect_file"
require_marker 'realtimeImageOverlayState?.update(compiledImage)' "$chain_file"
require_marker 'realtimeState = realtimeImageOverlayState' "$chain_file"
require_marker 'realtimeImageOverlayState.update(OverlayCompiler.compileImage(currentOverlaySettings()))' "$main_file"
require_marker 'latestMatchingAssetSnapshotReplacesStaleGeometry' "$test_file"
require_marker 'disabledOrReplacedAssetCannotLeakThroughOldShader' "$test_file"
require_marker 'PHASE6E2_1_REALTIME_LOGO_CONTROLS.md' "PLAN.md"
require_marker 'SOURCE_BLUR_DIRECT_TOUCH_ENABLED = false' "$main_file"

python3 - "$project_dir" <<'PY'
import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1])
main = (root / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt").read_text()
chain = (root / "app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt").read_text()
effect = (root / "app/src/main/kotlin/com/recapflow/ai/media/render/StaticImageOverlayEffect.kt").read_text()

preview_calls = list(re.finditer(r"TransformVideoEffects\.forPreview\(", main))
state_arguments = main.count("realtimeImageOverlayState = realtimeImageOverlayState")
if not preview_calls or state_arguments != len(preview_calls):
    raise SystemExit(
        f"LIVE LOGO STATE: expected state on {len(preview_calls)} preview calls, found {state_arguments}"
    )

render_start = chain.index("fun forRender(")
render_end = chain.index("private fun buildVisualEffects", render_start)
if "RealtimeImageOverlayState" in chain[render_start:render_end]:
    raise SystemExit("EXPORT IMMUTABILITY: forRender must not accept realtime mutable state")

draw_start = effect.index("override fun drawFrame")
draw_end = effect.index("override fun release", draw_start)
draw = effect[draw_start:draw_end]
if draw.index("snapshotFor") > draw.index("ImageOverlayLayoutPolicy.resolve"):
    raise SystemExit("LIVE LOGO STATE: snapshot must be resolved before per-frame geometry")

image_control_start = main.index("editor.imageOverlayEnabledSwitch.setOnCheckedChangeListener")
image_control_end = main.index("bindSourceBlurGuideGestures()", image_control_start)
image_controls = main[image_control_start:image_control_end]
if "setOnTouchListener" in image_controls or "MotionEvent" in image_controls:
    raise SystemExit("TOUCH SAFETY: Phase 6E.2.1 must remain slider/preset only")

print("Phase 6E.2.1 live-state/export-immutability/touch-safety checks: PASS")
PY

echo "PASS: RecapFlowAI Phase 6E.2.1 realtime logo control markers are valid."
