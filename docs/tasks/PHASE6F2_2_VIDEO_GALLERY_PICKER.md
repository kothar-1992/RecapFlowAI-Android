# Phase 6F.2.2 — Video gallery picker UX

## Problem

The Home and Editor import actions launch `ACTION_OPEN_DOCUMENT` directly. On the owner device,
that opens DocumentsUI and makes the user navigate folders without a useful visual video grid.
Finding the intended clip is slow even though Android provides a privacy-preserving media picker.

## Gate

Replace only the source-video selection contract with AndroidX `PickVisualMedia` in video-only
mode. Supported devices must show the system Photo Picker with video thumbnails, ordered by the
device media library. When the Photo Picker is unavailable, AndroidX must retain its documented
`ACTION_OPEN_DOCUMENT` fallback.

## Included

- Register one `ActivityResultContracts.PickVisualMedia` launcher for source video.
- Launch it with `PickVisualMediaRequest(PickVisualMedia.VideoOnly)`.
- Pin AndroidX Activity KTX 1.10.1 so the picker contract is explicit in the build.
- Request installation of the Google Play services Photo Picker backport on eligible API 28-29
  devices.
- Keep cancellation, URI grant persistence when supported, private working-copy import, FFmpeg
  probe, preview, editing, render, export, and source replacement behavior unchanged.
- Update Home copy so the import path is described as a device gallery rather than folder storage.

## Non-goals

- No custom gallery UI or direct MediaStore browser.
- No broad `READ_MEDIA_VIDEO` permission.
- No multi-select, camera capture, cloud upload, or background upload.
- Audio replacement and image/logo selection remain document pickers in this gate.
- No change to source-blur, preferences, effects, encoding, or public-export semantics.

## Acceptance

- On the owner device, Import video and Choose another video open a visual video thumbnail picker.
- Only videos are selectable.
- Selecting a video continues into the existing private copy and FFmpeg probe flow.
- Closing the picker restores the empty/current state without importing anything.
- On a device without Photo Picker support, source selection still works through DocumentsUI.
- No whole-library media permission is introduced on Android 13+.
