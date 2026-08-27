# RecapFlow FFmpeg SDK layout

Phase 2 uses a locally built, ARM64-only static FFmpeg SDK. Generated headers
and archives are intentionally not committed or bundled in the source archive.

Expected layout after running the build script:

```text
prebuilt/arm64-v8a/
├── include/
│   ├── libavcodec/
│   ├── libavfilter/
│   ├── libavformat/
│   ├── libavutil/
│   ├── libswresample/
│   └── libswscale/
└── lib/
    ├── libavcodec.a
    ├── libavfilter.a
    ├── libavformat.a
    ├── libavutil.a
    ├── libswresample.a
    └── libswscale.a
```

Build and install the SDK from an extracted FFmpeg source tree:

```bash
chmod +x scripts/build_ffmpeg_android_arm64.sh
./scripts/build_ffmpeg_android_arm64.sh /path/to/ffmpeg-source
```

Then enable the Phase 2 link gate:

```bash
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

The default remains `false`, so the already verified JNI baseline continues to
build before the generated FFmpeg SDK is available.
