# llama.cpp JNI — keep native method names
-keepclasseswithmembernames class com.example.qwen2gguf.LlamaAndroid {
    native <methods>;
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
