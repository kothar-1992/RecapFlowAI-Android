#!/usr/bin/env python3
"""Corrected runner for the second Human Myanmar review pass."""

from __future__ import annotations

import re
import refine_human_myanmar_copy as review

# The first script intentionally keeps all reviewed replacement data in one place.
# Override its matcher here with the correct XML string-name regex.
review.STRING_RE = re.compile(
    r'(<string\b[^>]*\bname="(?P<name>[^"]+)"[^>]*>)(?P<value>.*?)(</string>)'
)

if __name__ == "__main__":
    raise SystemExit(review.main())
