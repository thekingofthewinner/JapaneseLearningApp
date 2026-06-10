package com.example.japaneselearningapp.tts

import android.util.Log

/**
 * TTS 使用示例
 * 
 * 如何在应用中使用 TTS 功能：
 */
object TtsExample {
    private const val TAG = "TtsExample"
    
    /**
     * 示例 1：初始化 TTS 引擎
     */
    fun initializeExample(context: android.content.Context) {
        // 初始化 TTS 引擎
        TtsConfig.initialize(context) { success, error ->
            if (success) {
                Log.d(TAG, "TTS 引擎初始化成功！")
            } else {
                Log.e(TAG, "TTS 初始化失败: $error")
            }
        }
    }
    
    /**
     * 示例 2：生成日语语音
     */
    fun speakJapaneseExample(text: String) {
        // 直接播放日语语音
        TtsConfig.speakJapanese(text) { success, error ->
            if (success) {
                Log.d(TAG, "语音播放成功")
            } else {
                Log.e(TAG, "语音播放失败: $error")
            }
        }
    }
    
    /**
     * 示例 3：生成指定语言的语音
     */
    fun speakWithLanguageExample(text: String, language: String) {
        // 使用指定的语言生成语音
        TtsConfig.speak(
            text = text,
            language = language,
            voiceStyle = TtsConfig.VoiceStyles.DEFAULT,
            speed = 1.0f
        ) { success, error ->
            if (success) {
                Log.d(TAG, "语音播放成功")
            } else {
                Log.e(TAG, "语音播放失败: $error")
            }
        }
    }
    
    /**
     * 示例 4：调整语速
     */
    fun speakWithSpeedExample(text: String, speed: Float) {
        // speed: 0.5 = 慢速, 1.0 = 正常, 2.0 = 快速
        TtsConfig.speak(
            text = text,
            language = TtsConfig.DEFAULT_LANGUAGE,
            voiceStyle = TtsConfig.VoiceStyles.DEFAULT,
            speed = speed
        ) { success, error ->
            if (success) {
                Log.d(TAG, "语速调整语音播放成功")
            } else {
                Log.e(TAG, "语音播放失败: $error")
            }
        }
    }
    
    /**
     * 示例 5：切换语音风格
     */
    fun speakWithVoiceStyleExample(text: String, voiceStyle: Int) {
        // voiceStyle: 0-7，不同的风格（男声/女声/不同情感）
        TtsConfig.speak(
            text = text,
            language = TtsConfig.DEFAULT_LANGUAGE,
            voiceStyle = voiceStyle,
            speed = 1.0f
        ) { success, error ->
            if (success) {
                Log.d(TAG, "风格切换语音播放成功")
            } else {
                Log.e(TAG, "语音播放失败: $error")
            }
        }
    }
    
    /**
     * 示例 6：在 Live2DActivity 中使用 TTS
     */
    fun useInLive2DActivity(context: android.content.Context) {
        // 1. 在 Activity 中初始化
        TtsConfig.initialize(context) { success, _ ->
            if (success) {
                // 2. 当用户点击 Live2D 模型时，生成语音
                val japaneseText = "こんにちは！"  // 你好！
                TtsConfig.speakJapanese(japaneseText) { _, _ ->
                    // 语音播放完成
                }
            }
        }
    }
    
    /**
     * 示例 7：释放 TTS 资源
     */
    fun releaseExample() {
        // 当不需要使用 TTS 时，释放资源
        TtsConfig.release()
        Log.d(TAG, "TTS 资源已释放")
    }
    
    /**
     * 示例 8：检查 TTS 状态
     */
    fun checkStatusExample() {
        if (TtsConfig.isReady()) {
            Log.d(TAG, "TTS 引擎已就绪")
        } else {
            Log.d(TAG, "TTS 引擎未初始化")
        }
    }
}
