#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAIN="$ROOT/app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
EN="$ROOT/app/src/main/res/values/strings.xml"
MY="$ROOT/app/src/main/res/values-my/strings_core.xml"

fail() {
  echo "[Phase 6H.1F.2] FAIL: $*" >&2
  exit 1
}

require_text() {
  local file="$1"
  local text="$2"
  grep -Fq "$text" "$file" || fail "Missing '$text' in ${file#$ROOT/}"
}

for file in "$MAIN" "$EN" "$MY"; do
  [[ -f "$file" ]] || fail "Missing ${file#$ROOT/}"
done

# Canonical tracked source must contain the runtime integration itself.
require_text "$MAIN" "PHASE6H1F_TARGET_DURATION_UI"
require_text "$MAIN" "PHASE6H1F2_CLIPS_UX_UNIFICATION"
require_text "$MAIN" "bindTargetDurationClipsControls()"
require_text "$MAIN" "TargetDurationClipIntegration.generate("
require_text "$MAIN" "editor.trimRangeSlider.isVisible = false"

# Target Duration is the single normal planner. Legacy preset generation/apply remains internal only.
require_text "$MAIN" "editor.adaptivePresetGroup.isVisible = false"
require_text "$MAIN" "editor.generateAdaptiveDraftButton.isVisible = false"
require_text "$MAIN" "editor.adaptiveApplySwitch.isVisible = false"
require_text "$MAIN" "editor.adaptiveApplyNote.isVisible = false"

# Shared review must remain available.
require_text "$MAIN" "renderAdaptiveCutControls()"
require_text "$EN" '<string name="adaptive_title">Review generated clips</string>'
require_text "$EN" '<string name="adaptive_clear">Clear clip plan</string>'
require_text "$MY" '<string name="adaptive_title">ဖန်တီးထားတဲ့ ကလစ်တွေကို စစ်ဆေးပါ</string>'
require_text "$MY" '<string name="adaptive_clear">Clip plan ဖျက်မယ်</string>'

# Guard against the old contradictory user-facing copy returning.
if grep -Fq '<string name="adaptive_title">Draft pacing cuts</string>' "$EN"; then
  fail "Legacy Draft pacing cuts copy is still user-facing"
fi
if grep -Fq 'Off keeps the draft for review but renders the normal Trim.' "$EN"; then
  fail "Legacy Apply/Trim lifecycle copy is still user-facing"
fi

echo "[Phase 6H.1F.2] PASS: tracked source uses Target Duration as the normal Clips planner and shared Review downstream."
