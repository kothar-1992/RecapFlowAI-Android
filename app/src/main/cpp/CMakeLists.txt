cmake_minimum_required(VERSION 3.18.1)

project(flowai LANGUAGES CXX)

option(
    RECAPFLOW_ENABLE_FFMPEG
    "Link the locally built ARM64 FFmpeg libraries"
    OFF
)

add_library(
    ${PROJECT_NAME}
    SHARED
    recapflow_jni.cpp
    ffmpeg/ffmpeg_probe.cpp
    ffmpeg/ffmpeg_runtime.cpp
    media/media_engine.cpp
    utils/native_log.cpp
)

target_compile_features(${PROJECT_NAME} PRIVATE cxx_std_17)

target_compile_options(
    ${PROJECT_NAME}
    PRIVATE
    -Wall
    -Wextra
    -Wpedantic
)

# NDK r24 needs explicit ELF segment alignment for 16 KB-page devices.
target_link_options(
    ${PROJECT_NAME}
    PRIVATE
    "-Wl,-z,max-page-size=16384"
    "-Wl,-z,common-page-size=16384"
)

target_include_directories(
    ${PROJECT_NAME}
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

find_library(android_log log REQUIRED)

target_link_libraries(
    ${PROJECT_NAME}
    PRIVATE
        android
        ${android_log}
)

if(RECAPFLOW_ENABLE_FFMPEG)
    include(${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/ffmpeg.cmake)
    recapflow_link_ffmpeg(${PROJECT_NAME})
    target_compile_definitions(${PROJECT_NAME} PRIVATE RECAPFLOW_HAS_FFMPEG=1)
else()
    target_compile_definitions(${PROJECT_NAME} PRIVATE RECAPFLOW_HAS_FFMPEG=0)
    message(STATUS "RecapFlow FFmpeg integration is disabled")
endif()
