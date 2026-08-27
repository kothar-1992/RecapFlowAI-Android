#include "ffmpeg/ffmpeg_probe.h"

#include <algorithm>
#include <cmath>
#include <cstdlib>
#include <memory>
#include <string>
#include <utility>

#ifndef RECAPFLOW_HAS_FFMPEG
#define RECAPFLOW_HAS_FFMPEG 0
#endif

#if RECAPFLOW_HAS_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavcodec/codec_par.h>
#include <libavcodec/packet.h>
#include <libavformat/avformat.h>
#include <libavutil/dict.h>
#include <libavutil/display.h>
#include <libavutil/error.h>
#include <libavutil/mathematics.h>
}
#endif

namespace recapflow::ffmpeg {

#if RECAPFLOW_HAS_FFMPEG
namespace {

struct FormatContextDeleter {
    void operator()(AVFormatContext* context) const noexcept {
        if (context != nullptr) {
            avformat_close_input(&context);
        }
    }
};

using FormatContextPointer =
    std::unique_ptr<AVFormatContext, FormatContextDeleter>;

std::string ErrorText(int error_code) {
    char buffer[AV_ERROR_MAX_STRING_SIZE] = {};
    if (av_strerror(error_code, buffer, sizeof(buffer)) < 0) {
        return "FFmpeg error " + std::to_string(error_code);
    }
    return buffer;
}

std::int64_t DurationMilliseconds(
    const AVFormatContext* context,
    const AVStream* video_stream) {
    if (context->duration != AV_NOPTS_VALUE && context->duration > 0) {
        return av_rescale(context->duration, 1000, AV_TIME_BASE);
    }

    if (video_stream->duration != AV_NOPTS_VALUE &&
        video_stream->duration > 0) {
        return av_rescale_q(
            video_stream->duration,
            video_stream->time_base,
            AVRational{1, 1000});
    }

    return 0;
}

int NormalizeRotation(double rotation) {
    if (!std::isfinite(rotation)) {
        return 0;
    }

    int degrees = static_cast<int>(std::lround(rotation));
    degrees %= 360;
    if (degrees < 0) {
        degrees += 360;
    }
    return degrees;
}

int RotationDegrees(const AVStream* video_stream) {
    const AVCodecParameters* parameters = video_stream->codecpar;
    const AVPacketSideData* display_matrix = av_packet_side_data_get(
        parameters->coded_side_data,
        parameters->nb_coded_side_data,
        AV_PKT_DATA_DISPLAYMATRIX);
    if (display_matrix != nullptr &&
        display_matrix->size >= 9 * sizeof(std::int32_t)) {
        const auto* matrix =
            reinterpret_cast<const std::int32_t*>(display_matrix->data);
        return NormalizeRotation(av_display_rotation_get(matrix));
    }

    const AVDictionaryEntry* rotate =
        av_dict_get(video_stream->metadata, "rotate", nullptr, 0);
    if (rotate != nullptr && rotate->value != nullptr) {
        char* end = nullptr;
        const double value = std::strtod(rotate->value, &end);
        if (end != rotate->value) {
            return NormalizeRotation(value);
        }
    }

    return 0;
}

std::int64_t Bitrate(
    const AVFormatContext* context,
    const AVCodecParameters* video,
    const AVCodecParameters* audio) {
    if (context->bit_rate > 0) {
        return context->bit_rate;
    }

    const std::int64_t video_bitrate =
        video != nullptr && video->bit_rate > 0 ? video->bit_rate : 0;
    const std::int64_t audio_bitrate =
        audio != nullptr && audio->bit_rate > 0 ? audio->bit_rate : 0;
    return video_bitrate + audio_bitrate;
}

std::string CodecName(const AVCodecParameters* parameters) {
    if (parameters == nullptr) {
        return {};
    }
    const char* name = avcodec_get_name(parameters->codec_id);
    return name == nullptr ? std::string{} : std::string{name};
}

}  // namespace
#endif

ProbeResult ProbeFile(const std::string& input_path) {
#if RECAPFLOW_HAS_FFMPEG
    if (input_path.empty()) {
        return ProbeResult{
            ProbeError::kInvalidPath,
            "The prepared video path is empty",
            {},
            {},
        };
    }

    AVFormatContext* raw_context = nullptr;
    const int open_result =
        avformat_open_input(&raw_context, input_path.c_str(), nullptr, nullptr);
    if (open_result < 0) {
        if (raw_context != nullptr) {
            avformat_close_input(&raw_context);
        }
        return ProbeResult{
            ProbeError::kOpenFailed,
            "FFmpeg could not open the selected video",
            ErrorText(open_result),
            {},
        };
    }

    FormatContextPointer context{raw_context};
    const int stream_info_result =
        avformat_find_stream_info(context.get(), nullptr);
    if (stream_info_result < 0) {
        return ProbeResult{
            ProbeError::kStreamInfoFailed,
            "FFmpeg could not read stream metadata",
            ErrorText(stream_info_result),
            {},
        };
    }

    const int video_index = av_find_best_stream(
        context.get(), AVMEDIA_TYPE_VIDEO, -1, -1, nullptr, 0);
    if (video_index < 0) {
        return ProbeResult{
            ProbeError::kVideoStreamMissing,
            "The selected file does not contain a readable video stream",
            ErrorText(video_index),
            {},
        };
    }

    AVStream* video_stream = context->streams[video_index];
    const AVCodecParameters* video_parameters = video_stream->codecpar;

    const int audio_index = av_find_best_stream(
        context.get(), AVMEDIA_TYPE_AUDIO, -1, -1, nullptr, 0);
    const AVCodecParameters* audio_parameters = audio_index >= 0
        ? context->streams[audio_index]->codecpar
        : nullptr;

    const AVRational guessed_rate =
        av_guess_frame_rate(context.get(), video_stream, nullptr);
    const double frame_rate =
        guessed_rate.num > 0 && guessed_rate.den > 0
            ? av_q2d(guessed_rate)
            : 0.0;

    const char* format_name =
        context->iformat == nullptr ? nullptr : context->iformat->name;

    ProbeMetadata metadata;
    metadata.duration_ms = DurationMilliseconds(context.get(), video_stream);
    metadata.width = std::max(video_parameters->width, 0);
    metadata.height = std::max(video_parameters->height, 0);
    metadata.rotation_degrees = RotationDegrees(video_stream);
    metadata.frame_rate = frame_rate;
    metadata.bitrate = Bitrate(
        context.get(), video_parameters, audio_parameters);
    metadata.video_codec = CodecName(video_parameters);
    metadata.audio_codec = CodecName(audio_parameters);
    metadata.audio_sample_rate = audio_parameters == nullptr
        ? 0
        : std::max(audio_parameters->sample_rate, 0);
    metadata.audio_channels = audio_parameters == nullptr
        ? 0
        : std::max(audio_parameters->ch_layout.nb_channels, 0);
    metadata.container_format =
        format_name == nullptr ? std::string{} : std::string{format_name};

    return ProbeResult{
        ProbeError::kNone,
        "Video metadata is ready",
        {},
        std::move(metadata),
    };
#else
    (void)input_path;
    return ProbeResult{
        ProbeError::kUnavailable,
        "FFmpeg is not linked into this build",
        {},
        {},
    };
#endif
}

}  // namespace recapflow::ffmpeg
