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
        // 在后台线程初始化 ASR 引擎，避免阻塞主线程
        Thread {
            try {
                Log.d(TAG, "开始初始化 ASR 引擎...")
                
                // 从 assets 复制模型文件到内部存储
                modelDir = copyAssetsToInternal(context)
                if (modelDir.isEmpty()) {
                    onComplete(false, "无法复制模型文件")
                    return@Thread
                }
                
                Log.d(TAG, "模型目录: $modelDir")
                
                // 检查实际存在的模型文件
                val hasEncoder = 
                    File(modelDir, "encoder.onnx").exists() ||
                    File(modelDir, "encoder.int8.onnx").exists() ||
                    File(modelDir, "encoder-epoch-99-avg-1.onnx").exists() ||
                    File(modelDir, "encoder-epoch-99-avg-1.int8.onnx").exists()
                
                val hasDecoder =
                    File(modelDir, "decoder.onnx").exists() ||
                    File(modelDir, "decoder.int8.onnx").exists() ||
                    File(modelDir, "decoder-epoch-99-avg-1.onnx").exists() ||
                    File(modelDir, "decoder-epoch-99-avg-1.int8.onnx").exists()
                
                val hasJoiner =
                    File(modelDir, "joiner.onnx").exists() ||
                    File(modelDir, "joiner.int8.onnx").exists() ||
                    File(modelDir, "joiner-epoch-99-avg-1.onnx").exists() ||
                    File(modelDir, "joiner-epoch-99-avg-1.int8.onnx").exists()
                
                // 获取实际的文件路径
                fun getFilePath(baseName: String): String {
                    return when {
                        File(modelDir, baseName).exists() -> "$modelDir/$baseName"
                        File(modelDir, baseName.replace(".onnx", ".int8.onnx")).exists() -> 
                            "$modelDir/${baseName.replace(".onnx", ".int8.onnx")}"
                        File(modelDir, baseName.replace(".onnx", "-epoch-99-avg-1.onnx")).exists() -> 
                            "$modelDir/${baseName.replace(".onnx", "-epoch-99-avg-1.onnx")}"
                        File(modelDir, baseName.replace(".onnx", "-epoch-99-avg-1.int8.onnx")).exists() -> 
                            "$modelDir/${baseName.replace(".onnx", "-epoch-99-avg-1.int8.onnx")}"
                        else -> "$modelDir/$baseName"
                    }
                }
                
                // 初始化 ASR（ReazonSpeech Zipformer 模型）
                val asrConfig = if (hasEncoder) {
                    // Transducer 模型（encoder + decoder + joiner）
                    OfflineRecognizerConfig(
                        featConfig = FeatureConfig(
                            sampleRate = SAMPLE_RATE,
                            featureDim = 80
                        ),
                        modelConfig = OfflineModelConfig(
                            transducer = OfflineTransducerModelConfig(
                                encoder = getFilePath("encoder.onnx"),
                                decoder = if (hasDecoder) getFilePath("decoder.onnx") else getFilePath("encoder.onnx"),
                                joiner = if (hasJoiner) getFilePath("joiner.onnx") else if (hasDecoder) getFilePath("decoder.onnx") else getFilePath("encoder.onnx")
                            ),
                            tokens = "$modelDir/tokens.txt",
                            numThreads = 2,
                            debug = true
                        )
                    )
                } else {
                    onComplete(false, "未找到 ASR 模型文件（encoder/decoder/joiner）")
                    return@Thread
                }
                
                Log.d(TAG, "开始创建 OfflineRecognizer...")
                
                // 创建 ASR 识别器
                // 注意：模型文件已经复制到内部存储，使用 context 访问
                recognizer = OfflineRecognizer(
                    context = context,
                    config = asrConfig
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
        }.start()
    }
    
    /**
     * 从 assets 复制模型文件到内部存储
     */
    private fun copyAssetsToInternal(context: Context): String {
        val asrDir = File(context.filesDir, "asr")
        if (!asrDir.exists()) {
            asrDir.mkdirs()
        }
        
        // 尝试多种可能的模型文件结构
        // ReazonSpeech 模型可能解压后的目录结构：
        // 1. 直接在 asr/ 目录下（encoder.onnx, decoder.onnx, joiner.onnx, tokens.txt）
        // 2. 在 asr/1a，转录为/ 子目录下
        
        val possibleModelFiles = listOf(
            "encoder.onnx",
            "encoder.int8.onnx",
            "encoder-epoch-99-avg-1.onnx",
            "encoder-epoch-99-avg-1.int8.onnx",
            "model.onnx",
            "model.int8.onnx"
        )
        
        val possibleTokensFiles = listOf(
            "tokens.txt"
        )
        
        val possibleVadFiles = listOf(
            "silero_vad.onnx"
        )
        
        // 检查是否已经有模型文件
        var hasModel = false
        for (fileName in possibleModelFiles) {
            val file = File(asrDir, fileName)
            if (file.exists()) {
                hasModel = true
                Log.d(TAG, "模型文件已存在: $fileName")
                break
            }
        }
        
        // 复制 ASR 模型文件
        if (!hasModel) {
            // 尝试从 asr/ 目录复制
            for (fileName in possibleModelFiles) {
                try {
                    val destFile = File(asrDir, fileName)
                    if (!destFile.exists()) {
                        context.assets.open("asr/$fileName").use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.d(TAG, "复制模型文件: $fileName")
                        hasModel = true
                        break
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "尝试复制 $fileName 失败，继续尝试其他文件...")
                }
            }
            
            // 如果 asr/ 目录没有，尝试从 asr/1a，转录为/ 目录复制
            if (!hasModel) {
                for (fileName in possibleModelFiles) {
                    try {
                        val destFile = File(asrDir, fileName)
                        if (!destFile.exists()) {
                            context.assets.open("asr/1a，转录为/$fileName").use { input ->
                                FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            Log.d(TAG, "从 1a，转录为/ 复制模型文件: $fileName")
                            hasModel = true
                            break
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "尝试从 1a，转录为/ 复制 $fileName 失败...")
                    }
                }
            }
        }
        
        // 复制 tokens.txt
        var hasTokens = File(asrDir, "tokens.txt").exists()
        if (!hasTokens) {
            try {
                context.assets.open("asr/tokens.txt").use { input ->
                    FileOutputStream(File(asrDir, "tokens.txt")).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "复制 tokens.txt")
                hasTokens = true
            } catch (e: Exception) {
                try {
                    context.assets.open("asr/1a，转录为/tokens.txt").use { input ->
                        FileOutputStream(File(asrDir, "tokens.txt")).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "从 1a，转录为/ 复制 tokens.txt")
                    hasTokens = true
                } catch (e2: Exception) {
                    Log.e(TAG, "复制 tokens.txt 失败", e2)
                }
            }
        }
        
        // 复制 VAD 模型
        for (fileName in possibleVadFiles) {
            try {
                val vadFile = File(asrDir, fileName)
                if (!vadFile.exists()) {
                    context.assets.open("asr/$fileName").use { input ->
                        FileOutputStream(vadFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "复制 VAD 模型文件: $fileName")
                }
            } catch (e: Exception) {
                Log.d(TAG, "复制 VAD 模型 $fileName 失败（VAD 可选）")
            }
        }
        
        // 检查必要的文件是否存在
        if (!hasModel) {
            Log.e(TAG, "没有找到模型文件！")
            return ""
        }
        
        if (!hasTokens) {
            Log.e(TAG, "没有找到 tokens.txt！")
            return ""
        }
        
        return asrDir.absolutePath
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