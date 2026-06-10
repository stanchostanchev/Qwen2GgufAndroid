plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.qwen2gguf"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.qwen2gguf"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-O3", "-DNDEBUG")
                arguments += listOf(
                    "-DLLAMA_BUILD_TESTS=OFF",
                    "-DLLAMA_BUILD_EXAMPLES=OFF",
                    "-DLLAMA_BUILD_SERVER=OFF",
                )
            }
        }
    }

    flavorDimensions += "gpu"

    productFlavors {
        // ── Samsung / Qualcomm Adreno — OpenCL with Adreno-optimised kernels ──────
        create("adreno") {
            dimension = "gpu"
            versionNameSuffix = "-adreno"
            externalNativeBuild {
                cmake {
                    arguments += listOf(
                        "-DGPU_BACKEND=adreno",
                        "-DGGML_OPENCL=ON",
                        "-DGGML_OPENCL_USE_ADRENO_KERNELS=ON",
                        "-DGGML_OPENCL_EMBED_KERNELS=ON",
                        "-DGGML_VULKAN=OFF",
                        "-DOpenCL_INCLUDE_DIR=/Users/stanchostanchev/Library/Android/sdk/ndk/27.0.12077973/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/include",
                        "-DOpenCL_LIBRARY=/Users/stanchostanchev/Library/Android/sdk/ndk/27.0.12077973/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/lib/aarch64-linux-android/libOpenCL.so",
                    )
                }
            }
        }

        // ── Google Pixel / ARM Mali — Vulkan backend ─────────────────────────────
        create("mali") {
            dimension = "gpu"
            versionNameSuffix = "-mali"
            externalNativeBuild {
                cmake {
                    arguments += listOf(
                        "-DGPU_BACKEND=mali",
                        "-DGGML_VULKAN=ON",
                        "-DGGML_OPENCL=OFF",
                        // SPIRV-Headers (Homebrew) — pass exact cmake config dir so cross-compile finds it
                        "-DSPIRV-Headers_DIR=/opt/homebrew/share/cmake/SPIRV-Headers",
                        "-DCMAKE_FIND_ROOT_PATH_MODE_PACKAGE=BOTH",
                    )
                }
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        // Store GGUF files uncompressed so AssetManager can openFd() them directly
        noCompress += listOf(".gguf", "gguf")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs.useLegacyPackaging = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)

    // Koog — agentic AI framework
    implementation(libs.koog.agents)
}
