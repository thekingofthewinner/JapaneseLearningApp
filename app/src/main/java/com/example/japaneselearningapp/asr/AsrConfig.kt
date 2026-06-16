package com.example.japaneselearningapp.asr

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import java.io.File
import java.io.FileOutputStream

object AsrConfig {
    private const val TAG = "AsrConfig"
    
    private var recognizer: OfflineRecognizer? = null
    private var isInitialized = false
    private var modelDir = ""
    
    // 采样率
    private const val SAMPLE_RATE = 16000
    
    // 模型文件名（ReazonSpeech 模型）
    private const val VAD_MODEL = "silero_vad.onnx"
    private const val ASR_MODEL = "model.onnx"  // ReazonSpeech 使用 model.onnx
    private const val ASR_TOKENS = "tokens.txt"
    
    interface RecognitionCallback {
        fun onPartialResult(text: String)
        fun onFinalResult(text: String, language: String)
        fun onError(error: String)
        fun onSpeechStart() {}
        fun onSpeechEnd() {}
    }
    
    /**
     * 初始化 ASR 引擎
     */
    fun initialize(
        context: Context,
        onComplete: (Boolean, String?) -> Unit
    ) {
            try {
                Log.d(TAG, "开始初始化 ASR 引擎...")
                
                // sherpa-onnx v1.13.x 需要从 assets 加载模型
                // 配置模型路径（相对于 assets 目录）
                val encoderPath = "asr/encoder.int8.onnx"
                val decoderPath = "asr/decoder.int8.onnx"
                val joinerPath = "asr/joiner.int8.onnx"
                val tokensPath = "asr/tokens.txt"
                
                // 检查 assets 中的模型文件是否存在
                try {
                    context.assets.open(encoderPath).close()
                    Log.d(TAG, "模型文件存在: $encoderPath")
                } catch (e: Exception) {
                    return
                }
                
                // 构建 ASR 配置（使用 assets 中的相对路径）
                val asrConfig = OfflineRecognizerConfig(
                    featConfig = FeatureConfig(
                        sampleRate = SAMPLE_RATE,
                        featureDim = 80
                    ),
                    modelConfig = OfflineModelConfig(
                        transducer = OfflineTransducerModelConfig(
                            encoder = encoderPath,
                            decoder = decoderPath,
                            joiner = joinerPath
                        ),
                        tokens = tokensPath,
                        numThreads = 2,
                        debug = true
                    )
                )
                
                Log.d(TAG, "开始创建 OfflineRecognizer...")
                
                // 创建 ASR 识别器（使用 assetManager 从 assets 加载）
                recognizer = OfflineRecognizer(
                    context.assets,
                    asrConfig
                )
                
                Log.d(TAG, "OfflineRecognizer 创建成功")
                
                isInitialized = true
                Log.d(TAG, "ASR 引擎初始化成功！")
                
                onComplete(true, null)
                
            } catch (e: Exception) {
                val errorMsg = "ASR 引擎初始化失败: ${e.message}"
                Log.e(TAG, errorMsg, e)
                onComplete(false, errorMsg)
            }
    }
    /**
     * 识别音频（简单版本，不使用 VAD）
     */
    fun recognizeAudio(
        samples: FloatArray,
        sampleRate: Int = SAMPLE_RATE,
        callback: RecognitionCallback
    ) {
        if (!isInitialized || recognizer == null) {
            callback.onError("ASR 引擎尚未初始化")
            return
        }
        
        try {
            val stream = recognizer!!.createStream()
            stream.acceptWaveform(samples, sampleRate)
            recognizer!!.decode(stream)
            
            val result = recognizer!!.getResult(stream)
            if (result.text.isNotEmpty()) {
                val language = detectLanguage(result.text)
                callback.onFinalResult(result.text, language)
            } else {
                callback.onFinalResult("", "ja")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "语音识别失败", e)
            callback.onError("语音识别失败: ${e.message}")
        }
    }
    
    /**
     * 识别音频片段（带 VAD）
     * 这是 recognizeWithVad 的别名，用于兼容 Live2DActivity 的调用
     */
    fun startRecognitionWithVad(
        samples: FloatArray,
        callback: RecognitionCallback
    ) {
        recognizeWithVad(samples, callback)
    }
    
    /**
     * 识别音频片段（带 VAD）
     */
    fun recognizeWithVad(
        samples: FloatArray,
        callback: RecognitionCallback
    ) {
        // 由于 VAD 实现较复杂，暂时使用简单的能量检测
        // TODO: 后续可以添加真正的 VAD 支持
        recognizeAudio(samples, callback = object : RecognitionCallback {
            override fun onPartialResult(text: String) {
                callback.onPartialResult(text)
            }

            override fun onFinalResult(text: String, language: String) {
                callback.onFinalResult(text, language)
            }

            override fun onError(error: String) {
                callback.onError(error)
            }

            override fun onSpeechStart() {
                callback.onSpeechStart()
            }

            override fun onSpeechEnd() {
                callback.onSpeechEnd()
            }
        })
    }
    
    /**
     * 简单的语言检测
     */
    private fun detectLanguage(text: String): String {
        val japaneseChars = Regex("[\\u3040-\\u30FF]")
        val chineseChars = Regex("[\\u4E00-\\u9FFF]")
        
        return when {
            japaneseChars.containsMatchIn(text) -> "ja"
            chineseChars.containsMatchIn(text) -> "zh"
            text.matches(Regex("[a-zA-Z\\s]+")) -> "en"
            else -> "ja"  // 默认日语
        }
    }
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            recognizer?.release()
            recognizer = null
            isInitialized = false
            Log.d(TAG, "ASR 引擎已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放 ASR 引擎时出错", e)
        }
    }
}