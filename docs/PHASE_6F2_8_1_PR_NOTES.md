# PR notes — Phase 6F.2.8.1

Draft only until owner-device CBR quality validation passes.

Fixes #17 after verified merge. Bundles #13 presentation cleanup.

The branch restores CBR for the final H.264 master because the tested VBR encoder undershot a 10 Mbps request to about 2.78 Mbps and produced visibly soft 1080p output. It raises the bitrate presets, adds a hard finalized-output bitrate quality gate, widens the bounded duration floor to 350 ms, preserves source-aware FPS, and leaves the immutable EditPlan / CompositionPlayer / one-final-Transformer architecture unchanged.
