package com.example.qwen2gguf

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

private const val TAG = "LlamaAndroid"

class LlamaAndroid {

    private var modelPtr: Long = 0
    private var ctxPtr:   Long = 0

    // Tracks how many tokens are already in the KV cache so each turn
    // only encodes the new portion of the prompt.
    private var nPast: Int = 0

    // Set to true after a GPU inference failure — used to surface a warning in UI
    var gpuFailed: Boolean = false
        private set

    // Path kept for CPU fallback reload
    private var loadedModelPath: String = ""
    private var loadedNCtx: Int = 2048
    private var loadedNThreads: Int = 6

    // Flip to true before freeing native pointers so the token callback
    // returns false and the native generation loop exits cleanly.
    @Volatile private var stopRequested: Boolean = false

    // ── native declarations ───────────────────────────────────────────────────

    private external fun nativeLoadModel(path: String, nGpuLayers: Int): Long
    private external fun nativeCreateContext(modelPtr: Long, nCtx: Int, nThreads: Int): Long
    private external fun nativeFreeContext(ctxPtr: Long)
    private external fun nativeFreeModel(modelPtr: Long)
    private external fun nativeClearCache(ctxPtr: Long)

    // Returns new nPast (prompt_tokens + generated_tokens), or -1 on GPU failure.
    // The callback returns true to continue generation, false to stop early.
    private external fun nativeGenerate(
        ctxPtr: Long,
        modelPtr: Long,
        prompt: String,
        nPast: Int,
        maxNewTokens: Int,
        temperature: Float,
        callback: (String) -> Boolean,
    ): Int

    private external fun nativeGetContextSize(ctxPtr: Long): Int

    // ── public API ────────────────────────────────────────────────────────────

    fun load(
        modelPath: String,
        nCtx: Int = 2048,
        nThreads: Int = 6,
        nGpuLayers: Int = 99,  // offload all layers to Vulkan GPU
    ) {
        if (modelPtr != 0L) {
            Log.w(TAG, "load() called while model already loaded — closing first")
            close()
        }
        loadedModelPath = modelPath
        loadedNCtx = nCtx
        loadedNThreads = nThreads
        gpuFailed = false

        modelPtr = nativeLoadModel(modelPath, nGpuLayers)
        require(modelPtr != 0L) { "Failed to load model from $modelPath" }
        ctxPtr = nativeCreateContext(modelPtr, nCtx, nThreads)
        require(ctxPtr != 0L) { "Failed to create context" }
        nPast = 0
        Log.i(TAG, "Loaded model with nGpuLayers=$nGpuLayers, ctx_size=${nativeGetContextSize(ctxPtr)}")
    }

    /** Reload the model on CPU only — called automatically after a GPU decode failure. */
    private fun reloadOnCpu() {
        Log.w(TAG, "GPU inference failed — reloading model on CPU (nGpuLayers=0)")
        if (ctxPtr   != 0L) { nativeFreeContext(ctxPtr);  ctxPtr   = 0 }
        if (modelPtr != 0L) { nativeFreeModel(modelPtr);  modelPtr = 0 }
        nPast = 0
        modelPtr = nativeLoadModel(loadedModelPath, 0)
        require(modelPtr != 0L) { "CPU fallback: failed to load model from $loadedModelPath" }
        ctxPtr = nativeCreateContext(modelPtr, loadedNCtx, loadedNThreads)
        require(ctxPtr != 0L) { "CPU fallback: failed to create context" }
        gpuFailed = true
        Log.i(TAG, "CPU fallback ready")
    }

    fun generate(
        prompt: String,
        maxNewTokens: Int = 512,
        temperature: Float = 0.7f,
    ): Flow<String> = callbackFlow {
        check(ctxPtr != 0L) { "Model not loaded" }
        stopRequested = false

        // Callback returns true to continue, false to stop (when close() was called).
        val result = nativeGenerate(
            ctxPtr       = ctxPtr,
            modelPtr     = modelPtr,
            prompt       = prompt,
            nPast        = nPast,
            maxNewTokens = maxNewTokens,
            temperature  = temperature,
        ) { piece ->
            if (stopRequested) return@nativeGenerate false
            trySend(piece)
            !stopRequested
        }

        if (!stopRequested && result == -1) {
            // GPU decode failed — reload on CPU and re-run the same prompt
            Log.w(TAG, "nativeGenerate returned -1 (GPU failure), falling back to CPU")
            reloadOnCpu()
            val cpuResult = nativeGenerate(
                ctxPtr       = ctxPtr,
                modelPtr     = modelPtr,
                prompt       = prompt,
                nPast        = 0,
                maxNewTokens = maxNewTokens,
                temperature  = temperature,
            ) { piece ->
                if (stopRequested) return@nativeGenerate false
                trySend(piece)
                !stopRequested
            }
            nPast = if (cpuResult >= 0) cpuResult else 0
        } else if (!stopRequested) {
            nPast = result
        }

        close()
        awaitClose()
    }.flowOn(Dispatchers.IO)

    /** Call when the conversation is cleared or the system prompt changes. */
    fun resetCache() {
        if (ctxPtr != 0L) nativeClearCache(ctxPtr)
        nPast = 0
    }

    fun close() {
        // Signal any running nativeGenerate callback to stop BEFORE freeing pointers.
        // This prevents a use-after-free SIGSEGV when the user closes the app mid-generation.
        stopRequested = true
        if (ctxPtr   != 0L) { nativeFreeContext(ctxPtr);  ctxPtr   = 0 }
        if (modelPtr != 0L) { nativeFreeModel(modelPtr);  modelPtr = 0 }
        nPast = 0
    }

    companion object {
        init { System.loadLibrary("llama-android") }
    }
}
