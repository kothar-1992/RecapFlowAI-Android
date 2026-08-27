#include "utils/native_log.h"

#include <android/log.h>

namespace recapflow::log {
namespace {

void Write(android_LogPriority priority, const char* tag, const std::string& message) {
    __android_log_write(priority, tag, message.c_str());
}

}  // namespace

void Debug(const char* tag, const std::string& message) {
    Write(ANDROID_LOG_DEBUG, tag, message);
}

void Info(const char* tag, const std::string& message) {
    Write(ANDROID_LOG_INFO, tag, message);
}

void Warn(const char* tag, const std::string& message) {
    Write(ANDROID_LOG_WARN, tag, message);
}

void Error(const char* tag, const std::string& message) {
    Write(ANDROID_LOG_ERROR, tag, message);
}

}  // namespace recapflow::log
