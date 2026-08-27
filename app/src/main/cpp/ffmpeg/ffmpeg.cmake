set(RECAPFLOW_FFMPEG_ABI "arm64-v8a")

function(recapflow_link_ffmpeg target_name)
    if(NOT ANDROID)
        message(FATAL_ERROR "RecapFlow FFmpeg integration is Android-only")
    endif()

    if(NOT ANDROID_ABI STREQUAL RECAPFLOW_FFMPEG_ABI)
        message(FATAL_ERROR
            "RecapFlow Phase 2 supports only ${RECAPFLOW_FFMPEG_ABI}; "
            "received ${ANDROID_ABI}")
    endif()

    # CMAKE_CURRENT_LIST_DIR has dynamic scope inside a function and resolves to
    # the caller (the parent CMakeLists.txt). Use the function definition's
    # directory so this module always finds ffmpeg/prebuilt correctly.
    set(ffmpeg_root
        "${CMAKE_CURRENT_FUNCTION_LIST_DIR}/prebuilt/${RECAPFLOW_FFMPEG_ABI}")
    set(ffmpeg_include_dir "${ffmpeg_root}/include")

    if(NOT EXISTS "${ffmpeg_include_dir}/libavutil/avutil.h")
        message(FATAL_ERROR
            "FFmpeg headers are missing from ${ffmpeg_include_dir}. "
            "Run scripts/build_ffmpeg_android_arm64.sh first.")
    endif()

    set(ffmpeg_components
        avfilter
        avformat
        avcodec
        swscale
        swresample
        avutil
    )

    set(ffmpeg_targets "")
    foreach(component IN LISTS ffmpeg_components)
        set(library_path "${ffmpeg_root}/lib/lib${component}.a")
        if(NOT EXISTS "${library_path}")
            message(FATAL_ERROR
                "Required FFmpeg library is missing: ${library_path}")
        endif()

        set(imported_target "recapflow_ffmpeg_${component}")
        add_library(${imported_target} STATIC IMPORTED GLOBAL)
        set_target_properties(
            ${imported_target}
            PROPERTIES IMPORTED_LOCATION "${library_path}"
        )
        list(APPEND ffmpeg_targets ${imported_target})
    endforeach()

    find_library(android_media_library mediandk REQUIRED)
    find_library(android_z_library z REQUIRED)
    find_library(android_math_library m REQUIRED)
    find_library(android_dl_library dl REQUIRED)

    target_include_directories(${target_name} PRIVATE "${ffmpeg_include_dir}")

    # FFmpeg's AArch64 assembly uses direct ADRP references to internal lookup
    # tables. When static FFmpeg archives are folded into libflowai.so, bind
    # those definitions inside this shared object so LLD does not treat them as
    # interposable and reject the relocation as non-PIC.
    target_link_options(${target_name} PRIVATE "-Wl,-Bsymbolic")

    # FFmpeg static archives contain circular references, so keep them in one
    # linker rescan group. Only libflowai.so is packaged into the APK.
    target_link_libraries(
        ${target_name}
        PRIVATE
        "-Wl,--start-group"
        ${ffmpeg_targets}
        "-Wl,--end-group"
        ${android_media_library}
        ${android_z_library}
        ${android_math_library}
        ${android_dl_library}
    )
endfunction()
