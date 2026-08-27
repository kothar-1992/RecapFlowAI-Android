#ifndef RECAPFLOW_FFMPEG_PROBE_H
#define RECAPFLOW_FFMPEG_PROBE_H

#include <cstdint>
#include <string>

namespace recapflow::ffmpeg {

enum class ProbeError {
    kNone,
    kUnavailable,
    kInvalidPath,
    kOpenFailed,
    kStreamInfoFailed,
    kVideoStreamMissing,
};

struct ProbeMetadata {
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

struct ProbeResult {
    ProbeError error{ProbeError::kNone};
    std::string message;
    std::string ffmpeg_error;
    ProbeMetadata metadata;

    [[nodiscard]] bool ok() const noexcept {
        return error == ProbeError::kNone;
    }
};

[[nodiscard]] ProbeResult ProbeFile(const std::string& input_path);

}  // namespace recapflow::ffmpeg

#endif  // RECAPFLOW_FFMPEG_PROBE_H
