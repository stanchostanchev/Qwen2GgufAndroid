package com.example.qwen2gguf

import android.content.Context
import android.util.Log
import java.io.File

private const val TAG = "AssetExtractor"

/**
 * Copies a file from assets to filesDir if it isn't already there (or the size differs).
 * Returns the absolute path of the extracted file.
 */
object AssetExtractor {

    fun extract(context: Context, assetName: String): String {
        val dest = File(context.filesDir, assetName)

        val assetSize = context.assets.openFd(assetName).use { it.length }
        if (dest.exists() && dest.length() == assetSize) {
            Log.i(TAG, "Already extracted: ${dest.absolutePath}")
            return dest.absolutePath
        }

        Log.i(TAG, "Extracting $assetName (${assetSize / 1_048_576} MB) …")
        context.assets.open(assetName).use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output, bufferSize = 4 * 1024 * 1024)
            }
        }
        Log.i(TAG, "Extracted to ${dest.absolutePath}")
        return dest.absolutePath
    }
}
