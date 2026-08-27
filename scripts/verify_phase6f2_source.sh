#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"

bash "$project_dir/scripts/verify_phase6f1_1_source.sh"

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

require_marker 'rootProject.name = "RecapFlowAI_Phase6F2_6_2"' settings.gradle.kts
require_marker 'versionName = "1.0-phase6f2.6.2"' app/build.gradle.kts
require_marker 'class EditorPreferencesStore' app/src/main/kotlin/com/recapflow/ai/preferences/EditorPreferencesStore.kt
require_marker 'const val SCHEMA_VERSION = 2' app/src/main/kotlin/com/recapflow/ai/preferences/EditorPreferencesStore.kt
require_marker 'object EditorPreferencesPolicy' app/src/main/kotlin/com/recapflow/ai/preferences/EditorPreferences.kt
require_marker 'Source paths, imported asset paths' app/src/main/kotlin/com/recapflow/ai/preferences/EditorPreferences.kt
require_marker 'settingsAutoRestoreSwitch' app/src/main/res/layout/view_settings_destination.xml
require_marker 'settingsSavePresetButton' app/src/main/res/layout/view_settings_destination.xml
require_marker 'settingsRestorePresetButton' app/src/main/res/layout/view_settings_destination.xml
require_marker 'settingsRestoreLastSessionButton' app/src/main/res/layout/view_settings_destination.xml
require_marker 'settingsResetCurrentSectionButton' app/src/main/res/layout/view_settings_destination.xml
require_marker 'settingsResetAllButton' app/src/main/res/layout/view_settings_destination.xml
require_marker 'EditorPreferencesPolicyTest' app/src/test/kotlin/com/recapflow/ai/preferences/EditorPreferencesPolicyTest.kt

# The preference store must remain metadata-only. Source/asset/output paths, URIs, tokens and
# secrets belong to import/render lifecycles and must never enter this SharedPreferences file.
require_absent 'workingFilePath' app/src/main/kotlin/com/recapflow/ai/preferences/EditorPreferencesStore.kt
require_absent 'outputPath' app/src/main/kotlin/com/recapflow/ai/preferences/EditorPreferencesStore.kt
require_absent 'sourceUri' app/src/main/kotlin/com/recapflow/ai/preferences/EditorPreferencesStore.kt
require_absent 'apiKey' app/src/main/kotlin/com/recapflow/ai/preferences/EditorPreferencesStore.kt

echo "PASS: RecapFlowAI Phase 6F.2 preference markers remain valid in Phase 6F.2.3."
