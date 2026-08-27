# Phase 6F.1 AndroidIDE and device verification

## 1. Build gate

Open `RecapFlowAI_Phase6F1` and run:

```bash
bash scripts/verify_phase6f1_source.sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Confirm `versionName = "1.0-phase6f1"`, minSdk 28, targetSdk 34, Media3 `1.10.0`, and the fifth
Review Editor tab is `Export`.

## 2. Android 10+ scoped-storage matrix

1. Import/probe a short source and render 720p.
2. Confirm the Export tab changes Private/Publishing/Saved without a storage-permission prompt.
3. Open Gallery or Files and verify `Movies/RecapFlowAI/<name>.mp4` appears and plays.
4. Tap Open, then Share; verify both receive the finalized video.
5. Play the private 720p output to unlock 1080p, render it, and confirm a second public result.
6. Render twice within separate runs and confirm neither existing public file is overwritten.

## 3. Android 9 / API 28 matrix

1. Render 720p with storage permission not granted.
2. Confirm the private render completes and Export shows the permission-required state.
3. Deny permission. Confirm the private Play action still works and no public partial file appears.
4. Tap Retry, grant permission, and confirm the final MP4 appears in `Movies/RecapFlowAI`.
5. Confirm Open/Share works through temporary URI permission and Gallery indexes the video.

## 4. Failure and cleanup matrix

- Cancel renders around 10%, 50%, and 90%; no public export must start.
- During a disposable test with low free storage, force public-copy failure. Confirm the MediaStore
  pending row or API 28 `.pending` file is removed and Retry remains available.
- Import a new source while no render is active; the new Export tab must return to waiting state.
- Confirm source video and completed private render are never deleted by public-export failure.

## 5. Regression matrix

Verify portrait/landscape H.264 and HEVC, Trim, reviewed cuts, all enabled Transform operations,
Keep/Mute/Replace/Mix, source blur, logo, 720p playback unlock, 1080p render, Activity recreation,
and preview stability. Compare public output duration, aspect, frame content, audio, blur, and logo
against the in-app completed render.

## Pass criteria

- Gallery visibility and Open/Share work only after finalization.
- No visible zero-byte/partial file, source loss, crash, ANR, or new preview regression.
- API 29+ asks for no broad storage permission; API 28 asks only when saving publicly.
