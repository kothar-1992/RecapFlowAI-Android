#include "ffmpeg/ffmpeg_runtime.h"

#ifndef RECAPFLOW_HAS_FFMPEG
#define RECAPFLOW_HAS_FFMPEG 0
#endif

#if RECAPFLOW_HAS_FFMPEG
extern "C" {
#include <libavutil/avutil.h>
}
#endif

namespace recapflow::ffmpeg {

bool IsLinked() noexcept {
    return RECAPFLOW_HAS_FFMPEG == 1;
}

std::string LinkedVersion() {
#if RECAPFLOW_HAS_FFMPEG
    const char* version = av_version_info();
    return version == nullptr ? std::string{} : std::string{version};
#else
    return {};
#endif
}

}  // namespace recapflow::ffmpeg
