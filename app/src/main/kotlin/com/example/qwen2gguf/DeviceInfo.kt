package com.example.qwen2gguf

import android.os.Build

/**
 * Runtime GPU detection — used to decide nGpuLayers and UI labels.
 *
 * Both flavors attempt full GPU offload (nGpuLayers = 99).
 * The compiled backend differs per flavor:
 *   adreno flavor → OpenCL + Adreno kernels  (Samsung / Qualcomm)
 *   mali   flavor → Vulkan                   (Google Pixel / ARM Mali)
 *
 * If GPU inference fails at runtime, LlamaAndroid auto-reloads on CPU.
 */
object DeviceInfo {

    /** True on Qualcomm / Adreno devices (Samsung Galaxy etc.). */
    val isAdreno: Boolean by lazy {
        val hardware = Build.HARDWARE.lowercase()
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Build.SOC_MANUFACTURER.lowercase() else ""
        hardware.contains("qcom") ||
        soc.contains("qualcomm") ||
        soc.contains("qti")
    }

    /** True on Google Tensor / ARM Mali devices (Pixel etc.). */
    val isMali: Boolean by lazy {
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Build.SOC_MANUFACTURER.lowercase() else ""
        // Google Tensor chips: manufacturer is "Google"
        soc.contains("google") ||
        Build.BRAND.lowercase().contains("google") && !isAdreno
    }

    /**
     * Always attempt full GPU offload.
     * The correct backend (OpenCL or Vulkan) is already compiled into the flavor's .so.
     * LlamaAndroid falls back to CPU automatically if GPU init fails.
     */
    val defaultGpuLayers: Int = 99

    /** Human-readable backend label for the UI. */
    val gpuLabel: String get() = when {
        isAdreno -> "Adreno / OpenCL"
        isMali   -> "Mali / Vulkan"
        else     -> "Unknown GPU"
    }
}
