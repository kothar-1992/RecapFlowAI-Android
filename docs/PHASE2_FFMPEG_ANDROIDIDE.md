# Phase 2 — FFmpeg ARM64 build on AndroidIDE-RV2

- **Verified release:** FFmpeg `9.0.1`
- **Verified date:** 2026-08-19
- **Gate result:** ARM64 build, static link, APK launch, and runtime version call successful

This gate keeps the verified NDK `24.0.8215888` and CMake `3.18.1` baseline.
Do not switch back to the incompatible NDK r29 host compiler.

## 1. Verify the working toolchain

```bash
grep Pkg.Revision \
  "$HOME/android-sdk/ndk/24.0.8215888/source.properties"

file \
  "$HOME/android-sdk/ndk/24.0.8215888/toolchains/llvm/prebuilt/"*/bin/aarch64-linux-android21-clang
```

The compiler must be executable on the Android device. The directory name may
still contain `linux-x86_64`; trust the `file` result and the proven build, not
the compatibility directory label.

Install and verify GNU Make before starting the FFmpeg source build:

```bash
pkg install make
make --version
```

## 2. Extract an official FFmpeg source release

Keep the extracted source outside this project. Record its exact release/tag
for licensing and reproducibility. Example placeholder:

```text
/storage/emulated/0/Download/ffmpeg-source
```

## 3. Build and install the ARM64 SDK

Run from the RecapFlowAI project root:

```bash
chmod +x scripts/build_ffmpeg_android_arm64.sh

RECAPFLOW_BUILD_JOBS=4 \
  ./scripts/build_ffmpeg_android_arm64.sh \
  /storage/emulated/0/Download/ffmpeg-source
```

The temporary compiler output is kept under internal app storage at
`$HOME/.recapflow/ffmpeg-build`. The generated headers and static libraries are
installed into `app/src/main/cpp/ffmpeg/prebuilt/arm64-v8a`.

The script detects whether the selected FFmpeg release still provides
`libpostproc`. Newer releases that removed it automatically skip the obsolete
`--disable-postproc` configure option.

It also restores executable permission on `configure` and all FFmpeg `*.sh`
helpers. This is required when a source archive was first extracted on Android
shared storage, which does not reliably preserve executable mode bits. The
source tree should therefore be copied under `$HOME` before this step.

If the device becomes hot or runs low on memory, repeat with
`RECAPFLOW_BUILD_JOBS=2`.

## 4. Enable the FFmpeg link gate

```bash
rm -rf app/.cxx app/build .gradle build
rm -rf "$HOME/.recapflow/cxx/RecapFlowAI"

AAPT2_BIN="$HOME/android-sdk/build-tools/35.0.1/aapt2"

bash ./gradlew :app:assembleDebug \
  -Precapflow.ffmpeg.enabled=true \
  -Pandroid.aapt2FromMavenOverride="$AAPT2_BIN" \
  --no-daemon \
  --max-workers=2 \
  --stacktrace
```

AndroidIDE-RV2 runs on an ARM64 Android host. The explicit AAPT2 override uses
the verified Build Tools `35.0.1` binary instead of the incompatible Maven Linux
host executable. Keep this device-local path in `$HOME/.gradle/gradle.properties`
if a persistent override is desired; do not commit it to the project.

All removed paths above are generated build caches; project sources and the
generated FFmpeg SDK are not removed.

## 5. Install and verify

Launch the debug APK. The existing status screen should change from:

```text
RecapFlow Native 0.1.0
```

to a value containing:

```text
RecapFlow Native 0.1.0 / FFmpeg <version>
```

That result proves a real `av_version_info()` symbol was linked into
`libflowai.so` and resolved at runtime. It does not yet prove probe, codecs,
rendering, or audio synchronization; those remain later gates.

Verified result:

```text
BUILD SUCCESSFUL in 1m 52s
Native bridge READY
RecapFlow Native 0.1.0 / FFmpeg 9.0.1
```

## 6. Preserve evidence

Save the successful build log and screenshot. Before public distribution, also
save the generated FFmpeg configure output and complete the LGPL/static-link
compliance review described in `THIRD_PARTY_NOTICES.md`.
