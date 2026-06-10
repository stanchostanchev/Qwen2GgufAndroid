package com.example.qwen2gguf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.qwen2gguf.ui.ChatScreen
import com.example.qwen2gguf.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

// Asset filename must match the file placed in app/src/main/assets/
private const val MODEL_ASSET = "qwen2-0_5b-instruct-q4_k_m.gguf"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                val context = LocalContext.current
                // Pass null until the ViewModel drives loading; the asset name is enough.
                ChatScreen(
                    modelAssetName = MODEL_ASSET,
                )
            }
        }
    }
}
