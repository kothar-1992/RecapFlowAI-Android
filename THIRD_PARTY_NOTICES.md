# Third-party notices

## FFmpeg

RecapFlowAI Phase 2 was verified with a locally built FFmpeg `9.0.1` SDK. The
checked-in configuration disables GPL and nonfree options and does not enable
external codec libraries automatically. FFmpeg source code and generated binaries
are not included in this source archive.

Before distributing an APK that contains FFmpeg, record the exact FFmpeg version,
the complete configure output, enabled codecs, and the license text from that source
release. The current static-link proof of concept also requires a release-specific
LGPL compliance review and a documented relinking mechanism or a move to a reviewed
shared-library layout before public distribution.

## AndroidX Media3

RecapFlowAI Phase 6F.2.7 uses AndroidX Media3 `1.10.0` (`media3-common`,
`media3-effect`, `media3-exoplayer`, `media3-ui`, and `media3-transformer`)
under the Apache License 2.0. The application baseline is Android 9 / API 28
and newer.

Project and license information:

- https://github.com/androidx/media
- https://www.apache.org/licenses/LICENSE-2.0
