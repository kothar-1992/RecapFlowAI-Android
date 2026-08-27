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

bash scripts/verify_phase6f2_6_1_source.sh

require_marker 'object PausedPreviewRefreshPolicy' app/src/main/kotlin/com/recapflow/ai/media/render/PausedPreviewRefreshPolicy.kt
require_marker 'Two source frames is large enough' app/src/main/kotlin/com/recapflow/ai/media/render/PausedPreviewRefreshPolicy.kt
require_marker 'PausedPreviewRefreshPolicy.refreshTargetMs(' app/src/main/kotlin/com/recapflow/ai/MainActivity.kt
require_marker 'settlePausedPreviewFrameRefresh' app/src/main/kotlin/com/recapflow/ai/MainActivity.kt
require_marker 'PAUSED_PREVIEW_REFRESH_SETTLE_MS = 360L' app/src/main/kotlin/com/recapflow/ai/MainActivity.kt
require_marker 'preview-only invalidation: no graph rebuild and no render' app/src/main/kotlin/com/recapflow/ai/MainActivity.kt
require_marker 'class PausedPreviewRefreshPolicyTest' app/src/test/kotlin/com/recapflow/ai/media/render/PausedPreviewRefreshPolicyTest.kt

if grep -Fq 'previewPlayer.seekTo(previewPlayer.currentPosition.coerceAtLeast(0L))' app/src/main/kotlin/com/recapflow/ai/MainActivity.kt; then
  echo 'FAIL: old same-position paused-frame seek is still present' >&2
  exit 1
fi

if command -v kotlinc >/dev/null 2>&1; then
  tmpjar="$(mktemp --suffix=.jar)"
  kotlinc app/src/main/kotlin/com/recapflow/ai/media/render/PausedPreviewRefreshPolicy.kt -d "$tmpjar"
  rm -f "$tmpjar"
fi

echo 'Phase 6F.2.6.1A paused logo/blur live-refresh checks: PASS'
