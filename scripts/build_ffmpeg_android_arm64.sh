#!/usr/bin/env bash

set -euo pipefail

readonly RECAPFLOW_NDK_VERSION="24.0.8215888"
readonly RECAPFLOW_ANDROID_API="21"
readonly RECAPFLOW_ABI="arm64-v8a"
readonly RECAPFLOW_TARGET="aarch64-linux-android"

script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_directory="$(cd "${script_directory}/.." && pwd)"
ffmpeg_source_directory="${1:-${FFMPEG_SOURCE_DIR:-}}"

if [[ -z "${ffmpeg_source_directory}" ]]; then
    echo "Usage: $0 /path/to/extracted-ffmpeg-source" >&2
    exit 2
fi

if [[ ! -f "${ffmpeg_source_directory}/configure" ]]; then
    echo "FFmpeg configure script was not found: ${ffmpeg_source_directory}/configure" >&2
    exit 2
fi

ffmpeg_source_directory="$(cd "${ffmpeg_source_directory}" && pwd)"

# Android shared storage does not reliably preserve executable bits when an
# archive is extracted and copied. FFmpeg invokes these helpers directly from
# Makefiles, so normalize their permissions in the internal source copy.
if ! chmod u+x "${ffmpeg_source_directory}/configure"; then
    echo "Cannot make FFmpeg configure executable." >&2
    echo "Copy the source tree under \$HOME and run the script from there." >&2
    exit 2
fi
find "${ffmpeg_source_directory}" -type f -name "*.sh" -exec chmod u+x {} +

if ! command -v make >/dev/null 2>&1; then
    echo "GNU make is required to build FFmpeg." >&2
    echo "Install it in AndroidIDE-RV2 with: pkg install make" >&2
    exit 2
fi

android_sdk_directory="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-${HOME}/android-sdk}}"
android_ndk_directory="${ANDROID_NDK_HOME:-${android_sdk_directory}/ndk/${RECAPFLOW_NDK_VERSION}}"

if [[ ! -f "${android_ndk_directory}/source.properties" ]]; then
    echo "Android NDK ${RECAPFLOW_NDK_VERSION} was not found: ${android_ndk_directory}" >&2
    exit 2
fi

toolchain_directory=""
for candidate in "${android_ndk_directory}/toolchains/llvm/prebuilt"/*; do
    if [[ -x "${candidate}/bin/${RECAPFLOW_TARGET}${RECAPFLOW_ANDROID_API}-clang" ]]; then
        toolchain_directory="${candidate}"
        break
    fi
done

if [[ -z "${toolchain_directory}" ]]; then
    echo "No executable ARM64 Android clang was found in ${android_ndk_directory}" >&2
    exit 2
fi

build_parent_directory="${RECAPFLOW_FFMPEG_BUILD_ROOT:-${HOME}/.recapflow/ffmpeg-build}"
mkdir -p "${build_parent_directory}"
build_directory="$(mktemp -d "${build_parent_directory}/arm64.XXXXXX")"
install_directory="${build_directory}/install"

cleanup() {
    rm -rf "${build_directory}"
}
trap cleanup EXIT

configure_arguments=(
    "--prefix=${install_directory}"
    "--target-os=android"
    "--arch=aarch64"
    "--cpu=armv8-a"
    "--enable-cross-compile"
    "--sysroot=${toolchain_directory}/sysroot"
    "--cc=${toolchain_directory}/bin/${RECAPFLOW_TARGET}${RECAPFLOW_ANDROID_API}-clang"
    "--cxx=${toolchain_directory}/bin/${RECAPFLOW_TARGET}${RECAPFLOW_ANDROID_API}-clang++"
    "--ar=${toolchain_directory}/bin/llvm-ar"
    "--nm=${toolchain_directory}/bin/llvm-nm"
    "--ranlib=${toolchain_directory}/bin/llvm-ranlib"
    "--strip=${toolchain_directory}/bin/llvm-strip"
    "--enable-static"
    "--disable-shared"
    "--enable-pic"
    "--enable-jni"
    "--enable-mediacodec"
    "--disable-programs"
    "--disable-doc"
    "--disable-debug"
    "--disable-avdevice"
    "--disable-network"
    "--disable-autodetect"
    "--disable-gpl"
    "--disable-nonfree"
    "--extra-cflags=-O2 -fPIC"
    "--extra-ldflags=-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
)

# libpostproc and its configure switch were removed from newer FFmpeg releases.
# Keep older releases lean without breaking newer source trees.
if grep -q -- "--disable-postproc" "${ffmpeg_source_directory}/configure"; then
    configure_arguments+=("--disable-postproc")
else
    echo "FFmpeg does not provide libpostproc; skipping --disable-postproc"
fi

echo "Configuring FFmpeg for ${RECAPFLOW_ABI} with NDK ${RECAPFLOW_NDK_VERSION}"
cd "${build_directory}"
"${ffmpeg_source_directory}/configure" "${configure_arguments[@]}"

parallel_jobs="${RECAPFLOW_BUILD_JOBS:-}"
if [[ -z "${parallel_jobs}" ]]; then
    parallel_jobs="$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 4)"
fi

make -j"${parallel_jobs}"
make install

destination_directory="${project_directory}/app/src/main/cpp/ffmpeg/prebuilt/${RECAPFLOW_ABI}"
mkdir -p "${destination_directory}/include" "${destination_directory}/lib"
cp -R "${install_directory}/include/." "${destination_directory}/include/"

required_components=(avutil avcodec avformat avfilter swscale swresample)
for component in "${required_components[@]}"; do
    source_library="${install_directory}/lib/lib${component}.a"
    if [[ ! -f "${source_library}" ]]; then
        echo "Expected FFmpeg archive was not produced: ${source_library}" >&2
        exit 1
    fi
    cp "${source_library}" "${destination_directory}/lib/"
done

cp "${ffmpeg_source_directory}/LICENSE.md" \
    "${destination_directory}/FFMPEG_LICENSE.md"

echo "FFmpeg ARM64 SDK installed at ${destination_directory}"
echo "Build with: ./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace"
