#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

MAIN="app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
PREFS="app/src/main/kotlin/com/recapflow/ai/preferences/EditorPreferences.kt"
STORE="app/src/main/kotlin/com/recapflow/ai/preferences/EditorPreferencesStore.kt"
CONTROLLER="app/src/main/kotlin/com/recapflow/ai/ui/ImageOverlayAnimationController.kt"
LAYOUT="app/src/main/res/layout/view_image_overlay_animation_controls.xml"
EN="app/src/main/res/values/strings_phase6h2.xml"
MY="app/src/main/res/values-my/strings_phase6h2.xml"
CORE_EN="app/src/main/res/values/strings.xml"
CORE_MY="app/src/main/res/values-my/strings_overlay_adaptive.xml"
TEST="app/src/test/kotlin/com/recapflow/ai/preferences/ImageOverlayAnimationPreferencePolicyTest.kt"

for path in "$MAIN" "$PREFS" "$STORE" "$CONTROLLER" "$LAYOUT" "$EN" "$MY" "$CORE_EN" "$CORE_MY" "$TEST"; do
  [[ -f "$path" ]] || { echo "FAIL: missing $path" >&2; exit 1; }
done

grep -q 'PHASE6H2_ANIMATION_UI' "$MAIN" || {
  echo "FAIL: canonical MainActivity animation UI marker missing; run apply_phase6h2_animation_ui.py" >&2
  exit 1
}
grep -q 'ImageOverlayAnimationController' "$MAIN" || {
  echo "FAIL: MainActivity animation controller binding missing" >&2; exit 1;
}
grep -q 'animation = imageOverlayAnimation' "$MAIN" || {
  echo "FAIL: EditPlan image overlay does not carry animation state" >&2; exit 1;
}
grep -q 'imageAnimationPreset = imageOverlayAnimation.preset' "$MAIN" || {
  echo "FAIL: editor preference snapshot does not persist animation preset" >&2; exit 1;
}
grep -q 'preset = snapshot.overlay.imageAnimationPreset' "$MAIN" || {
  echo "FAIL: editor preference restore does not restore animation preset" >&2; exit 1;
}
grep -q 'KEY_IMAGE_OVERLAY_ANIMATION_DURATION_MS' "$MAIN" || {
  echo "FAIL: Activity saved-state animation duration key missing" >&2; exit 1;
}

grep -q 'val imageAnimationPreset: ImageOverlayAnimationPreset' "$PREFS" || {
  echo "FAIL: OverlayPreference animation preset missing" >&2; exit 1;
}
grep -q 'imageAnimationPeriodMs' "$PREFS" || {
  echo "FAIL: OverlayPreference animation period missing" >&2; exit 1;
}
grep -q 'const val SCHEMA_VERSION = 4' "$STORE" || {
  echo "FAIL: editor preference schema was not advanced to v4" >&2; exit 1;
}
grep -q 'overlay.image.animation.preset' "$STORE" || {
  echo "FAIL: animation preset persistence key missing" >&2; exit 1;
}
grep -q 'overlay.image.animation.period' "$STORE" || {
  echo "FAIL: animation period persistence key missing" >&2; exit 1;
}

grep -q 'imageOverlayAnimationPresetDropdown' "$LAYOUT" || {
  echo "FAIL: preset dropdown missing from animation child layout" >&2; exit 1;
}
grep -q 'imageOverlayAnimationLoopSwitch' "$LAYOUT" || {
  echo "FAIL: loop switch missing from animation child layout" >&2; exit 1;
}
grep -q 'imageOverlayAnimationDurationSlider' "$LAYOUT" || {
  echo "FAIL: duration slider missing from animation child layout" >&2; exit 1;
}
grep -q 'imageOverlayAnimationPeriodSlider' "$LAYOUT" || {
  echo "FAIL: loop period slider missing from animation child layout" >&2; exit 1;
}

grep -q 'ImageOverlayAnimationPreset.ROTATE' "$CONTROLLER" || {
  echo "FAIL: animation preset controller coverage incomplete" >&2; exit 1;
}
grep -q 'phaseOffsetMs = 0L' "$CONTROLLER" || {
  echo "FAIL: editor controller must leave projected phase offset compiler-owned" >&2; exit 1;
}

grep -q 'image_overlay_animation_preset_fade_scale' "$EN" || {
  echo "FAIL: English animation copy missing" >&2; exit 1;
}
grep -q 'image_overlay_animation_preset_fade_scale' "$MY" || {
  echo "FAIL: Myanmar animation copy missing" >&2; exit 1;
}
if grep -q '[၀၁၂၃၄၅၆၇၈၉]' "$MY"; then
  echo "FAIL: Myanmar phase strings must keep Arabic digits 0-9" >&2
  exit 1
fi
if grep -q 'animation are deferred' "$CORE_EN"; then
  echo "FAIL: stale English deferred-animation copy remains" >&2
  exit 1
fi
if grep -q 'animation.*deferred' "$CORE_MY"; then
  echo "FAIL: stale Myanmar deferred-animation copy remains" >&2
  exit 1
fi

grep -q 'oldPreferenceDefaultsRemainStatic' "$TEST" || {
  echo "FAIL: backward-compatible static preference test missing" >&2; exit 1;
}

echo "PASS: Phase 6H.2 animated-logo UI + persistence source contract is present."
