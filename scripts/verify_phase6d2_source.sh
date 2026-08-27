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
render_file="app/src/main/kotlin/com/recapflow/ai/media/render/LocalRenderCoordinator.kt"
compiler_file="app/src/main/kotlin/com/recapflow/ai/media/edit/AudioCompiler.kt"
processor_file="app/src/main/kotlin/com/recapflow/ai/media/render/PcmVolumeAudioProcessor.kt"

require_marker 'rootProject.name = "RecapFlowAI_Phase6D2"' "settings.gradle.kts"
require_marker 'versionName = "1.0-phase6d2"' "app/build.gradle.kts"
require_marker 'android:id="@+id/audioVolumeSlider"' "$layout_file"
require_marker 'android:id="@+id/audioVolumeResetButton"' "$layout_file"
require_marker 'private var audioVolume = AudioCompiler.UNITY_LINEAR_GAIN' "$main_file"
require_marker 'outState.putFloat(KEY_AUDIO_VOLUME, audioVolume)' "$main_file"
require_marker 'val linearGain: Float' "$compiler_file"
require_marker 'class PcmVolumeAudioProcessor' "$processor_file"
require_marker 'add(PcmVolumeAudioProcessor(it.linearGain))' "$render_file"
require_marker 'speedEffects?.audioProcessor?.let(::add)' "$render_file"
require_marker 'class AudioCompilerTest' \
  "app/src/test/kotlin/com/recapflow/ai/media/edit/AudioCompilerTest.kt"
require_marker 'class PcmVolumeAudioProcessorTest' \
  "app/src/test/kotlin/com/recapflow/ai/media/render/PcmVolumeAudioProcessorTest.kt"
require_marker 'AUDIO_POLICY_UNSUPPORTED' \
  "app/src/main/kotlin/com/recapflow/ai/media/edit/EditPlanValidator.kt"

python3 - "$project_dir" <<'PY'
import collections
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

root = pathlib.Path(sys.argv[1])
res = root / "app/src/main/res"
android_id = "{http://schemas.android.com/apk/res/android}id"

for xml in res.rglob("*.xml"):
    tree = ET.parse(xml)
    if xml.parent.name.startswith("layout"):
        ids = []
        for node in tree.iter():
            value = node.attrib.get(android_id, "")
            if value.startswith("@+id/"):
                ids.append(value.removeprefix("@+id/"))
        duplicates = [name for name, count in collections.Counter(ids).items() if count > 1]
        if duplicates:
            raise SystemExit(f"DUPLICATE IDS in {xml}: {duplicates}")

definitions = {
    "string": set(),
    "color": set(),
    "dimen": set(),
    "drawable": {p.stem for p in res.glob("drawable*/*")},
}
for values_dir in res.glob("values*"):
    for xml in values_dir.glob("*.xml"):
        values_root = ET.parse(xml).getroot()
        for node in values_root:
            name = node.attrib.get("name")
            if name and node.tag in definitions:
                definitions[node.tag].add(name)

ref_pattern = re.compile(r"@(string|color|dimen|drawable)/([A-Za-z0-9_]+)")
for xml in res.rglob("*.xml"):
    text = xml.read_text(encoding="utf-8")
    for kind, name in ref_pattern.findall(text):
        if name not in definitions[kind]:
            raise SystemExit(f"MISSING RESOURCE: @{kind}/{name} referenced by {xml}")

print("XML/resources: PASS")
PY

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
                state = "triple"
                index += 3
                continue
            if two == "//":
                state = "line_comment"
                index += 2
                continue
            if two == "/*":
                state = "block_comment"
                index += 2
                continue
            if char == '"':
                state = "string"
            elif char == "'":
                state = "char"
            elif char in openers:
                stack.append(char)
            elif char in pairs:
                if not stack or stack.pop() != pairs[char]:
                    raise SystemExit(f"UNBALANCED KOTLIN DELIMITER in {source} near {index}")
        elif state == "line_comment":
            if char == "\n":
                state = "code"
        elif state == "block_comment":
            if two == "*/":
                state = "code"
                index += 2
                continue
        elif state == "triple":
            if three == '\"\"\"':
                state = "code"
                index += 3
                continue
        elif char == "\\":
            index += 2
            continue
        elif (state == "string" and char == '"') or (state == "char" and char == "'"):
            state = "code"
        index += 1
    if stack or state in {"block_comment", "string", "char", "triple"}:
        raise SystemExit(f"UNTERMINATED KOTLIN STRUCTURE in {source}")

print("Kotlin delimiter scan: PASS")
PY

while IFS= read -r script; do
  bash -n "$script"
done < <(find "$project_dir/scripts" -maxdepth 1 -type f -name '*.sh' -print)

if grep -RInE '(AIza[0-9A-Za-z_-]{20,}|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|api[_-]?key[[:space:]]*=[[:space:]]*["'"'][^"'"']+["'"'])' \
  "$project_dir/app/src" "$project_dir/docs" --exclude-dir=build; then
  echo "POSSIBLE SECRET found in delivery source" >&2
  exit 1
fi

echo "PASS: RecapFlowAI Phase 6D.2 Volume markers and static source checks are valid."
