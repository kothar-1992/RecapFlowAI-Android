#!/usr/bin/env python3
"""Verify Myanmar user-facing string coverage and numeral policy.

Rules:
- Every translatable <string> in res/values must exist in res/values-my.
- Myanmar translations must not contain Myanmar numeral glyphs (၀၁၂၃၄၅၆၇၈၉).
  RecapFlowAI intentionally keeps 0-9 numerals consistent in both languages.
- Android format placeholders are compared to catch accidental localization breakage.
"""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DIR = ROOT / "app" / "src" / "main" / "res" / "values"
MYANMAR_DIR = ROOT / "app" / "src" / "main" / "res" / "values-my"
MYANMAR_DIGITS = set("၀၁၂၃၄၅၆၇၈၉")

# Android string resources in this project primarily use positional format arguments
# such as %1$s, %2$d, and %3$.2f. Also accept compact non-positional printf-style
# arguments (for example %s or %.2f), but deliberately do not allow whitespace after
# '%' so ordinary percentage prose such as "100% for preview" is never mistaken for
# a format argument.
FORMAT_RE = re.compile(
    r"%(?:"
    r"\d+\$[-+#0,(<]*\d*(?:\.\d+)?[bBhHsScCdoxXeEfgGaAtTn]"
    r"|"
    r"[-+#0,(<]*\d*(?:\.\d+)?[bBhHsScCdoxXeEfgGaAtTn]"
    r")"
)


def read_strings(directory: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for path in sorted(directory.glob("*.xml")):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as exc:
            raise SystemExit(f"Invalid XML: {path}: {exc}") from exc
        for node in root.findall("string"):
            if node.attrib.get("translatable", "true").lower() == "false":
                continue
            name = node.attrib.get("name")
            if not name:
                continue
            if name in result:
                raise SystemExit(f"Duplicate string resource {name!r} in {directory}")
            result[name] = "".join(node.itertext())
    return result


def placeholders(value: str) -> list[str]:
    return sorted(FORMAT_RE.findall(value))


def main() -> int:
    default = read_strings(DEFAULT_DIR)
    myanmar = read_strings(MYANMAR_DIR)

    problems: list[str] = []
    missing = sorted(set(default) - set(myanmar))
    extra = sorted(set(myanmar) - set(default))
    if missing:
        problems.append("Missing Myanmar strings: " + ", ".join(missing))
    if extra:
        problems.append("Myanmar-only strings without a default resource: " + ", ".join(extra))

    for name in sorted(set(default) & set(myanmar)):
        value = myanmar[name]
        digits = sorted(set(value) & MYANMAR_DIGITS)
        if digits:
            problems.append(
                f"{name}: Myanmar numeral glyphs are not allowed ({''.join(digits)}); use 0-9"
            )
        if placeholders(default[name]) != placeholders(value):
            problems.append(
                f"{name}: format placeholders differ: "
                f"default={placeholders(default[name])}, my={placeholders(value)}"
            )

    if problems:
        print("Myanmar localization verification FAILED")
        for problem in problems:
            print(f"- {problem}")
        return 1

    print(
        "Myanmar localization verification PASS: "
        f"{len(default)} strings covered; English numerals 0-9 policy satisfied."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
