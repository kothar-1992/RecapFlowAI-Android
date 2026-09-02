#!/usr/bin/env sh
set -eu

PATCH_FILE="scripts/phase6h1d_boundary_controls.patch"

if [ ! -f "$PATCH_FILE" ]; then
  echo "Missing $PATCH_FILE" >&2
  exit 1
fi

# The staged patch was authored with stale hunk line counts. --recount makes git infer
# the hunk sizes from the actual patch body while still requiring all context to match.
git apply --recount --check "$PATCH_FILE"
git apply --recount "$PATCH_FILE"

git diff --check

echo "Phase 6H.1D MainActivity integration applied."
echo "Review with: git status --short && git diff -- app/src/main/kotlin/com/recapflow/ai/MainActivity.kt"
