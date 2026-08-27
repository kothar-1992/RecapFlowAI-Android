#ifndef RECAPFLOW_NATIVE_LOG_H
#define RECAPFLOW_NATIVE_LOG_H

#include <string>

namespace recapflow::log {

inline constexpr char kNativeTag[] = "RecapFlowNative";
inline constexpr char kRenderTag[] = "RecapFlowRender";
inline constexpr char kStorageTag[] = "RecapFlowStorage";
inline constexpr char kAiTag[] = "RecapFlowAI";

void Debug(const char* tag, const std::string& message);
void Info(const char* tag, const std::string& message);
void Warn(const char* tag, const std::string& message);
void Error(const char* tag, const std::string& message);

}  // namespace recapflow::log

#endif  // RECAPFLOW_NATIVE_LOG_H
