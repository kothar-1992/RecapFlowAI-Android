#!/usr/bin/env bash
set -euo pipefail

project_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

require_marker() {
  local marker="$1"
  local relative_path="$2"
  if ! grep -Fq "$marker" "$project_dir/$relative_path"; then
    echo "WRONG BASELINE: $relative_path lacks $marker" >&2
    exit 1
  fi
}

reject_marker() {
  local marker="$1"
  local relative_path="$2"
  if grep -Fq "$marker" "$project_dir/$relative_path"; then
    echo "STALE BASELINE: $relative_path still contains $marker" >&2
    exit 1
  fi
}

bash "$project_dir/scripts/verify_phase6e2_1_source.sh"

require_marker 'rootProject.name = "RecapFlowAI_Phase6E3A"' "settings.gradle.kts"
require_marker 'minSdk = 28' "app/build.gradle.kts"
require_marker 'compileSdk = 36' "app/build.gradle.kts"
require_marker 'targetSdk = 34' "app/build.gradle.kts"
require_marker 'versionName = "1.0-phase6e3a"' "app/build.gradle.kts"
require_marker 'agp = "8.13.0"' "gradle/libs.versions.toml"
require_marker 'kotlin = "2.1.0"' "gradle/libs.versions.toml"
require_marker 'coreKtx = "1.16.0"' "gradle/libs.versions.toml"
require_marker 'appcompat = "1.7.1"' "gradle/libs.versions.toml"
require_marker 'material = "1.13.0"' "gradle/libs.versions.toml"
require_marker 'constraintlayout = "2.1.4"' "gradle/libs.versions.toml"
require_marker 'media3 = "1.10.0"' "gradle/libs.versions.toml"
require_marker 'Build.VERSION.SDK_INT == Build.VERSION_CODES.P' \
  "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
require_marker 'SOURCE_BLUR_DIRECT_TOUCH_ENABLED = false' \
  "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
require_marker 'PHASE6E3A_PLATFORM_MEDIA3_BASELINE.md' "PLAN.md"
require_marker 'Media3 `1.10.0`' "THIRD_PARTY_NOTICES.md"

reject_marker 'media3 = "1.8.0"' "gradle/libs.versions.toml"
reject_marker 'minSdk = 21' "app/build.gradle.kts"
reject_marker 'Build.VERSION.SDK_INT < Build.VERSION_CODES.M' \
  "app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"

echo "PASS: Phase 6E.3A platform and Media3 baseline markers are valid."
