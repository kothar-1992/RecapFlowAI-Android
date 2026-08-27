#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"

bash "$project_dir/scripts/verify_phase6f2_1_source.sh"

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
manifest_file="app/src/main/AndroidManifest.xml"

require_marker 'rootProject.name = "RecapFlowAI_Phase6F2_6_2"' settings.gradle.kts
require_marker 'versionName = "1.0-phase6f2.6.2"' app/build.gradle.kts
require_marker 'activityKtx = "1.10.1"' gradle/libs.versions.toml
require_marker 'implementation(libs.androidx.activity.ktx)' app/build.gradle.kts
require_marker 'import androidx.activity.result.PickVisualMediaRequest' "$activity_file"
require_marker 'ActivityResultContracts.PickVisualMedia(),' "$activity_file"
require_marker 'PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)' "$activity_file"
require_marker 'contentResolver.takePersistableUriPermission(' "$activity_file"
require_marker 'com.google.android.gms.metadata.ModuleDependencies' "$manifest_file"
require_marker 'photopicker_activity:0:required' "$manifest_file"
require_marker 'tools:ignore="MissingClass"' "$manifest_file"
require_marker 'Choose a video from your device gallery.' app/src/main/res/values/strings.xml

require_absent 'videoPicker.launch(arrayOf("video/*"))' "$activity_file"
require_absent 'android.permission.READ_MEDIA_VIDEO' "$manifest_file"

echo "PASS: RecapFlowAI Phase 6F.2.3 video Photo Picker markers are valid."
