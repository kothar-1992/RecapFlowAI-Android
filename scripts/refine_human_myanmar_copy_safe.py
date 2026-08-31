#!/usr/bin/env python3
"""Safely apply the second Human Myanmar review pass.

This runner deliberately avoids the older generic STRING_RE substitution path.
It replaces complete <string>...</string> elements by name, validates that every
requested key was found exactly once, parses all touched XML files after writing,
and only then patches the duration-advisor Kotlin formatting.

Recommended recovery/run sequence from repo root:
    git restore app/src/main/kotlin/com/recapflow/ai/MainActivity.kt \
      app/src/main/res/values/strings.xml \
      app/src/main/res/values-my/
    python3 scripts/humanize_myanmar_copy.py
    python3 scripts/refine_human_myanmar_copy_safe.py
    python3 scripts/verify_myanmar_localization.py
"""

from __future__ import annotations

from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

import refine_human_myanmar_copy as review

ROOT = Path(__file__).resolve().parents[1]
VALUES_MY = ROOT / "app" / "src" / "main" / "res" / "values-my"
DEFAULT_STRINGS = ROOT / "app" / "src" / "main" / "res" / "values" / "strings.xml"

NAME_RE = re.compile(r'<string\b[^>]*\bname="([^"]+)"')


def replace_named_string(text: str, name: str, value: str) -> tuple[str, int]:
    pattern = re.compile(
        r'(<string\b(?=[^>]*\bname="' + re.escape(name) + r'")[^>]*>)'
        r'(.*?)'
        r'(</string>)',
        re.DOTALL,
    )
    return pattern.subn(lambda m: f"{m.group(1)}{value}{m.group(3)}", text, count=1)


def rewrite_myanmar_files() -> int:
    if not VALUES_MY.is_dir():
        raise SystemExit(f"Missing Myanmar resource directory: {VALUES_MY}")

    seen: set[str] = set()
    changed = 0

    for path in sorted(VALUES_MY.glob("*.xml")):
        text = path.read_text(encoding="utf-8")
        names = set(NAME_RE.findall(text))
        file_changed = 0

        for name in sorted(names & review.MY.keys()):
            text, count = replace_named_string(text, name, review.MY[name])
            if count != 1:
                raise SystemExit(f"Expected exactly one <string name=\"{name}\"> in {path}")
            seen.add(name)
            file_changed += 1

        if file_changed:
            path.write_text(text, encoding="utf-8")
            changed += file_changed

        try:
            ET.parse(path)
        except ET.ParseError as exc:
            raise SystemExit(f"Malformed XML after Human Myanmar rewrite: {path}: {exc}") from exc

    missing = sorted(set(review.MY) - seen)
    if missing:
        raise SystemExit("Human Myanmar keys not found: " + ", ".join(missing))

    return changed


def rewrite_default_strings() -> int:
    text = DEFAULT_STRINGS.read_text(encoding="utf-8")
    changed = 0

    for name, value in review.DEFAULT.items():
        text, count = replace_named_string(text, name, value)
        if count != 1:
            raise SystemExit(f"Expected exactly one default <string name=\"{name}\">")
        changed += 1

    DEFAULT_STRINGS.write_text(text, encoding="utf-8")
    try:
        ET.parse(DEFAULT_STRINGS)
    except ET.ParseError as exc:
        raise SystemExit(f"Malformed default strings.xml after rewrite: {exc}") from exc

    return changed


def main() -> int:
    my_changed = rewrite_myanmar_files()
    default_changed = rewrite_default_strings()
    kotlin_changed = review.patch_duration_units()

    print(
        "Safe Human Myanmar review pass complete: "
        f"{my_changed} Myanmar strings updated; "
        f"{default_changed} English duration strings updated; "
        f"MainActivity duration formatting {'updated' if kotlin_changed else 'already current'}."
    )
    print("XML structure validation PASS.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
