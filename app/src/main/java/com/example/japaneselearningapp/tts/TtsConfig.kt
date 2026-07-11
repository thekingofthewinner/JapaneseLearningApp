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

/**
 * TTS 配置管理器
 * 基于 Kokoro 多语言模型，支持日语等多种语言
 */
object TtsConfig {
    private const val TAG = "TtsConfig"
    
    // TTS 模型相关配置
    private const val TTS_ASSET_DIR = "ttsKokoro"
    
    // 默认语言 - 日语
    const val DEFAULT_LANGUAGE = "ja"
    
    // 支持的语言列表
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
    
    // 日语声音预设
    object JapaneseVoices {
        const val FEMALE_ALPHA = 6   // jf_alpha 女声
        const val MALE_OMEGA = 7    // jm_omega 男声
        const val DEFAULT = FEMALE_ALPHA
    }
    
    private var ttsEngine: OfflineTts? = null
    private var isInitialized = false
    private var isPlaying = false
    private var modelDir: String = ""
    
    /**
     * 初始化 TTS 引擎
     */
    fun initialize(context: Context, onComplete: (Boolean, String?) -> Unit) {
        try {
            Log.d(TAG, "开始初始化 Kokoro TTS 引擎...")
            
            val modelDir = TTS_ASSET_DIR
            
            // 检查必需的模型文件是否存在
            val requiredFiles = listOf(
                "$modelDir/model.int8.onnx",
                "$modelDir/voices.bin",
                "$modelDir/tokens.txt"
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
            
            // 检查可选文件
            val hasDataDir = try {
                context.assets.list("$modelDir/espeak-ng-data")?.isNotEmpty() == true
            } catch (e: Exception) { false }
            
            val hasDictDir = try {
                context.assets.list("$modelDir/dict")?.isNotEmpty() == true
            } catch (e: Exception) { false }
            
            Log.d(TAG, "espeak-ng-data: $hasDataDir, dict: $hasDictDir")
            
            val modelConfig = OfflineTtsKokoroModelConfig(
                model = "$modelDir/model.int8.onnx",
                voices = "$modelDir/voices.bin",
                tokens = "$modelDir/tokens.txt",
                lengthScale = 1.0f,
                dataDir = if (hasDataDir) "$modelDir/espeak-ng-data" else "",
                dictDir = if (hasDictDir) "$modelDir/dict" else ""
            )
            
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    kokoro = modelConfig,
                    numThreads = 2,
                    debug = true
                )
            )
            
            Log.d(TAG, "开始创建 OfflineTts (Kokoro)...")
            
            // 创建 TTS 引擎（使用 assetManager 从 assets 加载）
            ttsEngine = OfflineTts(context.assets, config)
            
            Log.d(TAG, "OfflineTts 创建成功")
            
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
    
    /**
     * 生成语音并直接播放
     */
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
                
                // Kokoro 使用 lengthScale 控制语速
                // lengthScale > 1.0 -> 更慢
                // lengthScale < 1.0 -> 更快
                // 我们把 speed 参数转换一下：
                // speed=1.0 正常 -> lengthScale=1.0
                // speed=0.5 慢 -> lengthScale=2.0
                // speed=2.0 快 -> lengthScale=0.5
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
                    
                    // 播放音频
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
    
    /**
     * 播放音频数据
     */
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
            
            // 将 Float 转换为 Short (16-bit PCM)
            val shortBuffer = ShortArray(samples.size)
            for (i in samples.indices) {
                val sample = (samples[i] * Short.MAX_VALUE).toInt()
                shortBuffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            
            // 分块写入
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
    
    /**
     * 生成日语语音（快捷方法）
     */
    fun speakJapanese(
        text: String,
        voiceStyle: Int = JapaneseVoices.DEFAULT,
        speed: Float = 1.0f,
        onComplete: (Boolean, String?) -> Unit
    ) {
        speak(text, language = "ja", voiceStyle = voiceStyle, speed = speed, onComplete = onComplete)
    }
    
    /**
     * 释放 TTS 资源
     */
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
    
    /**
     * 停止播放
     */
    fun stop() {
        try {
            isPlaying = false
            Log.d(TAG, "停止播放标记已设置")
        } catch (e: Exception) {
            Log.e(TAG, "停止播放时出错", e)
        }
    }
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
}