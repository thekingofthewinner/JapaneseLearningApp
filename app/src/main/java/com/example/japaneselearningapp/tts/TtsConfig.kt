package com.example.japaneselearningapp.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import java.io.File
import java.io.FileOutputStream

object TtsConfig {
    private const val TAG = "TtsConfig"
    
    private const val TTS_ASSET_DIR = "ttsKokoro"
    
    const val DEFAULT_LANGUAGE = "ja"
    
    val SUPPORTED_LANGUAGES = mapOf(
        "ja" to "日语",
        "en" to "英语",
        "zh" to "中文",
        "es" to "西班牙语",
        "fr" to "法语",
        "de" to "德语",
        "it" to "意大利语",
        "pt" to "葡萄牙语",
        "ko" to "韩语",
        "hi" to "印地语"
    )
    
    object JapaneseVoices {
        const val FEMALE_ALPHA = 6
        const val MALE_OMEGA = 7
        const val DEFAULT = FEMALE_ALPHA
    }
    
    private var ttsEngine: OfflineTts? = null
    private var isInitialized = false
    private var isPlaying = false
    
    fun initialize(context: Context, onComplete: (Boolean, String?) -> Unit) {
        try {
            Log.d(TAG, "开始初始化 Kokoro TTS 引擎...")
            
            val filesDir = context.filesDir
            val destDir = File(filesDir, TTS_ASSET_DIR)
            
            Log.d(TAG, "目标目录: ${destDir.absolutePath}")
            
            val requiredFiles = listOf(
                "$TTS_ASSET_DIR/model.int8.onnx",
                "$TTS_ASSET_DIR/voices.bin",
                "$TTS_ASSET_DIR/tokens.txt"
            )
            
            for (filePath in requiredFiles) {
                try {
                    context.assets.open(filePath).close()
                    Log.d(TAG, "模型文件存在: $filePath")
                } catch (e: Exception) {
                    onComplete(false, "未找到 TTS 模型文件: $filePath")
                    return
                }
            }
            
            val hasDataDir = try {
                context.assets.list("$TTS_ASSET_DIR/espeak-ng-data")?.isNotEmpty() == true
            } catch (e: Exception) { false }
            
            val hasDictDir = try {
                context.assets.list("$TTS_ASSET_DIR/dict")?.isNotEmpty() == true
            } catch (e: Exception) { false }
            
            Log.d(TAG, "espeak-ng-data: $hasDataDir, dict: $hasDictDir")
            
            var dataDirPath = ""
            var dictDirPath = ""
            
            if (hasDataDir) {
                val dataDestDir = File(destDir, "espeak-ng-data")
                copyAssetDir(context, "$TTS_ASSET_DIR/espeak-ng-data", dataDestDir)
                dataDirPath = dataDestDir.absolutePath
                Log.d(TAG, "espeak-ng-data 复制完成: $dataDirPath")
            }
            
            if (hasDictDir) {
                val dictDestDir = File(destDir, "dict")
                copyAssetDir(context, "$TTS_ASSET_DIR/dict", dictDestDir)
                dictDirPath = dictDestDir.absolutePath
                Log.d(TAG, "dict 复制完成: $dictDirPath")
            }
            
            val modelConfig = OfflineTtsKokoroModelConfig(
                model = "$TTS_ASSET_DIR/model.int8.onnx",
                voices = "$TTS_ASSET_DIR/voices.bin",
                tokens = "$TTS_ASSET_DIR/tokens.txt",
                lengthScale = 1.0f,
                dataDir = dataDirPath,
                dictDir = dictDirPath
            )
            
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    kokoro = modelConfig,
                    numThreads = 2,
                    debug = true
                )
            )
            
            Log.d(TAG, "开始创建 OfflineTts (Kokoro)...")
            Log.d(TAG, "model=$TTS_ASSET_DIR/model.int8.onnx")
            Log.d(TAG, "dataDir=$dataDirPath")
            Log.d(TAG, "dictDir=$dictDirPath")
            
            try {
                System.loadLibrary("sherpa-onnx-jni")
                Log.d(TAG, "sherpa-onnx native 库加载成功")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "sherpa-onnx native 库加载失败", e)
                onComplete(false, "Native 库加载失败: ${e.message}")
                return
            }
            
            try {
                ttsEngine = OfflineTts(context.assets, config)
                Log.d(TAG, "OfflineTts 创建成功")
            } catch (e: Throwable) {
                val errorMsg = "创建 OfflineTts 失败: ${e.message}\n模型文件可能损坏或不兼容"
                Log.e(TAG, errorMsg, e)
                if (e is UnsatisfiedLinkError) {
                    Log.e(TAG, "Native 库加载失败！请检查 .so 文件是否正确打包", e)
                }
                ttsEngine = null
                onComplete(false, errorMsg)
                return
            }
            
            isInitialized = true
            Log.d(TAG, "Kokoro TTS 引擎初始化成功！")
            Log.d(TAG, "支持的语言: ${SUPPORTED_LANGUAGES.keys.joinToString()}")
            
            onComplete(true, null)
            
        } catch (e: Exception) {
            val errorMsg = "TTS 引擎初始化失败: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onComplete(false, errorMsg)
        }
    }
    
    private fun copyAssetDir(context: Context, assetPath: String, destDir: File) {
        try {
            if (destDir.exists()) {
                Log.d(TAG, "目标目录已存在，跳过复制: ${destDir.absolutePath}")
                return
            }
            
            destDir.mkdirs()
            
            val files = context.assets.list(assetPath) ?: return
            
            for (file in files) {
                val sourcePath = "$assetPath/$file"
                val destFile = File(destDir, file)
                
                try {
                    val subFiles = context.assets.list(sourcePath)
                    if (subFiles != null && subFiles.isNotEmpty()) {
                        copyAssetDir(context, sourcePath, destFile)
                    } else {
                        context.assets.open(sourcePath).use { inputStream ->
                            FileOutputStream(destFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        Log.d(TAG, "复制文件: $sourcePath -> ${destFile.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "复制文件失败: $sourcePath", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "复制目录失败: $assetPath", e)
            throw e
        }
    }
    
    fun speak(
        text: String,
        language: String = DEFAULT_LANGUAGE,
        voiceStyle: Int = JapaneseVoices.DEFAULT,
        speed: Float = 1.0f,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (!isInitialized || ttsEngine == null) {
            onComplete(false, "TTS 引擎未初始化")
            return
        }

        Thread {
            try {
                Log.d(TAG, "生成语音: text='$text', 声音: $voiceStyle, 语速: $speed")
                
                val lengthScale = 1.0f / speed
                
                val genConfig = GenerationConfig(
                    sid = voiceStyle,
                    speed = speed
                )
                
                Log.d(TAG, "调用 generate, genConfig.sid=$voiceStyle, speed=$speed")
                
                val audio = ttsEngine?.generate(text, voiceStyle, speed)
                
                Log.d(TAG, "generate 返回: audio=$audio")
                if (audio != null) {
                    Log.d(TAG, "samples.size=${audio.samples.size}, sampleRate=${audio.sampleRate}")
                }
                
                if (audio != null && audio.samples.isNotEmpty()) {
                    Log.d(TAG, "语音生成成功，长度: ${audio.samples.size} 采样点, 采样率: ${audio.sampleRate}Hz")
                    
                    playAudio(audio.samples, audio.sampleRate)
                    
                    onComplete(true, null)
                } else {
                    Log.e(TAG, "语音生成失败: samples 为空")
                    onComplete(false, "语音生成失败，返回为空")
                }
                
            } catch (e: Exception) {
                val errorMsg = "语音生成失败: ${e.message}"
                Log.e(TAG, errorMsg, e)
                onComplete(false, errorMsg)
            }
        }.start()
    }
    
    private fun playAudio(samples: FloatArray, sampleRate: Int) {
        try {
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            
            audioTrack.play()
            
            val shortBuffer = ShortArray(samples.size)
            for (i in samples.indices) {
                val sample = (samples[i] * Short.MAX_VALUE).toInt()
                shortBuffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            
            var offset = 0
            while (offset < shortBuffer.size) {
                val length = minOf(bufferSize / 2, shortBuffer.size - offset)
                audioTrack.write(shortBuffer, offset, length)
                offset += length
            }
            
            audioTrack.stop()
            audioTrack.release()
            
            Log.d(TAG, "音频播放完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "音频播放失败", e)
        }
    }
    
    fun speakJapanese(
        text: String,
        voiceStyle: Int = JapaneseVoices.DEFAULT,
        speed: Float = 1.0f,
        onComplete: (Boolean, String?) -> Unit
    ) {
        speak(text, language = "ja", voiceStyle = voiceStyle, speed = speed, onComplete = onComplete)
    }
    
    fun release() {
        try {
            ttsEngine?.release()
            ttsEngine = null
            isInitialized = false
            Log.d(TAG, "TTS 引擎已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放 TTS 引擎时出错", e)
        }
    }
    
    fun stop() {
        try {
            isPlaying = false
            Log.d(TAG, "停止播放标记已设置")
        } catch (e: Exception) {
            Log.e(TAG, "停止播放时出错", e)
        }
    }
    
    fun isInitialized(): Boolean = isInitialized
}
