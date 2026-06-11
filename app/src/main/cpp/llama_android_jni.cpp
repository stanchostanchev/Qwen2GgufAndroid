#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <stdexcept>
#include <cstdlib>   // setenv
#include <sstream>
#include <streambuf>
#include <iostream>

#include "llama.h"

#define TAG "LlamaAndroid"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Route llama.cpp / ggml log callbacks → Android logcat (covers ggml_vulkan init msgs).
static void llama_log_callback_android(ggml_log_level level, const char* text, void*) {
    int android_level = ANDROID_LOG_DEBUG;
    if (level == GGML_LOG_LEVEL_ERROR)   android_level = ANDROID_LOG_ERROR;
    else if (level == GGML_LOG_LEVEL_WARN) android_level = ANDROID_LOG_WARN;
    else if (level == GGML_LOG_LEVEL_INFO) android_level = ANDROID_LOG_INFO;
    // Strip trailing newline for cleaner logcat lines
    std::string msg(text);
    if (!msg.empty() && msg.back() == '\n') msg.pop_back();
    if (!msg.empty())
        __android_log_print(android_level, "ggml", "%s", msg.c_str());
}

// Redirect std::cerr lines → Android logcat so we can see llama.cpp error messages
// (on Android std::cerr goes nowhere by default).
class AndroidCerrBuf : public std::streambuf {
    std::string line_;
protected:
    int overflow(int c) override {
        if (c == '\n') {
            __android_log_print(ANDROID_LOG_WARN, TAG, "[stderr] %s", line_.c_str());
            line_.clear();
        } else if (c != EOF) {
            line_ += static_cast<char>(c);
        }
        return c;
    }
};
static AndroidCerrBuf g_cerr_buf;
static struct CerrRedirect {
    CerrRedirect() { std::cerr.rdbuf(&g_cerr_buf); }
} g_cerr_redirect;

static std::string jstring_to_str(JNIEnv* env, jstring jstr) {
    if (!jstr) return {};
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

// ── model / context lifecycle ─────────────────────────────────────────────────

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_qwen2gguf_LlamaAndroid_nativeLoadModel(
        JNIEnv* env, jobject, jstring model_path, jint n_gpu_layers) {
    llama_backend_init();
    llama_log_set(llama_log_callback_android, nullptr);
    llama_numa_init(GGML_NUMA_STRATEGY_DISABLED);
    auto params = llama_model_default_params();
    params.n_gpu_layers = static_cast<int>(n_gpu_layers);
    std::string path = jstring_to_str(env, model_path);
    llama_model* model = llama_model_load_from_file(path.c_str(), params);
    if (!model) { LOGE("Failed to load model from %s", path.c_str()); return 0; }
    LOGI("Model loaded: %s", path.c_str());
    return reinterpret_cast<jlong>(model);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_qwen2gguf_LlamaAndroid_nativeCreateContext(
        JNIEnv*, jobject, jlong model_ptr, jint n_ctx, jint n_threads) {
    auto* model = reinterpret_cast<llama_model*>(model_ptr);
    auto params = llama_context_default_params();
    params.n_ctx           = static_cast<uint32_t>(n_ctx);
    params.n_threads       = static_cast<uint32_t>(n_threads);
    params.n_threads_batch = params.n_threads;
    llama_context* ctx = llama_init_from_model(model, params);
    if (!ctx) { LOGE("Failed to create context"); return 0; }
    LOGI("Context created (n_ctx=%d, threads=%d)", n_ctx, n_threads);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_qwen2gguf_LlamaAndroid_nativeFreeContext(JNIEnv*, jobject, jlong ctx_ptr) {
    if (ctx_ptr) llama_free(reinterpret_cast<llama_context*>(ctx_ptr));
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_qwen2gguf_LlamaAndroid_nativeFreeModel(JNIEnv*, jobject, jlong model_ptr) {
    if (model_ptr) {
        llama_model_free(reinterpret_cast<llama_model*>(model_ptr));
        llama_backend_free();
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_qwen2gguf_LlamaAndroid_nativeClearCache(JNIEnv*, jobject, jlong ctx_ptr) {
    auto* ctx = reinterpret_cast<llama_context*>(ctx_ptr);
    llama_memory_t mem = llama_get_memory(ctx);
    if (mem) llama_memory_clear(mem, true);
    LOGI("KV cache cleared");
}

// ── inference ─────────────────────────────────────────────────────────────────
//
// Returns the new nPast value (prompt_tokens + generated_tokens).
// The caller should pass the previous nPast so we skip re-encoding tokens
// that are already in the KV cache — this is the key speedup.

extern "C" JNIEXPORT jint JNICALL
Java_com_example_qwen2gguf_LlamaAndroid_nativeGenerate(
        JNIEnv*  env,
        jobject,
        jlong    ctx_ptr,
        jlong    model_ptr,
        jstring  prompt_jstr,
        jint     n_past,        // tokens already in KV cache
        jint     max_new_tokens,
        jfloat   temperature,
        jobject  callback) {

    auto* ctx   = reinterpret_cast<llama_context*>(ctx_ptr);
    auto* model = reinterpret_cast<llama_model*>(model_ptr);
    const llama_vocab* vocab = llama_model_get_vocab(model);

    std::string prompt = jstring_to_str(env, prompt_jstr);

    // Tokenise full prompt
    const int n_ctx_max = static_cast<int>(llama_n_ctx(ctx));
    std::vector<llama_token> tokens(n_ctx_max);
    int n_tokens = llama_tokenize(
            vocab,
            prompt.c_str(), static_cast<int>(prompt.size()),
            tokens.data(), static_cast<int>(tokens.size()),
            true, true);
    if (n_tokens < 0) { LOGE("Tokenisation failed (%d)", n_tokens); return n_past; }
    tokens.resize(n_tokens);

    // Only encode the NEW tokens (those not yet in the KV cache)
    int n_new = n_tokens - n_past;
    LOGI("Prompt total=%d, cached=%d, new=%d", n_tokens, n_past, n_new);

    if (n_new > 0) {
        llama_batch batch = llama_batch_init(n_new, 0, 1);

        auto batch_add = [&](llama_token id, llama_pos pos, bool logits) {
            batch.token   [batch.n_tokens] = id;
            batch.pos     [batch.n_tokens] = pos;
            batch.n_seq_id[batch.n_tokens] = 1;
            batch.seq_id  [batch.n_tokens][0] = 0;
            batch.logits  [batch.n_tokens] = logits ? 1 : 0;
            batch.n_tokens++;
        };

        for (int i = 0; i < n_new; i++) {
            batch_add(tokens[n_past + i], n_past + i, i == n_new - 1);
        }

        int decode_ret = 0;
        try {
            decode_ret = llama_decode(ctx, batch);
        } catch (const std::exception& e) {
            LOGE("llama_decode (prefill) threw exception: %s", e.what());
            llama_batch_free(batch);
            return -1;  // signal failure to Kotlin
        } catch (...) {
            LOGE("llama_decode (prefill) threw unknown exception");
            llama_batch_free(batch);
            return -1;
        }
        if (decode_ret != 0) {
            LOGE("llama_decode (prefill) failed (ret=%d)", decode_ret);
            llama_batch_free(batch);
            return -1;
        }
        llama_batch_free(batch);
    }

    // Single-token generation batch (reused each step)
    llama_batch gen_batch = llama_batch_init(1, 0, 1);
    auto batch_add_single = [&](llama_token id, llama_pos pos) {
        gen_batch.n_tokens = 0;
        gen_batch.token   [0] = id;
        gen_batch.pos     [0] = pos;
        gen_batch.n_seq_id[0] = 1;
        gen_batch.seq_id  [0][0] = 0;
        gen_batch.logits  [0] = 1;
        gen_batch.n_tokens = 1;
    };

    // Callback — returns Boolean (true = continue, false = stop)
    jclass    cb_class   = env->GetObjectClass(callback);
    jmethodID invoke_id  = env->GetMethodID(cb_class, "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;");
    jclass    bool_class = env->FindClass("java/lang/Boolean");
    jmethodID bool_value = env->GetMethodID(bool_class, "booleanValue", "()Z");

    // Sampler — mirrors the HuggingFace inference params that produce good output:
    //   repetition_penalty=1.1, top_p=0.92, temperature=0.8
    // Penalty 1.3 was too aggressive and prevented </think> from being generated.
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler* smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_penalties(64, 1.1f, 0.0f, 0.0f));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.92f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    const llama_token eos = llama_vocab_eos(vocab);
    int n_pos = n_tokens;
    int n_generated = 0;

    for (int i = 0; i < static_cast<int>(max_new_tokens); i++) {
        llama_token new_token = llama_sampler_sample(smpl, ctx, -1);
        llama_sampler_accept(smpl, new_token);
        if (new_token == eos) break;

        char piece[256] = {};
        int piece_len = llama_token_to_piece(vocab, new_token, piece, sizeof(piece) - 1, 0, true);
        if (piece_len > 0) {
            piece[piece_len] = '\0';
            jstring jpiece   = env->NewStringUTF(piece);
            jobject cb_ret   = env->CallObjectMethod(callback, invoke_id, jpiece);
            env->DeleteLocalRef(jpiece);
            // Callback returns false when stopRequested — abort the generation loop.
            bool should_continue = (cb_ret != nullptr) &&
                                   env->CallBooleanMethod(cb_ret, bool_value);
            if (cb_ret) env->DeleteLocalRef(cb_ret);
            if (!should_continue) {
                LOGD("nativeGenerate: stop requested via callback, breaking");
                break;
            }
        }

        batch_add_single(new_token, n_pos++);
        int step_ret = 0;
        try {
            step_ret = llama_decode(ctx, gen_batch);
        } catch (const std::exception& e) {
            LOGE("llama_decode (step %d) threw exception: %s", i, e.what());
            llama_sampler_free(smpl);
            llama_batch_free(gen_batch);
            return -1;
        } catch (...) {
            LOGE("llama_decode (step %d) threw unknown exception", i);
            llama_sampler_free(smpl);
            llama_batch_free(gen_batch);
            return -1;
        }
        if (step_ret != 0) {
            LOGE("llama_decode (step %d) failed (ret=%d)", i, step_ret);
            break;
        }
        n_generated++;
    }

    llama_sampler_free(smpl);
    llama_batch_free(gen_batch);

    return n_tokens + n_generated; // new nPast for next call
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_qwen2gguf_LlamaAndroid_nativeGetContextSize(JNIEnv*, jobject, jlong ctx_ptr) {
    return static_cast<jint>(llama_n_ctx(reinterpret_cast<llama_context*>(ctx_ptr)));
}
