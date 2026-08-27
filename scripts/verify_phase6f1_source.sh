#!/usr/bin/env bash
set -euo pipefail

project_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

bash "$project_dir/scripts/verify_phase6e3b_source.sh"

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
export_file="app/src/main/kotlin/com/recapflow/ai/media/export/PublicExportCoordinator.kt"
state_file="app/src/main/kotlin/com/recapflow/ai/media/export/PublicExportUiState.kt"
render_file="app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
manifest_file="app/src/main/AndroidManifest.xml"
layout_file="app/src/main/res/layout/view_editor_destination.xml"

require_marker 'rootProject.name = "RecapFlowAI_Phase6F2_6_2"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6f2.6.2"' "app/build.gradle.kts"
require_marker 'minSdk = 28' "app/build.gradle.kts"
require_marker 'media3 = "1.10.0"' "gradle/libs.versions.toml"
require_marker 'class PublicExportCoordinator' "$export_file"
require_marker 'MediaStore.Video.Media.IS_PENDING, 1' "$export_file"
require_marker 'MediaStore.Video.Media.IS_PENDING, 0' "$export_file"
require_marker 'MediaStore.Video.Media.RELATIVE_PATH' "$export_file"
require_marker 'copyCancellable(input, output, source.length())' "$export_file"
require_marker 'resolver.delete(uri, null, null)' "$export_file"
require_marker 'pending.renameTo(target)' "$export_file"
require_marker 'Manifest.permission.WRITE_EXTERNAL_STORAGE' "$main_file"
require_marker 'android:maxSdkVersion="28"' "$manifest_file"
require_marker 'androidx.core.content.FileProvider' "$manifest_file"
require_marker 'data class Published' "$state_file"
require_marker 'ReviewEditorTab.EXPORT' "$main_file"
require_marker 'reviewExportTabButton' "$layout_file"
require_marker 'exportCard' "$layout_file"
require_marker 'getExternalFilesDir(Environment.DIRECTORY_MOVIES)' "$render_file"
require_marker 'normalizesUnsafeNameAndKeepsMp4Extension' \
  "app/src/test/kotlin/com/recapflow/ai/media/export/PublicExportNamePolicyTest.kt"
require_marker 'PHASE6F1_PUBLIC_MEDIASTORE_EXPORT.md' "PLAN.md"

reject_marker 'botToken' "$main_file"
reject_marker 'AIza' "$main_file"

python3 - "$project_dir" <<'PY'
import pathlib
import sys
import xml.etree.ElementTree as ET

root = pathlib.Path(sys.argv[1])
export = (root / "app/src/main/kotlin/com/recapflow/ai/media/export/PublicExportCoordinator.kt").read_text()
main = (root / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt").read_text()
android = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()

pending = export.index("MediaStore.Video.Media.IS_PENDING, 1")
copy = export.index("copyCancellable(input, output, source.length())", pending)
final = export.index("MediaStore.Video.Media.IS_PENDING, 0", copy)
if not pending < copy < final:
    raise SystemExit("PUBLIC FINALIZATION: MediaStore pending/copy/final order changed")

open_start = main.index("private fun openPublishedExport()")
share_start = main.index("private fun sharePublishedExport()")
render_start = main.index("private fun renderPublicExportState(")
if "PublicExportUiState.Published" not in main[open_start:share_start]:
    raise SystemExit("OPEN GATE: Open is not restricted to Published")
if "PublicExportUiState.Published" not in main[share_start:render_start]:
    raise SystemExit("SHARE GATE: Share is not restricted to Published")

write_permissions = [
    item for item in manifest.findall("uses-permission")
    if item.get(android + "name") == "android.permission.WRITE_EXTERNAL_STORAGE"
]
if len(write_permissions) != 1 or write_permissions[0].get(android + "maxSdkVersion") != "28":
    raise SystemExit("LEGACY PERMISSION: WRITE_EXTERNAL_STORAGE must exist only through API 28")

providers = manifest.find("application").findall("provider")
if not any(
    item.get(android + "name") == "androidx.core.content.FileProvider" and
    item.get(android + "exported") == "false" and
    item.get(android + "grantUriPermissions") == "true"
    for item in providers
):
    raise SystemExit("FILEPROVIDER: exported=false temporary-grant provider is missing")

for path in (root / "app/src/main/res").rglob("*.xml"):
    ET.parse(path)

print("Phase 6F.1 private-first/public-finalization checks: PASS")
PY

echo "PASS: RecapFlowAI Phase 6F.1 source markers are valid."
