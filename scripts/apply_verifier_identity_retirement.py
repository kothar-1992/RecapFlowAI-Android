#!/usr/bin/env python3
"""Retire the superseded project/version identity pins from the historical verifiers.

Each `scripts/verify_phase*.sh` used to assert two things about the phase it belonged to:

    require_marker 'rootProject.name = "RecapFlowAI_Phase6B1"' "settings.gradle.kts"
    require_marker 'versionName = "1.0-phase6b1"' "app/build.gradle.kts"

Both were correct on the day the phase landed. The project name and `versionName` then
advanced with every later phase, so the assertions became permanently unsatisfiable and
every one of those verifiers has been exiting at the first identity check. The behavioural
invariants that follow each pair -- the transforms, blurs, overlays, render policy, and
unit-test markers that are the actual point of the script -- have not run since.

This removes only the dead identity pins and leaves every other check in place, so the
real invariants start running again. The current phase verifier and the CBR baseline
verifier are skipped: they own version identity today, and the baseline already carries a
normalized drift guard.

Idempotent: a file with no remaining identity pins is left untouched.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPTS = ROOT / "scripts"

# These two own version identity today and must keep their checks.
KEEP = {"verify_phase6ux2a_side_menu.sh", "verify_phase6f2_8_1_source.sh"}

MARKER = "PHASE6UX2_VERIFIER_IDENTITY_RETIRED"

PROJECT_NAME_PIN = re.compile(r'rootProject\.name\s*=\s*"[^"]*"')
VERSION_NAME_PIN = re.compile(r'versionName\s*=\s*"1\.0-phase')

COMMENT = [
    f"# {MARKER}",
    "# The project name and versionName this phase used to pin have both moved on, so these",
    "# two identity checks could only ever fail. They also short-circuited every behavioural",
    "# check below, which is why they are retired rather than repaired. Version identity is",
    "# owned by the current phase verifier and the CBR baseline drift guard instead.",
]


def is_identity_line(line: str) -> bool:
    if MARKER in line:
        return False
    if not PROJECT_NAME_PIN.search(line) and not VERSION_NAME_PIN.search(line):
        return False
    # Leave the call-shaped lines of verifiers we are not touching out of the pattern.
    return "require_marker" in line or "require_text" in line or "grep -q" in line


def main() -> int:
    changed: list[str] = []
    total_removed = 0

    for path in sorted(SCRIPTS.glob("verify_phase*.sh")):
        if path.name in KEEP:
            continue

        text = path.read_text(encoding="utf-8")
        if MARKER in text:
            continue

        lines = text.splitlines(keepends=True)
        out: list[str] = []
        inserted = False
        removed = 0

        for line in lines:
            if is_identity_line(line):
                if not inserted:
                    out.extend(f"{c}\n" for c in COMMENT)
                    inserted = True
                removed += 1
                continue
            out.append(line)

        if not removed:
            continue
        if not inserted:
            continue

        path.write_text("".join(out), encoding="utf-8", newline="\n")
        changed.append(path.name)
        total_removed += removed

    print(f"PASS: retired {total_removed} superseded identity pins across {len(changed)} verifiers.")
    for name in changed:
        print(f"  {name}")
    print("Behavioural checks in those verifiers now run again.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
