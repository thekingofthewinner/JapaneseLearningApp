package com.example.japaneselearningapp.tts

import android.util.Log

class VoicevoxBridge {
    external fun initialize(dictPath: String, onnxruntimePath: String): Boolean
    external fun loadVoiceModel(modelPath: String): Int
    external fun synthesis(text: String, styleId: Int): ByteArray?
    external fun release()

    companion object {
        private const val TAG = "VoicevoxBridge"
        private var loaded = false

        @Synchronized
        fun loadLibraries(): Boolean {
            if (loaded) {
                Log.d(TAG, "Libraries already loaded")
                return true
            }
            return try {
                Log.d(TAG, "Loading voicevox_onnxruntime...")
                System.loadLibrary("voicevox_onnxruntime")
                Log.d(TAG, "voicevox_onnxruntime loaded")

                Log.d(TAG, "Loading voicevox_core...")
                System.loadLibrary("voicevox_core")
                Log.d(TAG, "voicevox_core loaded")

                Log.d(TAG, "Loading voicevox_jni...")
                System.loadLibrary("voicevox_jni")
                Log.d(TAG, "voicevox_jni loaded")

                loaded = true
                Log.d(TAG, "All libraries loaded successfully")
                true
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load native library: ${e.message}", e)
                false
            }
        }
    }
}