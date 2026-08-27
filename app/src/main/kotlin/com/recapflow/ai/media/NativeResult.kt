package com.recapflow.ai.media

enum class NativeErrorCode(val value: Int) {
    OK(0),
    NATIVE_LIBRARY_LOAD_FAILED(1000),
    INPUT_OPEN_FAILED(2000),
    UNSUPPORTED_CODEC(2001),
    INPUT_COPY_FAILED(2002),
    INVALID_INPUT(2003),
    ENCODER_NOT_FOUND(3000),
    FILTER_INIT_FAILED(4000),
    OUTPUT_CREATE_FAILED(5000),
    STORAGE_FULL(5001),
    CANCELLED(6000),
    NATIVE_ERROR(9000),

    ;

    companion object {
        fun fromValue(value: Int): NativeErrorCode =
            entries.firstOrNull { it.value == value } ?: NATIVE_ERROR
    }
}

enum class NativeStage(val value: Int) {
    INITIALIZATION(0),
    PROBE(1),
    VALIDATION(2),
    RENDER(3),
    CLEANUP(4),
    UNKNOWN(5),

    ;

    companion object {
        fun fromValue(value: Int): NativeStage =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}

data class NativeResult<out T>(
    val code: NativeErrorCode,
    val stage: NativeStage,
    val message: String,
    val ffmpegError: String? = null,
    val recoverable: Boolean = false,
    val value: T? = null,
) {
    val isSuccess: Boolean
        get() = code == NativeErrorCode.OK

    companion object {
        fun <T> success(
            value: T,
            stage: NativeStage,
            message: String,
        ): NativeResult<T> = NativeResult(
            code = NativeErrorCode.OK,
            stage = stage,
            message = message,
            value = value,
        )

        fun failure(
            code: NativeErrorCode,
            stage: NativeStage,
            message: String,
            ffmpegError: String? = null,
            recoverable: Boolean = false,
        ): NativeResult<Nothing> = NativeResult(
            code = code,
            stage = stage,
            message = message,
            ffmpegError = ffmpegError,
            recoverable = recoverable,
        )
    }
}
