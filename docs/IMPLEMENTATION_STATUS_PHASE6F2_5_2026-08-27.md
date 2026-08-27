# Phase 6F.2.5 implementation status — 2026-08-27

## Source complete

- Export displays the exact reviewed plan duration, nearest feasible whole second, adjustment,
  and calculated post-render drift allowance.
- `Update clips` changes only the final Trim or applied Adaptive range after user confirmation.
- Speed and Intro Freeze are included when resolving the target.
- Any prior render/public-export state is invalidated through the existing edit-change workflow.
- Post-render validation now uses a 250 ms floor, 0.1% long-file allowance, and 750 ms cap.
- The reported 293430 ms versus 293154 ms sample resolves to a 276 ms drift inside a 294 ms
  allowance, so it is accepted with a visible warning instead of deleted.
- Exact 720p/1080p/2K dimensions, H.264, AAC/mute policy, private-first rendering, and Gallery
  publication remain unchanged.
- Unit tests cover Trim, Adaptive Cuts, Speed/Freeze, whole-second state, the reported 276 ms
  result, a 295 ms rejection, and the hard tolerance cap.

## Verification pending

- AndroidIDE build/install with FFmpeg enabled
- Owner-device 293154 ms reproduction at 1080p
- Trim and Adaptive `Update clips` UI behavior
- 720p/1080p/2K output metadata and Gallery publication
- Audio sync, cancellation, low-storage, and preview fallback regression matrix

