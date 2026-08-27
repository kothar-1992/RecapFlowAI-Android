#include "media/media_engine.h"

#include <utility>

#include "ffmpeg/ffmpeg_probe.h"
#include "ffmpeg/ffmpeg_runtime.h"

namespace recapflow::media {
namespace {

constexpr char kNativeVersion[] = "RecapFlow Native 0.1.0";

}  // namespace

bool NativeResult::ok() const noexcept {
    return code == NativeErrorCode::kOk;
}

NativeResult NativeResult::Success(NativeStage stage, std::string message) {
    return NativeResult{
        NativeErrorCode::kOk,
        stage,
        std::move(message),
        {},
        false,
    };
}

NativeResult NativeResult::Failure(
    NativeErrorCode code,
    NativeStage stage,
    std::string message,
    std::string ffmpeg_error,
    bool recoverable) {
    return NativeResult{
        code,
        stage,
        std::move(message),
        std::move(ffmpeg_error),
        recoverable,
    };
}

std::string MediaEngine::NativeVersion() {
    if (ffmpeg::IsLinked()) {
        return std::string(kNativeVersion) + " / FFmpeg " + ffmpeg::LinkedVersion();
    }
    return kNativeVersion;
}

NativeResult MediaEngine::HealthCheck() {
    if (ffmpeg::IsLinked() && ffmpeg::LinkedVersion().empty()) {
        return NativeResult::Failure(
            NativeErrorCode::kNativeError,
            NativeStage::kInitialization,
            "FFmpeg is linked but its runtime version is unavailable");
    }

    return NativeResult::Success(
        NativeStage::kInitialization,
        "Native media engine is ready");
}

MediaProbeResult MediaEngine::Probe(const std::string& input_path) {
    const ffmpeg::ProbeResult probe = ffmpeg::ProbeFile(input_path);
    if (!probe.ok()) {
        NativeErrorCode code = NativeErrorCode::kInputOpenFailed;
        bool recoverable = true;
        switch (probe.error) {
            case ffmpeg::ProbeError::kUnavailable:
                code = NativeErrorCode::kNativeError;
                recoverable = false;
                break;
            case ffmpeg::ProbeError::kInvalidPath:
                code = NativeErrorCode::kInvalidInput;
                break;
            case ffmpeg::ProbeError::kVideoStreamMissing:
                code = NativeErrorCode::kUnsupportedCodec;
                break;
            case ffmpeg::ProbeError::kOpenFailed:
            case ffmpeg::ProbeError::kStreamInfoFailed:
                code = NativeErrorCode::kInputOpenFailed;
                break;
            case ffmpeg::ProbeError::kNone:
                code = NativeErrorCode::kNativeError;
                recoverable = false;
                break;
        }

        return MediaProbeResult{
            NativeResult::Failure(
                code,
                NativeStage::kProbe,
                probe.message,
                probe.ffmpeg_error,
                recoverable),
            {},
        };
    }

    MediaInfo media_info;
    media_info.duration_ms = probe.metadata.duration_ms;
    media_info.width = probe.metadata.width;
    media_info.height = probe.metadata.height;
    media_info.rotation_degrees = probe.metadata.rotation_degrees;
    media_info.frame_rate = probe.metadata.frame_rate;
    media_info.bitrate = probe.metadata.bitrate;
    media_info.video_codec = probe.metadata.video_codec;
    media_info.audio_codec = probe.metadata.audio_codec;
    media_info.audio_sample_rate = probe.metadata.audio_sample_rate;
    media_info.audio_channels = probe.metadata.audio_channels;
    media_info.container_format = probe.metadata.container_format;

    return MediaProbeResult{
        NativeResult::Success(
            NativeStage::kProbe,
            "Video metadata is ready"),
        std::move(media_info),
    };
}

}  // namespace recapflow::media
