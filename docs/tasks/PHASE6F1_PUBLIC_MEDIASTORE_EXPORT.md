# Phase 6F.1 — Public MediaStore export

## Goal

Make a completed RecapFlow MP4 visible in Gallery without weakening the verified render safety
contract. Transformer continues to render into app-private storage. Public publication is a
separate finalization stage that starts only after the private MP4 exists and has non-zero size.

## Included

- A fifth `Export` tab in the Review Editor.
- Automatic API 29+ publication into `Movies/RecapFlowAI` using scoped MediaStore.
- `IS_PENDING=1` while bytes are copied and `IS_PENDING=0` only after a complete copy.
- MediaStore-row deletion after publication failure/cancellation.
- API 28-only `WRITE_EXTERNAL_STORAGE` request, hidden pending file, final rename, scanner update,
  and FileProvider Open/Share URI.
- Collision-safe output names on API 28.
- Retry after permission denial or finalization failure while retaining the private render.
- Open and Share actions only for a `Published` state.
- Pure output-name tests, a source preflight, and an AndroidIDE/device matrix.

## Excluded

- Writing Transformer output directly to shared storage.
- Deleting the verified private render after publication.
- Background render/export survival across process death (Phase 7).
- Saved editor presets and last-session restore (Phase 6F.2).
- Telegram upload, Gemini, direct-touch effect editing, and image animation loops.

## Safety contract

```text
Media3 render
    ↓
non-empty private MP4
    ↓
pending public copy
    ↓
copy length verified
    ↓
public finalization
    ↓
Open / Share unlocked
```

Failure before public finalization removes only the pending public destination. The private
completed MP4 and original source remain untouched.

## Acceptance

- `bash scripts/verify_phase6f1_source.sh` passes.
- `:app:testDebugUnitTest` and `:app:assembleDebug` pass in AndroidIDE.
- API 29+ produces a Gallery-visible MediaStore video with no storage permission prompt.
- API 28 denial keeps the private output; grant + retry produces a Gallery-visible video.
- Open/Share never appears for Idle, PermissionRequired, Publishing, or Failed.
- Cancelled render and failed public-copy paths leave no visible partial MP4.
