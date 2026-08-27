#include <jni.h>

#include <exception>
#include <string>

#include "media/media_engine.h"
#include "utils/native_log.h"

namespace {

constexpr char kProbePayloadClass[] =
    "com/recapflow/ai/media/NativeProbePayload";
constexpr char kProbePayloadConstructor[] =
    "(IILjava/lang/String;Ljava/lang/String;ZJIIIDJ"
    "Ljava/lang/String;Ljava/lang/String;IILjava/lang/String;)V";

void ThrowIllegalState(JNIEnv* env, const std::string& message) {
    jclass exception_class = env->FindClass("java/lang/IllegalStateException");
    if (exception_class != nullptr) {
        env->ThrowNew(exception_class, message.c_str());
    }
}

jstring NewString(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

jobject NewProbePayload(
    JNIEnv* env,
    const recapflow::media::MediaProbeResult& probe) {
    jclass payload_class = env->FindClass(kProbePayloadClass);
    if (payload_class == nullptr) {
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(
        payload_class,
        "<init>",
        kProbePayloadConstructor);
    if (constructor == nullptr) {
        env->DeleteLocalRef(payload_class);
        return nullptr;
    }

    jstring message = NewString(env, probe.result.message);
    jstring ffmpeg_error = probe.result.ffmpeg_error.empty()
        ? nullptr
        : NewString(env, probe.result.ffmpeg_error);
    jstring video_codec = NewString(env, probe.media_info.video_codec);
    jstring audio_codec = probe.media_info.audio_codec.empty()
        ? nullptr
        : NewString(env, probe.media_info.audio_codec);
    jstring container_format =
        NewString(env, probe.media_info.container_format);

    jobject payload = env->NewObject(
        payload_class,
        constructor,
        static_cast<jint>(probe.result.code),
        static_cast<jint>(probe.result.stage),
        message,
        ffmpeg_error,
        static_cast<jboolean>(probe.result.recoverable),
        static_cast<jlong>(probe.media_info.duration_ms),
        static_cast<jint>(probe.media_info.width),
        static_cast<jint>(probe.media_info.height),
        static_cast<jint>(probe.media_info.rotation_degrees),
        static_cast<jdouble>(probe.media_info.frame_rate),
        static_cast<jlong>(probe.media_info.bitrate),
        video_codec,
        audio_codec,
        static_cast<jint>(probe.media_info.audio_sample_rate),
        static_cast<jint>(probe.media_info.audio_channels),
        container_format);

    env->DeleteLocalRef(container_format);
    if (audio_codec != nullptr) {
        env->DeleteLocalRef(audio_codec);
    }
    env->DeleteLocalRef(video_codec);
    if (ffmpeg_error != nullptr) {
        env->DeleteLocalRef(ffmpeg_error);
    }
    env->DeleteLocalRef(message);
    env->DeleteLocalRef(payload_class);
    return payload;
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_recapflow_ai_media_NativeMediaBridge_nativeVersionFromJni(
    JNIEnv* env,
    jobject /* bridge */) {
    try {
        const recapflow::media::NativeResult health =
            recapflow::media::MediaEngine::HealthCheck();
        if (!health.ok()) {
            recapflow::log::Error(recapflow::log::kNativeTag, health.message);
            ThrowIllegalState(env, health.message);
            return nullptr;
        }

        const std::string version = recapflow::media::MediaEngine::NativeVersion();
        recapflow::log::Info(
            recapflow::log::kNativeTag,
            "Native bridge initialized: " + version);
        return env->NewStringUTF(version.c_str());
    } catch (const std::exception& error) {
        recapflow::log::Error(recapflow::log::kNativeTag, error.what());
        ThrowIllegalState(env, error.what());
        return nullptr;
    } catch (...) {
        constexpr char kUnknownError[] = "Unknown native initialization failure";
        recapflow::log::Error(recapflow::log::kNativeTag, kUnknownError);
        ThrowIllegalState(env, kUnknownError);
        return nullptr;
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_recapflow_ai_media_NativeMediaBridge_probeFromJni(
    JNIEnv* env,
    jobject /* bridge */,
    jstring input_path) {
    try {
        std::string path;
        if (input_path != nullptr) {
            const char* characters =
                env->GetStringUTFChars(input_path, nullptr);
            if (characters == nullptr) {
                return nullptr;
            }
            path.assign(characters);
            env->ReleaseStringUTFChars(input_path, characters);
        }

        const recapflow::media::MediaProbeResult probe =
            recapflow::media::MediaEngine::Probe(path);
        if (!probe.result.ok()) {
            recapflow::log::Error(
                recapflow::log::kNativeTag,
                "Media probe failed: " + probe.result.message);
        } else {
            recapflow::log::Info(
                recapflow::log::kNativeTag,
                "Media probe completed");
        }
        return NewProbePayload(env, probe);
    } catch (const std::exception& error) {
        recapflow::log::Error(recapflow::log::kNativeTag, error.what());
        ThrowIllegalState(env, error.what());
        return nullptr;
    } catch (...) {
        constexpr char kUnknownError[] = "Unknown native media probe failure";
        recapflow::log::Error(recapflow::log::kNativeTag, kUnknownError);
        ThrowIllegalState(env, kUnknownError);
        return nullptr;
    }
}
