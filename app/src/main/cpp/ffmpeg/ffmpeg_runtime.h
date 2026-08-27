#ifndef RECAPFLOW_FFMPEG_RUNTIME_H
#define RECAPFLOW_FFMPEG_RUNTIME_H

#include <string>

namespace recapflow::ffmpeg {

[[nodiscard]] bool IsLinked() noexcept;
[[nodiscard]] std::string LinkedVersion();

}  // namespace recapflow::ffmpeg

#endif  // RECAPFLOW_FFMPEG_RUNTIME_H
