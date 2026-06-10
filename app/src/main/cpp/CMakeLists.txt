cmake_minimum_required(VERSION 3.22.1)
project(qwen2gguf)

# ── llama.cpp ──────────────────────────────────────────────────────────────────
# Clone llama.cpp into this directory before building:
#   cd app/src/main/cpp
#   git clone https://github.com/ggerganov/llama.cpp
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

if(NOT EXISTS ${LLAMA_DIR}/CMakeLists.txt)
    message(FATAL_ERROR
        "llama.cpp source not found at ${LLAMA_DIR}.\n"
        "Run: cd app/src/main/cpp && git clone https://github.com/ggerganov/llama.cpp"
    )
endif()

set(LLAMA_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER   OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS    OFF CACHE BOOL "" FORCE)

# GPU backend is selected by the Gradle product flavor via -DGPU_BACKEND=<adreno|mali>.
# Defaults to adreno (OpenCL) if not specified.
if(NOT DEFINED GPU_BACKEND)
    set(GPU_BACKEND "adreno")
endif()

if(GPU_BACKEND STREQUAL "adreno")
    message(STATUS "GPU backend: OpenCL (Adreno/Qualcomm)")
    set(GGML_VULKAN                    OFF CACHE BOOL "" FORCE)
    set(GGML_OPENCL                    ON  CACHE BOOL "" FORCE)
    set(GGML_OPENCL_USE_ADRENO_KERNELS ON  CACHE BOOL "" FORCE)
    set(GGML_OPENCL_EMBED_KERNELS      ON  CACHE BOOL "" FORCE)
elseif(GPU_BACKEND STREQUAL "mali")
    message(STATUS "GPU backend: Vulkan (Mali/Pixel)")
    set(GGML_OPENCL OFF CACHE BOOL "" FORCE)
    set(GGML_OPENCL_USE_ADRENO_KERNELS OFF CACHE BOOL "" FORCE)
    set(GGML_OPENCL_EMBED_KERNELS      OFF CACHE BOOL "" FORCE)
    set(GGML_VULKAN ON  CACHE BOOL "" FORCE)
    # vulkan.hpp (C++ wrapper) is not in the Android NDK sysroot.
    # Use Homebrew vulkan-headers which includes it.
    include_directories(/opt/homebrew/include)
endif()

add_subdirectory(${LLAMA_DIR} llama_build)

# ── JNI bridge ────────────────────────────────────────────────────────────────
add_library(llama-android SHARED llama_android_jni.cpp)

target_include_directories(llama-android PRIVATE
    ${LLAMA_DIR}/include
    ${LLAMA_DIR}/ggml/include
)

target_link_libraries(llama-android
    llama
    android
    log
    # OpenCL linked transitively via ggml-opencl → llama (adreno flavor only)
    # Vulkan linked transitively via ggml-vulkan → llama (mali flavor only)
)

target_compile_options(llama-android PRIVATE
    -std=c++17
    -O3
    -DNDEBUG
    -ffast-math
    # Enable NEON on arm64 for vectorised matrix ops
    $<$<STREQUAL:${ANDROID_ABI},arm64-v8a>:-march=armv8.4-a+dotprod+fp16>
)
