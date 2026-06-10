package com.example.qwen2gguf.di

import com.example.qwen2gguf.LlamaAndroid
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLlamaAndroid(): LlamaAndroid = LlamaAndroid()
}
