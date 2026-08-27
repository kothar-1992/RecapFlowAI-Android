#!/usr/bin/env bash
set -euo pipefail

project_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

require_marker() {
  local marker="$1"
  local relative_path="$2"
  if ! grep -Fq "$marker" "$project_dir/$relative_path"; then
    echo "OLD OR WRONG SOURCE: $relative_path lacks $marker" >&2
    exit 1
  fi
}

main_file="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
layout_file="app/src/main/res/layout/view_editor_destination.xml"
model_file="app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlan.kt"
compiler_file="app/src/main/kotlin/com/recapflow/ai/media/edit/OverlayCompiler.kt"
effect_file="app/src/main/kotlin/com/recapflow/ai/media/render/SourceSubtitleBlurEffect.kt"
effect_chain_file="app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt"
preview_policy_file="app/src/main/kotlin/com/recapflow/ai/media/edit/PreviewAspectPolicy.kt"
preview_policy_test_file="app/src/test/kotlin/com/recapflow/ai/media/edit/PreviewAspectPolicyTest.kt"
render_file="app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
validator_file="app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlanValidator.kt"
vertex_file="app/src/main/assets/shaders/vertex_shader_source_subtitle_blur_es2.glsl"
fragment_file="app/src/main/assets/shaders/fragment_shader_source_subtitle_blur_es2.glsl"

require_marker 'rootProject.name = "RecapFlowAI_Phase6E3A"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6e3a"' "app/build.gradle.kts"
require_marker 'android:id="@+id/reviewOverlayTabButton"' "$layout_file"
require_marker 'android:id="@+id/overlayCard"' "$layout_file"
require_marker 'android:id="@+id/sourceBlurRegionGuide"' "$layout_file"
require_marker 'ReviewEditorTab.OVERLAY -> R.id.reviewOverlayTabButton' "$main_file"
require_marker 'sourceSubtitleBlur: SourceSubtitleBlurSettings' "$model_file"
require_marker 'if (!settings.enabled || !blur.enabled) return null' "$compiler_file"
require_marker 'class SourceSubtitleBlurEffect' "$effect_file"
require_marker 'GlProgram(' "$effect_file"
require_marker 'VERTEX_SHADER_ASSET_PATH' "$effect_file"
require_marker 'FRAGMENT_SHADER_ASSET_PATH' "$effect_file"
require_marker 'attribute vec4 aFramePosition' "$vertex_file"
require_marker 'SourceSubtitleBlurEffect(blur, sourceTimeOffsetUs, fixedSourceTimeUs)' \
  "$effect_chain_file"
require_marker 'sourceTimeOffsetUs = range.startMs * 1_000L' "$render_file"
require_marker 'fixedSourceTimeUs = sourceFrameTimeMs * 1_000L' "$render_file"
require_marker 'SOURCE_BLUR_RECTANGLE_INVALID' "$validator_file"
require_marker 'masterOffOmitsRememberedSourceBlur' \
  "app/src/test/kotlin/com/recapflow/ai/media/edit/OverlayCompilerTest.kt"
require_marker 'enabledManualSourceBlurRejectsInvalidGeometryStrengthAndTime' \
  "app/src/test/kotlin/com/recapflow/ai/media/edit/EditPlanValidatorTest.kt"
require_marker 'float blurMix = smoothstep' "$fragment_file"
require_marker 'SOURCE_BLUR_PREVIEW_UPDATE_MS = 140L' "$main_file"
require_marker 'sourceBlurPreviewHandler.postDelayed' "$main_file"
require_marker 'TOUCH_GESTURE_VISUAL_ONLY' "$main_file"
require_marker 'guide.translationX =' "$main_file"
require_marker 'scaleX = resizePendingRectangle.width / resizeStartRectangle.width' "$main_file"
require_marker '@SuppressLint("ClickableViewAccessibility")' "$main_file"
require_marker 'scheduleSourceBlurGestureCommit(' "$main_file"
require_marker 'RELEASE_COMMIT_AFTER_TOUCH_DISPATCH' "$main_file"
require_marker 'editor.previewCard.postOnAnimation(runnable)' "$main_file"
require_marker 'sourceBlurGestureCommitRunnable: Runnable?' "$main_file"
require_marker 'cancelSourceBlurGestureCommit(resetGuide = true)' "$main_file"
require_marker 'throttlePreview = true' "$main_file"
require_marker '"Deferred blur gesture commit failed: $reason"' "$main_file"
require_marker '"Blur guide drag failed; action=${event.actionMasked}"' "$main_file"
require_marker '"Blur guide resize failed; action=${event.actionMasked}"' "$main_file"
require_marker 'throttleSourceBlurPreview = true' "$main_file"
require_marker 'as? FrameLayout.LayoutParams' "$main_file"
require_marker 'TAG_PREVIEW = "RecapFlowPreview"' "$main_file"
require_marker 'TAG_SOURCE_BLUR = "RecapFlowBlur"' "$main_file"
require_marker 'cancelSourceBlurPreviewUpdate(clearDirty = true)' "$main_file"
require_marker 'SOURCE_BLUR_DIRECT_TOUCH_ENABLED = false' "$main_file"
require_marker 'TEMPORARY_SAFETY_ROLLBACK' "$main_file"
require_marker 'setOnTouchListener(null)' "$main_file"
require_marker 'PREVIEW_TEXTURE_BOUNDS_RECOVERY' "$main_file"
require_marker 'PREVIEW_SINGLE_ASPECT_OWNER' "$main_file"
require_marker 'PreviewAspectPolicy.resolve(' "$main_file"
require_marker 'PreviewAspectOwner.VIDEO_EFFECTS -> AspectRatioFrameLayout.RESIZE_MODE_FILL' \
  "$main_file"
require_marker 'enum class PreviewAspectOwner' "$preview_policy_file"
require_marker 'liveEffectsAvailable: Boolean' "$preview_policy_file"
require_marker 'TransformCompiler.compile(settings, preset) != null' "$preview_policy_file"
require_marker 'portraitToLandscapeFitLetsPresentationOwnAspect' "$preview_policy_test_file"
require_marker 'previewFallbackReturnsAspectOwnershipToPlayerView' "$preview_policy_test_file"
require_marker 'refreshVideoPreviewSurfaceGeometry()' "$main_file"
require_marker 'app:surface_type="texture_view"' "$layout_file"
require_marker 'android:clipToOutline="true"' "$layout_file"
require_marker 'source_blur_region_slider_description' "$layout_file"
require_marker 'PHASE6E1_5_FIT_PREVIEW_ASPECT_PARITY.md' "PLAN.md"

python3 - "$project_dir" <<'PY'
import collections
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

root = pathlib.Path(sys.argv[1])
res = root / "app/src/main/res"
android_id = "{http://schemas.android.com/apk/res/android}id"
layout_ids = set()

for xml in res.rglob("*.xml"):
    tree = ET.parse(xml)
    if xml.parent.name.startswith("layout"):
        ids = []
        for node in tree.iter():
            value = node.attrib.get(android_id, "")
            if value.startswith("@+id/"):
                identifier = value.removeprefix("@+id/")
                ids.append(identifier)
                layout_ids.add(identifier)
        duplicates = [name for name, count in collections.Counter(ids).items() if count > 1]
        if duplicates:
            raise SystemExit(f"DUPLICATE IDS in {xml}: {duplicates}")

definitions = {
    "string": set(),
    "color": set(),
    "dimen": set(),
    "drawable": {p.stem for p in res.glob("drawable*/*")},
    "raw": {p.stem for p in res.glob("raw/*")},
}
for values_dir in res.glob("values*"):
    for xml in values_dir.glob("*.xml"):
        for node in ET.parse(xml).getroot():
            name = node.attrib.get("name")
            if name and node.tag in definitions:
                definitions[node.tag].add(name)

ref_pattern = re.compile(r"@(string|color|dimen|drawable|raw)/([A-Za-z0-9_]+)")
for xml in res.rglob("*.xml"):
    for kind, name in ref_pattern.findall(xml.read_text(encoding="utf-8")):
        if name not in definitions[kind]:
            raise SystemExit(f"MISSING RESOURCE: @{kind}/{name} referenced by {xml}")

main_source = (root / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt").read_text(
    encoding="utf-8"
)
binding_properties = set(re.findall(r"\beditor\.([A-Za-z_][A-Za-z0-9_]*)", main_source))
missing_bindings = sorted(binding_properties - layout_ids - {"root"})
if missing_bindings:
    raise SystemExit(f"MISSING EDITOR VIEWBINDING IDS: {missing_bindings}")

fragment = (
    root / "app/src/main/assets/shaders/fragment_shader_source_subtitle_blur_es2.glsl"
).read_text()
if fragment.count("texture2D") < 2:
    raise SystemExit("BLUR SHADER: expected original and dense-kernel sampling")
if "DENSE_9X9_NORMALIZED_KERNEL" not in fragment:
    raise SystemExit("BLUR SHADER: dense normalized kernel marker is missing")
if "for (int sampleY = -4; sampleY <= 4; sampleY++)" not in fragment:
    raise SystemExit("BLUR SHADER: dense vertical sampling loop is missing")
if "for (int sampleX = -4; sampleX <= 4; sampleX++)" not in fragment:
    raise SystemExit("BLUR SHADER: dense horizontal sampling loop is missing")
if "blurred *= 1.0 / 81.0" not in fragment:
    raise SystemExit("BLUR SHADER: dense kernel is not normalized")
if "REGION_CLAMP_PREVENTS_TILE_GHOSTS" not in fragment:
    raise SystemExit("BLUR SHADER: region-clamped sampling marker is missing")
if "gl_FragColor = original" not in fragment:
    raise SystemExit("BLUR SHADER: outside-region unchanged path is missing")

print("XML/resources/ViewBinding/GLSL markers: PASS")
PY

if grep -Fq 'R.raw.' "$project_dir/$effect_file"; then
  echo "MEDIA3 1.8 API REGRESSION: GlProgram must load shader asset paths, not R.raw IDs" >&2
  exit 1
fi

python3 - "$project_dir" <<'PY'
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
pairs = {')': '(', ']': '[', '}': '{'}
openers = set(pairs.values())

for source in (root / "app/src").rglob("*.kt"):
    text = source.read_text(encoding="utf-8")
    stack = []
    index = 0
    state = "code"
    while index < len(text):
        two = text[index:index + 2]
        three = text[index:index + 3]
        char = text[index]
        if state == "code":
            if three == '\"\"\"':
                state = "triple"; index += 3; continue
            if two == "//":
                state = "line_comment"; index += 2; continue
            if two == "/*":
                state = "block_comment"; index += 2; continue
            if char == '"': state = "string"
            elif char == "'": state = "char"
            elif char in openers: stack.append(char)
            elif char in pairs:
                if not stack or stack.pop() != pairs[char]:
                    raise SystemExit(f"UNBALANCED KOTLIN DELIMITER in {source} near {index}")
        elif state == "line_comment":
            if char == "\n": state = "code"
        elif state == "block_comment":
            if two == "*/": state = "code"; index += 2; continue
        elif state == "triple":
            if three == '\"\"\"': state = "code"; index += 3; continue
        elif char == "\\":
            index += 2; continue
        elif (state == "string" and char == '"') or (state == "char" and char == "'"):
            state = "code"
        index += 1
    if stack or state in {"block_comment", "string", "char", "triple"}:
        raise SystemExit(f"UNTERMINATED KOTLIN STRUCTURE in {source}")

print("Kotlin delimiter scan: PASS")
PY

python3 - "$project_dir" <<'PY'
import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1])
source = (root / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt").read_text(
    encoding="utf-8"
)
start = source.index("private fun bindSourceBlurGuideGestures()")
end = source.index("private fun resetSourceBlurGesturePreview()", start)
gesture = source[start:end]
move_blocks = re.findall(
    r"MotionEvent\.ACTION_MOVE\s*->\s*\{(.*?)MotionEvent\.ACTION_UP",
    gesture,
    flags=re.DOTALL,
)
if len(move_blocks) != 2:
    raise SystemExit(f"TOUCH ISOLATION: expected 2 ACTION_MOVE blocks, found {len(move_blocks)}")
for index, block in enumerate(move_blocks, start=1):
    forbidden = (
        "updateSourceBlurRectangle(",
        "renderSourceBlurGeometryControls(",
        "applySourceBlurGuideLayout(",
        "commitSourceBlurPreviewUpdate(",
        "onUserChangedOverlay(",
        ".layoutParams",
    )
    found = [marker for marker in forbidden if marker in block]
    if found:
        raise SystemExit(
            f"TOUCH ISOLATION: ACTION_MOVE block {index} mutates committed state: {found}"
        )

release_blocks = re.findall(
    r"MotionEvent\.ACTION_UP,\s*MotionEvent\.ACTION_CANCEL\s*->\s*\{(.*?)\n\s*true\n\s*\}",
    gesture,
    flags=re.DOTALL,
)
if len(release_blocks) != 2:
    raise SystemExit(f"RELEASE DEFERRAL: expected 2 release blocks, found {len(release_blocks)}")
for index, block in enumerate(release_blocks, start=1):
    if "scheduleSourceBlurGestureCommit(" not in block:
        raise SystemExit(f"RELEASE DEFERRAL: release block {index} does not schedule commit")
    forbidden = (
        "resetSourceBlurGesturePreview(",
        "updateSourceBlurRectangle(",
        "renderSourceBlurGeometryControls(",
        "applySourceBlurGuideLayout(",
        "commitSourceBlurPreviewUpdate(",
        "onUserChangedOverlay(",
        "performClick(",
        ".layoutParams",
    )
    found = [marker for marker in forbidden if marker in block]
    if found:
        raise SystemExit(
            f"RELEASE DEFERRAL: release block {index} performs synchronous work: {found}"
        )
if "performClick(" in gesture:
    raise SystemExit("RELEASE DEFERRAL: blur drag surfaces must not call performClick")

print("Direct-touch move/release isolation markers: PASS")
PY

python3 - "$project_dir" <<'PY'
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
source = (root / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt").read_text(
    encoding="utf-8"
)
start = source.index("private fun bindSourceBlurGuideGestures()")
end = source.index("private fun resetSourceBlurGesturePreview()", start)
gesture = source[start:end]
gate = gesture.index("if (!SOURCE_BLUR_DIRECT_TOUCH_ENABLED)")
first_active_listener = gesture.index("setOnTouchListener {", gate)
early_return = gesture.index("return", gate)
if not gate < early_return < first_active_listener:
    raise SystemExit("TOUCH ROLLBACK: active blur listener is reachable before safety return")
if "SOURCE_BLUR_DIRECT_TOUCH_ENABLED = false" not in source:
    raise SystemExit("TOUCH ROLLBACK: direct-touch flag is not default false")

layout = (root / "app/src/main/res/layout/view_editor_destination.xml").read_text(
    encoding="utf-8"
)
if 'app:surface_type="texture_view"' not in layout:
    raise SystemExit("PREVIEW BOUNDS: movable PlayerView must use texture_view")
if 'android:visibility="gone"' not in layout[layout.index('sourceBlurRegionResizeHandle'):]:
    raise SystemExit("TOUCH ROLLBACK: blur resize handle must start hidden")

print("Touch rollback and embedded preview-surface markers: PASS")
PY

while IFS= read -r script; do
  bash -n "$script"
done < <(find "$project_dir/scripts" -maxdepth 1 -type f -name '*.sh' -print)

if grep -RInE '(AIza[0-9A-Za-z_-]{20,}|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|api[_-]?key[[:space:]]*=[[:space:]]*["'"'][^"'"']+["'"'])' \
  "$project_dir/app/src" "$project_dir/docs" --exclude-dir=build; then
  echo "POSSIBLE SECRET found in delivery source" >&2
  exit 1
fi

echo "PASS: RecapFlowAI Phase 6E.1 baseline markers remain valid in the current source."
