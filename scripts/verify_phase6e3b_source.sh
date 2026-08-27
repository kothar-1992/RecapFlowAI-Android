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

reject_marker() {
  local marker="$1"
  local relative_path="$2"
  if grep -Fq "$marker" "$project_dir/$relative_path"; then
    echo "STALE OR UNSAFE SOURCE: $relative_path contains $marker" >&2
    exit 1
  fi
}

main_file="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
session_file="app/src/main/kotlin/com/recapflow/ai/media/render/RealtimePreviewSession.kt"
blur_state_file="app/src/main/kotlin/com/recapflow/ai/media/render/RealtimeSourceBlurState.kt"
blur_effect_file="app/src/main/kotlin/com/recapflow/ai/media/render/SourceSubtitleBlurEffect.kt"
chain_file="app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt"
layout_file="app/src/main/res/layout/view_editor_destination.xml"

require_marker 'rootProject.name = "RecapFlowAI_Phase6F2_6_2"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6f2.6.2"' "app/build.gradle.kts"
require_marker 'minSdk = 28' "app/build.gradle.kts"
require_marker 'media3 = "1.10.0"' "gradle/libs.versions.toml"
require_marker 'class RealtimePreviewSession' "$session_file"
require_marker 'fun request(key: PreviewGraphKey, reason: String)' "$session_file"
require_marker 'fun claimRecovery(path: String, expectedGeneration: Long)' "$session_file"
require_marker 'class RealtimeSourceBlurState' "$blur_state_file"
require_marker '@Volatile' "$blur_state_file"
require_marker 'PREVIEW_BLUR_LIVE_STATE' "$blur_effect_file"
require_marker 'realtimeState?.snapshot()' "$blur_effect_file"
require_marker 'realtimeSourceBlurState?.update(compiledBlur)' "$chain_file"
require_marker 'realtimeState = realtimeSourceBlurState' "$chain_file"
require_marker 'PREVIEW_READY_TIMEOUT_MS = 10_000L' "$main_file"
require_marker 'requestSourceBlurPreviewUpdate("transform controls", immediate = false)' "$main_file"
require_marker 'requestSourceBlurPreviewUpdate("trim range", immediate = false)' "$main_file"
require_marker 'keep_content_on_player_reset="true"' "$layout_file"
require_marker 'SOURCE_BLUR_DIRECT_TOUCH_ENABLED = false' "$main_file"
require_marker 'rapidRequestsCoalesceToLatestGraph' \
  "app/src/test/kotlin/com/recapflow/ai/media/render/RealtimePreviewSessionTest.kt"
require_marker 'disabledBlurCannotLeakThroughRetainedShader' \
  "app/src/test/kotlin/com/recapflow/ai/media/render/RealtimeSourceBlurStateTest.kt"
require_marker 'PHASE6E3B_REALTIME_PREVIEW_SESSION.md' "PLAN.md"

reject_marker 'media3 = "1.8.0"' "gradle/libs.versions.toml"
reject_marker 'minSdk = 21' "app/build.gradle.kts"

python3 - "$project_dir" <<'PY'
import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1])
main = (root / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt").read_text()
chain = (root / "app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt").read_text()
blur_effect = (root / "app/src/main/kotlin/com/recapflow/ai/media/render/SourceSubtitleBlurEffect.kt").read_text()

preview_calls = len(re.findall(r"TransformVideoEffects\.forPreview\(", main))
blur_state_args = main.count("realtimeSourceBlurState = realtimeSourceBlurState")
image_state_args = main.count("realtimeImageOverlayState = realtimeImageOverlayState")
if preview_calls == 0 or blur_state_args != preview_calls or image_state_args != preview_calls:
    raise SystemExit(
        "PREVIEW STATE: every preview graph must receive both realtime bridges "
        f"(calls={preview_calls}, blur={blur_state_args}, image={image_state_args})"
    )

render_start = chain.index("fun forRender(")
render_end = chain.index("private fun buildVisualEffects", render_start)
render_section = chain[render_start:render_end]
if "RealtimeSourceBlurState" in render_section or "RealtimeImageOverlayState" in render_section:
    raise SystemExit("EXPORT IMMUTABILITY: forRender accepts mutable preview state")
if "SourceSubtitleBlurEffect(blur, sourceTimeOffsetUs, fixedSourceTimeUs)" not in render_section:
    raise SystemExit("EXPORT PARITY: immutable source blur effect changed")

draw_start = blur_effect.index("override fun drawFrame")
draw_end = blur_effect.index("override fun release", draw_start)
draw = blur_effect[draw_start:draw_end]
if draw.index("realtimeState?.snapshot()") > draw.index('setFloatUniform("uBlurLeft"'):
    raise SystemExit("BLUR LIVE STATE: snapshot must precede per-frame uniforms")

def function_section(start_marker: str, end_marker: str) -> str:
    start = main.index(start_marker)
    end = main.index(end_marker, start)
    return main[start:end]

for name, section in {
    "overlay": function_section("private fun onUserChangedOverlay(", "private fun scheduleSourceBlurPreviewUpdate"),
    "trim": function_section("private fun onUserChangedTrim()", "private fun onUserChangedTransform()"),
    "transform": function_section("private fun onUserChangedTransform()", "private fun previewIntroFreeze()"),
}.items():
    if "setMediaItem(" in section or ".prepare()" in section or "setVideoEffects(" in section:
        raise SystemExit(f"RETAINED SESSION: {name} control directly reloads/replaces the player")

print("Phase 6E.3B retained-session/live-state/export-immutability checks: PASS")
PY

echo "PASS: Phase 6E.3B preview regression markers remain valid in Phase 6F.2.3."
