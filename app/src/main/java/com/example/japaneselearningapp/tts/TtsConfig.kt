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
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import java.io.File
import java.io.FileOutputStream

/**
 * TTS 配置管理器
 * 基于 Supertonic 3 模型，支持 31 种语言
 */
object TtsConfig {
    private const val TAG = "TtsConfig"
    
    // TTS 模型相关配置
    private const val TTS_ASSET_DIR = "tts"
    private const val TTS_MODEL_VERSION = "2026-05-11"  // Supertonic 3 模型版本
    
    // 默认语言 - 日语
    const val DEFAULT_LANGUAGE = "ja"
    
    // 支持的语言列表
    val SUPPORTED_LANGUAGES = mapOf(
        "ja" to "日语",
        "en" to "英语",
        "zh" to "中文",
        "ko" to "韩语",
        "es" to "西班牙语",
        "fr" to "法语",
        "de" to "德语",
        "it" to "意大利语",
        "pt" to "葡萄牙语",
        "ru" to "俄语",
        "ar" to "阿拉伯语",
        "hi" to "印地语"
    )
    
    // 语音风格（Supertonic 3 支持 0-7）
    object VoiceStyles {
        const val STYLE_0 = 0
        const val STYLE_1 = 1
        const val STYLE_2 = 2
        const val STYLE_3 = 3
        const val STYLE_4 = 4
        const val STYLE_5 = 5
        const val STYLE_6 = 6
        const val STYLE_7 = 7
        
        // 默认使用 STYLE_6（通常是女声）
        const val DEFAULT = STYLE_6
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
                Log.d(TAG, "开始初始化 TTS 引擎...")
                
                // sherpa-onnx v1.13.x 需要从 assets 加载模型
                // 配置模型路径（相对于 assets 目录）
                val modelDir = "tts"
                
                // 检查 assets 中的模型文件是否存在
                try {
                    context.assets.open("$modelDir/tts.json").close()
                    Log.d(TAG, "TTS 模型文件存在")
                } catch (e: Exception) {
                    onComplete(false, "未找到 TTS 模型文件")
                    return
                }
                
                // 构建配置（使用 assets 中的相对路径）
                val modelConfig = OfflineTtsSupertonicModelConfig(
                    durationPredictor = "$modelDir/duration_predictor.int8.onnx",
                    textEncoder = "$modelDir/text_encoder.int8.onnx",
                    vectorEstimator = "$modelDir/vector_estimator.int8.onnx",
                    vocoder = "$modelDir/vocoder.int8.onnx",
                    ttsJson = "$modelDir/tts.json",
                    unicodeIndexer = "$modelDir/unicode_indexer.bin",
                    voiceStyle = "$modelDir/voice.bin"
                )
                
                val config = OfflineTtsConfig(
                    model = OfflineTtsModelConfig(
                        supertonic = modelConfig,
                        numThreads = 2,
                        debug = true
                    )
                )
                
                Log.d(TAG, "开始创建 OfflineTts...")
                
                // 创建 TTS 引擎（使用 assetManager 从 assets 加载）
                ttsEngine = OfflineTts(
                    context.assets,
                    config
                )
                
                Log.d(TAG, "OfflineTts 创建成功")
                
                isInitialized = true
                Log.d(TAG, "TTS 引擎初始化成功！")
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
        voiceStyle: Int = VoiceStyles.DEFAULT,
        speed: Float = 0.5f,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (!isInitialized || ttsEngine == null) {
            onComplete(false, "TTS 引擎未初始化")
            return
        }

        Thread {
            try {
                Log.d(TAG, "生成语音: text='$text', 风格: $voiceStyle, 语速: $speed")

                // Supertonic 模型使用简单的 generate 方法
                // 风格由 voice.bin 文件决定，GenerationConfig 不需要额外参数
                Log.d(TAG, "开始调用 ttsEngine?.generate...")

                val audio = ttsEngine?.generate(text)

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

                    // 尝试使用 GenerationConfig
                    Log.d(TAG, "尝试使用 GenerationConfig...")
                    val genConfig = GenerationConfig(
                        sid = voiceStyle,
                        speed = speed
                    )

                    val audio2 = ttsEngine?.generate(text, voiceStyle,speed)
                    Log.d(TAG, "generate with config 返回: audio=$audio2, samples.size=${audio2?.samples?.size ?: -1}")

                    if (audio2 != null && audio2.samples.isNotEmpty()) {
                        Log.d(TAG, "语音生成成功，长度: ${audio2.samples.size} 采样点, 采样率: ${audio2.sampleRate}Hz")
                        playAudio(audio2.samples, audio2.sampleRate)
                        onComplete(true, null)
                    } else {
                        onComplete(false, "语音生成失败，返回为空")
                    }
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
        onComplete: (Boolean, String?) -> Unit
    ) {
        speak(text, language = "ja", onComplete = onComplete)
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
            // sherpa-onnx 的 OfflineTts 不支持直接停止，
            // 但我们可以标记停止状态
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