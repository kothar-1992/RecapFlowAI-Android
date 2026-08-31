#!/usr/bin/env python3
"""Fix Phase 6H.1E ViewBinding constructor parameter overflow.

view_editor_destination.xml is already close to the JVM method parameter limit. Adding
2 Random Mirror IDs pushes the generated ViewEditorDestinationBinding constructor over
that limit. Move the 4 Mirror controls into an included child layout and access them
through root.findViewById from MainActivity. This reduces the parent binding field count
without changing runtime UI semantics.

Run after scripts/apply_phase6h1e_random_mirror.py.
"""

from __future__ import annotations

from pathlib import Path
import textwrap

ROOT = Path(__file__).resolve().parents[1]
LAYOUT = ROOT / "app/src/main/res/layout/view_editor_destination.xml"
CHILD = ROOT / "app/src/main/res/layout/view_transform_mirror_controls.xml"
MAIN = ROOT / "app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"

IDS = (
    "mirrorEnabledSwitch",
    "mirrorSummary",
    "randomMirrorPerClipSwitch",
    "randomMirrorPerClipSummary",
)


def locate_element(text: str, view_id: str) -> tuple[int, int]:
    marker = f'android:id="@+id/{view_id}"'
    marker_pos = text.find(marker)
    if marker_pos < 0:
        raise RuntimeError(f"{view_id}: id not found in parent layout")
    start = text.rfind("<", 0, marker_pos)
    end = text.find("/>", marker_pos)
    if start < 0 or end < 0:
        raise RuntimeError(f"{view_id}: element boundaries not found")
    return start, end + 2


def patch_layout() -> bool:
    text = LAYOUT.read_text(encoding="utf-8")
    if '@layout/view_transform_mirror_controls' in text:
        return False

    spans = [locate_element(text, view_id) for view_id in IDS]
    start = min(span[0] for span in spans)
    end = max(span[1] for span in spans)
    block = text[start:end]

    for view_id in IDS:
        if f'@+id/{view_id}' not in block:
            raise RuntimeError(f"mirror block does not contain {view_id}")

    line_start = text.rfind("\n", 0, start) + 1
    indent = text[line_start:start]
    include = (
        f'<include\n'
        f'{indent}    layout="@layout/view_transform_mirror_controls"\n'
        f'{indent}    android:layout_width="match_parent"\n'
        f'{indent}    android:layout_height="wrap_content" />'
    )
    text = text[:start] + include + text[end:]
    LAYOUT.write_text(text, encoding="utf-8")

    child_block = textwrap.dedent(block).strip()
    child = f'''<?xml version="1.0" encoding="utf-8"?>\n<LinearLayout\n    xmlns:android="http://schemas.android.com/apk/res/android"\n    android:layout_width="match_parent"\n    android:layout_height="wrap_content"\n    android:orientation="vertical">\n\n{textwrap.indent(child_block, "    ")}\n\n</LinearLayout>\n'''
    CHILD.write_text(child, encoding="utf-8")
    return True


def patch_main() -> bool:
    text = MAIN.read_text(encoding="utf-8")
    anchor = '''    private val editor\n        get() = binding.editorContent\n'''
    properties = '''    private val mirrorEnabledSwitch\n        get() = binding.root.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(\n            R.id.mirrorEnabledSwitch,\n        )\n    private val mirrorSummaryView\n        get() = binding.root.findViewById<android.widget.TextView>(R.id.mirrorSummary)\n    private val randomMirrorPerClipSwitch\n        get() = binding.root.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(\n            R.id.randomMirrorPerClipSwitch,\n        )\n    private val randomMirrorPerClipSummaryView\n        get() = binding.root.findViewById<android.widget.TextView>(R.id.randomMirrorPerClipSummary)\n'''

    if "private val randomMirrorPerClipSummaryView" not in text:
        if anchor not in text:
            raise RuntimeError("MainActivity editor binding anchor not found")
        text = text.replace(anchor, anchor + properties, 1)

    replacements = {
        "editor.mirrorEnabledSwitch": "mirrorEnabledSwitch",
        "editor.mirrorSummary": "mirrorSummaryView",
        "editor.randomMirrorPerClipSwitch": "randomMirrorPerClipSwitch",
        "editor.randomMirrorPerClipSummary": "randomMirrorPerClipSummaryView",
    }
    for old, new in replacements.items():
        text = text.replace(old, new)

    MAIN.write_text(text, encoding="utf-8")
    return True


def verify() -> None:
    parent = LAYOUT.read_text(encoding="utf-8")
    child = CHILD.read_text(encoding="utf-8")
    main = MAIN.read_text(encoding="utf-8")

    if '@layout/view_transform_mirror_controls' not in parent:
        raise RuntimeError("parent include missing")
    for view_id in IDS:
        if f'@+id/{view_id}' in parent:
            raise RuntimeError(f"{view_id} still contributes to parent binding")
        if f'@+id/{view_id}' not in child:
            raise RuntimeError(f"{view_id} missing from child layout")
    for old in (
        "editor.mirrorEnabledSwitch",
        "editor.mirrorSummary",
        "editor.randomMirrorPerClipSwitch",
        "editor.randomMirrorPerClipSummary",
    ):
        if old in main:
            raise RuntimeError(f"stale parent binding reference remains: {old}")
    for token in (
        "private val mirrorEnabledSwitch",
        "private val randomMirrorPerClipSwitch",
        "R.id.randomMirrorPerClipSummary",
    ):
        if token not in main:
            raise RuntimeError(f"MainActivity binding-budget token missing: {token}")


def main() -> int:
    missing = [path for path in (LAYOUT, MAIN) if not path.exists()]
    if missing:
        for path in missing:
            print(f"ERROR: missing {path}")
        return 2

    changed_layout = patch_layout()
    patch_main()
    verify()
    print("Phase 6H.1E ViewBinding parameter-budget fix applied.")
    if changed_layout:
        print("Mirror controls moved into view_transform_mirror_controls.xml.")
    else:
        print("Mirror controls were already split from the parent binding.")
    print("Next: rm -rf app/build scripts/__pycache__; git diff --check; rerun unit tests.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
