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

bash "$project_dir/scripts/verify_phase6e1_source.sh"

main_file="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
model_file="app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlan.kt"
compiler_file="app/src/main/kotlin/com/recapflow/ai/media/edit/OverlayCompiler.kt"
validator_file="app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlanValidator.kt"
importer_file="app/src/main/kotlin/com/recapflow/ai/media/importer/ImageOverlayImportCoordinator.kt"
effect_file="app/src/main/kotlin/com/recapflow/ai/media/render/StaticImageOverlayEffect.kt"
chain_file="app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt"
render_file="app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
layout_file="app/src/main/res/layout/view_editor_destination.xml"
fragment_file="app/src/main/assets/shaders/fragment_shader_static_image_overlay_es2.glsl"

require_marker 'rootProject.name = "RecapFlowAI_Phase6E3A"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6e3a"' "app/build.gradle.kts"
require_marker 'data class ImageOverlayAsset' "$model_file"
require_marker 'data class ImageOverlaySettings' "$model_file"
require_marker 'fun compileImage(settings: OverlaySettings)' "$compiler_file"
require_marker 'fun hasOperationIntersecting' "$compiler_file"
require_marker 'IMAGE_OVERLAY_ASSET_INVALID' "$validator_file"
require_marker 'class ImageOverlayImportCoordinator' "$importer_file"
require_marker 'MAX_SOURCE_BYTES = 20L * 1024L * 1024L' "$importer_file"
require_marker 'class StaticImageOverlayEffect' "$effect_file"
require_marker 'ImageOverlayLayoutPolicy.resolve' "$effect_file"
require_marker 'MAX_BITMAP_SIDE = 2_048' "$effect_file"
require_marker 'StaticImageOverlayEffect(image, sourceTimeOffsetUs, fixedSourceTimeUs)' "$chain_file"
require_marker 'OverlayCompiler.hasOperationIntersecting(it, range)' "$render_file"
require_marker 'OverlayCompiler.hasOperationActiveAt(it, sourceFrameTimeMs)' "$render_file"
require_marker 'android:id="@+id/imageOverlayEnabledSwitch"' "$layout_file"
require_marker 'android:id="@+id/imageOverlayPositionGroup"' "$layout_file"
require_marker 'android:id="@+id/imageOverlayTimeRangeSlider"' "$layout_file"
require_marker 'imageOverlayPicker.launch(arrayOf("image/png", "image/jpeg", "image/webp"))' "$main_file"
require_marker 'editor.imageOverlayPositionGroup.clearChecked()' "$main_file"
require_marker 'SOURCE_BLUR_DIRECT_TOUCH_ENABLED = false' "$main_file"
require_marker 'texture2D(uOverlaySampler, overlayCoordinate)' "$fragment_file"
require_marker 'overlay.a * uOverlayOpacity' "$fragment_file"
require_marker 'masterOffOmitsRememberedImageOverlay' \
  "app/src/test/kotlin/com/recapflow/ai/media/edit/OverlayCompilerTest.kt"
require_marker 'landscapeLogoKeepsPixelAspectOnPortraitOutput' \
  "app/src/test/kotlin/com/recapflow/ai/media/edit/ImageOverlayLayoutPolicyTest.kt"
require_marker 'PHASE6E2_STATIC_IMAGE_OVERLAY.md' "PLAN.md"

python3 - "$project_dir" <<'PY'
import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1])
main = (root / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt").read_text()
picker_start = main.index("private val imageOverlayPicker")
picker_end = main.index("private val previewListener", picker_start)
picker = main[picker_start:picker_end]
if "setOnTouchListener" in picker or "MotionEvent" in picker:
    raise SystemExit("TOUCH SAFETY: image overlay picker installs a touch gesture")

layout = (root / "app/src/main/res/layout/view_editor_destination.xml").read_text()
image_start = layout.index('android:id="@+id/imageOverlayEnabledSwitch"')
image_section = layout[image_start:layout.index('</com.google.android.material.card.MaterialCardView>', image_start)]
if "sourceBlurRegionGuide" in image_section:
    raise SystemExit("TOUCH SAFETY: image controls reuse the source blur guide")

effect = (root / "app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt").read_text()
blur = effect.index("SourceSubtitleBlurEffect")
image = effect.index("StaticImageOverlayEffect", blur)
if image < blur:
    raise SystemExit("EFFECT ORDER: image must be composited after source blur")

fragment = (root / "app/src/main/assets/shaders/fragment_shader_static_image_overlay_es2.glsl").read_text()
if fragment.count("gl_FragColor = original") < 2:
    raise SystemExit("IMAGE SHADER: outside/time-disabled pass-through paths are missing")
if not re.search(r"mix\(original\.rgb, overlay\.rgb, alpha\)", fragment):
    raise SystemExit("IMAGE SHADER: alpha composite is missing")

print("Phase 6E.2 touch-safety/effect-order/shader checks: PASS")
PY

echo "PASS: RecapFlowAI Phase 6E.2 static image/logo overlay markers are valid."
