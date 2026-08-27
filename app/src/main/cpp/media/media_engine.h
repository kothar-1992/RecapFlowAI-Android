#ifndef RECAPFLOW_MEDIA_ENGINE_H
#define RECAPFLOW_MEDIA_ENGINE_H

#include <cstdint>
#include <string>

namespace recapflow::media {

enum class NativeErrorCode : int {
    kOk = 0,
    kNativeLibraryLoadFailed = 1000,
    kInputOpenFailed = 2000,
    kUnsupportedCodec = 2001,
    kInputCopyFailed = 2002,
    kInvalidInput = 2003,
    kEncoderNotFound = 3000,
    kFilterInitFailed = 4000,
    kOutputCreateFailed = 5000,
    kStorageFull = 5001,
    kCancelled = 6000,
    kNativeError = 9000,
};

enum class NativeStage : int {
    kInitialization = 0,
    kProbe = 1,
    kValidation = 2,
    kRender = 3,
    kCleanup = 4,
    kUnknown = 5,
};

struct MediaInfo {
    std::int64_t duration_ms{0};
    int width{0};
    int height{0};
    int rotation_degrees{0};
    double frame_rate{0.0};
    std::int64_t bitrate{0};
    std::string video_codec;
    std::string audio_codec;
    int audio_sample_rate{0};
    int audio_channels{0};
    std::string container_format;
};

struct NativeResult {
    NativeErrorCode code;
    NativeStage stage;
    std::string message;
    std::string ffmpeg_error;
    bool recoverable;

    [[nodiscard]] bool ok() const noexcept;

    static NativeResult Success(NativeStage stage, std::string message);
    static NativeResult Failure(
        NativeErrorCode code,
        NativeStage stage,
        std::string message,
        std::string ffmpeg_error = {},
        bool recoverable = false);
};

struct MediaProbeResult {
    NativeResult result;
    MediaInfo media_info;
};

class MediaEngine final {
public:
    MediaEngine() = delete;

    [[nodiscard]] static std::string NativeVersion();
    [[nodiscard]] static NativeResult HealthCheck();
    [[nodiscard]] static MediaProbeResult Probe(const std::string& input_path);
};

}  // namespace recapflow::media

#endif  // RECAPFLOW_MEDIA_ENGINE_H
