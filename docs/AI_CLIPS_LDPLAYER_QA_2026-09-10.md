# Manual / Auto editor and LDPlayer validation

The editor now separates the existing user-controlled tools into Manual and Gemini analysis into Auto. Export is shared. Changing modes changes the visible controls without discarding the edit. Auto exposes content type, output-duration preference and a bounded instruction field before the existing Analyze and Review/Apply flow. Those options are serialized into the Gemini request and described as whole-video preferences, not separate duration targets for each analysis window. Actual duration is shown at review; these presets do not guarantee an exact final length.

## Verified environment

- LDPlayer 14.0.25.2, Android API 34, `emulator-5554`, model 2211133C.
- ABI list includes x86_64 and arm64-v8a; the existing ARM64 APK installed successfully.
- Root setting was initially enabled. Tested with `basicSettings.rootMode` true, then false with a reboot. In non-root mode `command -v su` returned no executable. The original enabled setting was restored after testing.
- In both configurations the app process ran as ordinary Android user `u0_a74`; the app did not request root. No su invocation, privileged file access, root requirement or root-device rejection was added to the application.

## UI checks performed

| Check | Root enabled | Root disabled |
|---|---|---|
| Install / launch, native engine initialization | PASS | PASS |
| Import local smoke-test MP4 through Android picker | PASS | PASS |
| Manual tools visible; Auto switches to AI setup and shared export | PASS | PASS |
| Auto setup opens with content, duration and instruction controls | PASS | PASS |
| Content selection, duration selection and instruction input | Movie/story, 30–60 seconds, “Keep the key points” verified in UI tree | Controls present |
| Analyze without upload consent | Explicit consent message; no analysis started | Not repeated |
| Test connection with empty key | Not repeated | Explicit key error, loading dismissed |
| Crash buffer after checks | No crash observed | No crash observed |

ADB taps were derived from UI-tree bounds. The smoke-test video was a local screen recording sufficient to exercise import/navigation; its static-frame duration was not suitable for export-quality evaluation. No API key, personal media or test file was sent to Gemini.

## Automated validation and limits

Final FFmpeg/Crossfade-enabled debug build: PASS. Full unit suite: 279 tests, zero failures/errors. Localization verifier: 611 strings PASS.

The options contract tests verify content type, whole-video duration and instructions reach the structured analysis request, reject oversized instructions, and ensure instruction text cannot add JSON tool fields. Existing planner, transport and Apply/Undo tests remain part of the full suite. EN/MY resources include the new Manual/Auto workflow labels, and the old local-processing message now distinguishes optional Gemini upload.

These checks establish launch and setup compatibility on the tested rooted and non-rooted LDPlayer configurations. They do not establish compatibility with every rooted ROM, physical-device media codecs, successful live Gemini inference, Burmese semantic/timestamp quality or 720p/1080p A/V export. Those acceptance gates remain open in PR #38 / Issue #36. Voiceover, TTS, SRT and Narrator remain deferred.
