#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"

bash "$project_dir/scripts/verify_phase6f2_3_source.sh"

require_marker() {
  local marker="$1"
  local file="$2"
  if ! grep -Fq "$marker" "$file"; then
    echo "FAIL: missing '$marker' in $file" >&2
    exit 1
  fi
}

require_absent() {
  local marker="$1"
  local file="$2"
  if grep -Fq "$marker" "$file"; then
    echo "FAIL: forbidden '$marker' found in $file" >&2
    exit 1
  fi
}

activity_file="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
session_file="app/src/main/kotlin/com/recapflow/ai/media/render/RealtimePreviewSession.kt"
geometry_file="app/src/main/kotlin/com/recapflow/ai/media/render/PreviewGeometryPolicy.kt"
state_file="app/src/main/kotlin/com/recapflow/ai/media/render/PreviewUiState.kt"
layout_file="app/src/main/res/layout/view_editor_destination.xml"
strings_file="app/src/main/res/values/strings.xml"

require_marker 'rootProject.name = "RecapFlowAI_Phase6F2_6_2"' settings.gradle.kts
require_marker 'versionName = "1.0-phase6f2.6.2"' app/build.gradle.kts
require_marker 'Local media workspace • Phase 6F.2.6' "$strings_file"
require_marker 'sealed interface PreviewUiState' "$state_file"
require_marker 'data class SourceOnly' "$state_file"
require_marker 'data class Unavailable' "$state_file"
require_marker 'private fun replacePreviewPlayer()' "$activity_file"
require_marker 'setPreviewUiState(PreviewUiState.SourceOnly(reason))' "$activity_file"
require_marker 'private fun retryLivePreviewEffects()' "$activity_file"
require_marker 'retryLivePreviewButton.setOnClickListener' "$activity_file"
require_marker 'retryLivePreviewButton' "$layout_file"
require_marker 'Retry live effects' "$strings_file"
require_marker 'MAX_SHORT_SIDE_PIXELS = 720' "$geometry_file"
require_marker 'sourceWidth = info.width' "$activity_file"
require_marker 'fun markApplying(key: PreviewGraphKey)' "$session_file"
require_marker 'fun confirmApplied()' "$session_file"
require_marker 'override fun onRenderedFirstFrame()' "$activity_file"
require_marker 'error.errorCodeName' "$activity_file"
require_marker 'lowResolutionSourceIsNotUpscaledForInteractivePreview' \
  app/src/test/kotlin/com/recapflow/ai/media/render/PreviewGeometryPolicyTest.kt
require_marker 'graphIsNotAppliedUntilAFrameConfirmsIt' \
  app/src/test/kotlin/com/recapflow/ai/media/render/RealtimePreviewSessionTest.kt

require_absent 'restartLivePreviewAfterFallback' "$activity_file"
require_absent 'Preview is unavailable, but FFmpeg metadata is ready.' "$strings_file"
require_absent 'previewFallbackActive = false' "$activity_file"
require_absent 'previewFallbackActive = true' "$activity_file"

python3 - "$project_dir" <<'PY'
import pathlib
import sys
import xml.etree.ElementTree as ET

root = pathlib.Path(sys.argv[1])
for path in (root / "app/src/main/res").rglob("*.xml"):
    ET.parse(path)

activity = (root / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt").read_text()
for function in ("onUserChangedTrim", "onUserChangedTransform", "onUserChangedOverlay"):
    start = activity.index(f"private fun {function}")
    next_function = activity.find("\n    private fun ", start + 1)
    block = activity[start:next_function if next_function >= 0 else len(activity)]
    if "retryLivePreviewEffects()" in block:
        raise SystemExit(f"PREVIEW FLOW: {function} automatically retries live effects")

effects = (root / "app/src/main/kotlin/com/recapflow/ai/media/render/TransformVideoEffects.kt").read_text()
preview_start = effects.index("fun forPreview(")
render_start = effects.index("fun forRender(", preview_start)
preview_block = effects[preview_start:render_start]
if "RenderPreset.HD_720P" in preview_block:
    raise SystemExit("PREVIEW FLOW: preview still inherits the 720p export preset")

print("Phase 6F.2.4 preview capability/fallback separation checks: PASS")
PY

echo "PASS: RecapFlowAI Phase 6F.2.4 preview workflow source is valid."
