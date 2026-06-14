package com.example.japaneselearningapp.asr

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import java.io.File
import java.io.FileOutputStream

object AsrConfig {
    private const val TAG = "AsrConfig"
    
    private var recognizer: OfflineRecognizer? = null
    private var vad: VoiceActivityDetector? = null
    private var isInitialized = false
    private var modelDir = ""
    
    // VAD 配置
    private const val VAD_SILENCE_DURATION = 0.8  // 静音持续时间（秒）
    private const val VAD_SPEECH_DURATION = 0.25  // 语音持续时间（秒）
    private const val SAMPLE_RATE = 16000
    
    // 模型文件名
    private const val VAD_MODEL = "silero_vad.onnx"
    private const val ASR_ENCODER = "encoder.int8.onnx"
    private const val ASR_DECODER = "decoder.int8.onnx"
    private const val ASR_TOKENS = "tokens.txt"
    
    interface RecognitionCallback {
        fun onPartialResult(text: String)
        fun onFinalResult(text: String, language: String)
        fun onError(error: String)
        fun onSpeechStart()
        fun onSpeechEnd()
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
            
            // 从 assets 复制模型文件到内部存储
            modelDir = copyAssetsToInternal(context)
            if (modelDir.isEmpty()) {
                onComplete(false, "无法复制模型文件")
                return
            }
            
            Log.d(TAG, "模型目录: $modelDir")
            
            // 初始化 VAD
            val vadConfig = SileroVadModelConfig(
                model = "$modelDir/$VAD_MODEL",
                threshold = 0.5f,
                minSilenceDuration = VAD_SILENCE_DURATION,
                minSpeechDuration = VAD_SPEECH_DURATION,
                windowSize = 512
            )
            
            vad = VoiceActivityDetector(vadConfig)
            Log.d(TAG, "VAD 初始化成功")
            
            // 初始化 ASR（SenseVoice 多语言模型）
            val asrConfig = OfflineRecognizerConfig(
                featConfig = FeatureConfig(
                    sampleRate = SAMPLE_RATE,
                    featureDim = 80
                ),
                modelConfig = OfflineModelConfig(
                    transducer = OfflineTransducerModelConfig(
                        encoder = "$modelDir/$ASR_ENCODER",
                        decoder = "$modelDir/$ASR_DECODER"
                    ),
                    tokens = "$modelDir/$ASR_TOKENS",
                    numThreads = 4,
                    debug = true
                )
            )
            
            recognizer = OfflineRecognizer(asrConfig)
            
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
     * 从 assets 复制模型文件到内部存储
     */
    private fun copyAssetsToInternal(context: Context): String {
        val asrDir = File(context.filesDir, "asr")
        if (!asrDir.exists()) {
            asrDir.mkdirs()
        }
        
        val modelFiles = listOf(VAD_MODEL, ASR_ENCODER, ASR_DECODER, ASR_TOKENS)
        
        for (fileName in modelFiles) {
            try {
                val inputFile = File(asrDir, fileName)
                if (!inputFile.exists()) {
                    context.assets.open("asr/$fileName").use { input ->
                        FileOutputStream(inputFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "复制模型文件: $fileName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "复制模型文件失败: $fileName", e)
                return ""
            }
        }
        
        return asrDir.absolutePath
    }
    
    /**
     * 开始语音识别（带 VAD）
     */
    fun startRecognitionWithVad(
        audioData: FloatArray,
        callback: RecognitionCallback
    ) {
        if (!isInitialized || recognizer == null || vad == null) {
            callback.onError("ASR 引擎尚未初始化")
            return
        }
        
        try {
            // 重置 VAD
            vad!!.reset()
            
            // 使用 VAD 检测语音段
            vad!!.acceptWaveform(audioData)
            
            var speechStarted = false
            var totalText = StringBuilder()
            
            while (!vad!!.isEmpty) {
                val speechSegment = vad!!.front
                vad!!.pop()
                
                if (speechSegment.isNotEmpty()) {
                    if (!speechStarted) {
                        speechStarted = true
                        callback.onSpeechStart()
                        Log.d(TAG, "检测到语音开始")
                    }
                    
                    // 对语音段进行识别
                    val stream = recognizer!!.createStream()
                    stream.acceptWaveform(speechSegment, SAMPLE_RATE)
                    recognizer!!.decode(stream)
                    
                    val result = recognizer!!.getResult(stream)
                    if (result.text.isNotEmpty()) {
                        totalText.append(result.text)
                        callback.onPartialResult(result.text)
                        Log.d(TAG, "识别结果: ${result.text}")
                    }
                }
            }
            
            if (speechStarted) {
                callback.onSpeechEnd()
                Log.d(TAG, "检测到语音结束")
            }
            
            // 返回最终结果
            if (totalText.isNotEmpty()) {
                val language = detectLanguage(totalText.toString())
                callback.onFinalResult(totalText.toString(), language)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "语音识别失败", e)
            callback.onError("语音识别失败: ${e.message}")
        }
    }
    
    /**
     * 简单的语言检测
     */
    private fun detectLanguage(text: String): String {
        val japaneseChars = Regex("[\\u3040-\\u30FF]")
        val chineseChars = Regex("[\\u4E00-\\u9FFF]")
        val koreanChars = Regex("[\\uAC00-\\uD7AF]")
        
        return when {
            japaneseChars.containsMatchIn(text) -> "ja"
            koreanChars.containsMatchIn(text) -> "ko"
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
        recognizer = null
        vad = null
        isInitialized = false
        Log.d(TAG, "ASR 引擎已释放")
    }
}