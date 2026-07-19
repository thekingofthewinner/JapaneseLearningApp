package com.example.japaneselearningapp.tts

class VoicevoxBridge {
    external fun initialize(dictPath: String, onnxruntimePath: String): Boolean
    external fun loadVoiceModel(modelPath: String): Int
    external fun synthesis(text: String, styleId: Int): ByteArray?
    external fun release()

    companion object {
        private var loaded = false

        fun loadLibraries() {
            if (!loaded) {
                System.loadLibrary("voicevox_core")
                System.loadLibrary("voicevox_onnxruntime")
                System.loadLibrary("voicevox_jni")
                loaded = true
            }
        }
    }
}