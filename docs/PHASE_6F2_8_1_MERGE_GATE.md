# Phase 6F.2.8.1 Merge Gate

This branch must remain unmerged until all of the following are true:

- [ ] source verifier passes after syncing branch
- [ ] Termux unit tests pass
- [ ] Termux debug APK build passes
- [ ] owner-device 1080p CBR quality passes
- [ ] owner-device 720p CBR quality passes
- [ ] reported bitrate is >=80% target when available
- [ ] geometry/FPS/H.264/audio/duration/Gallery checks pass
- [ ] one-final-render architecture remains intact

On PASS: merge PR, close #17, close #13, create/update stable rollback marker, then unblock Phase 6G.1.
