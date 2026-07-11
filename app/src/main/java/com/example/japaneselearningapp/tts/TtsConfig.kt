package com.example.japaneselearningapp.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.bertvits2_infer_wrapper.impl.BertVITS2SimpleInferImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object TtsConfig {
    private const val TAG = "TtsConfig"
    
    const val DEFAULT_LANGUAGE = "ja"
    
    val SUPPORTED_LANGUAGES = mapOf(
        "ja" to "日语",
        "en" to "英语",
        "zh" to "中文"
    )
    
    object JapaneseVoices {
        const val FEMALE_ALPHA = "八重神子_JP"
        const val MALE_OMEGA = "宵宫_JP"
        const val FEMALE_CHUN = "椿_JP"
        const val MALE_YAJIU = "野兽先辈_JP"
        const val DEFAULT = FEMALE_ALPHA
    }
    
    object ChineseVoices {
        const val CHARACTER_A = "八重神子_ZH"
        const val CHARACTER_B = "阿米娅_ZH"
        const val DEFAULT = CHARACTER_A
    }
    
    object EnglishVoices {
        const val CHARACTER_A = "1999_EN"
        const val DEFAULT = CHARACTER_A
    }
    
    private var ttsEngine: BertVITS2SimpleInferImpl? = null
    private var isInitialized = false
    
    fun initialize(context: Context, onComplete: (Boolean, String?) -> Unit) {
        Log.d(TAG, "开始初始化 Bert-VITS2-MNN TTS 引擎...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ttsEngine = BertVITS2SimpleInferImpl(context)
                
                val initResult = ttsEngine?.init()
                Log.d(TAG, "Bert-VITS2-MNN 初始化结果: $initResult")
                
                if (initResult == true) {
                    isInitialized = true
                    val spkList = ttsEngine?.getSpkNameList()
                    Log.d(TAG, "Bert-VITS2-MNN 引擎初始化成功！")
                    Log.d(TAG, "支持的语音: ${spkList?.joinToString()}")
                    
                    withContext(Dispatchers.Main) {
                        onComplete(true, null)
                    }
                } else {
                    Log.e(TAG, "Bert-VITS2-MNN 引擎初始化失败")
                    ttsEngine = null
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Bert-VITS2-MNN 引擎初始化失败")
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "TTS 引擎初始化失败: ${e.message}"
                Log.e(TAG, errorMsg, e)
                ttsEngine = null
                withContext(Dispatchers.Main) {
                    onComplete(false, errorMsg)
                }
            }
        }
    }
    
    fun speak(
        text: String,
        language: String = DEFAULT_LANGUAGE,
        voiceStyle: String = JapaneseVoices.DEFAULT,
        speed: Float = 1.0f,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (!isInitialized || ttsEngine == null) {
            onComplete(false, "TTS 引擎未初始化")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "生成语音: text='$text', 声音: $voiceStyle, 语速: $speed")
                
                ttsEngine?.setAudioLengthScale(1.0f / speed)
                
                val result = ttsEngine?.infer(text, voiceStyle)
                
                Log.d(TAG, "infer 返回: $result")
                if (result != null && result.first != null && result.first!!.isNotEmpty()) {
                    Log.d(TAG, "语音生成成功，长度: ${result.first!!.size} 采样点, 采样率: ${result.second}Hz")
                    
                    playAudio(result.first!!, result.second)
                    
                    withContext(Dispatchers.Main) {
                        onComplete(true, null)
                    }
                } else {
                    Log.e(TAG, "语音生成失败: samples 为空")
                    withContext(Dispatchers.Main) {
                        onComplete(false, "语音生成失败，返回为空")
                    }
                }
                
            } catch (e: Exception) {
                val errorMsg = "语音生成失败: ${e.message}"
                Log.e(TAG, errorMsg, e)
                withContext(Dispatchers.Main) {
                    onComplete(false, errorMsg)
                }
            }
        }
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
        voiceStyle: String = JapaneseVoices.DEFAULT,
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
            Log.d(TAG, "停止播放")
        } catch (e: Exception) {
            Log.e(TAG, "停止播放时出错", e)
        }
    }
    
    fun isInitialized(): Boolean = isInitialized
    
    fun getAvailableVoices(): List<String> {
        return ttsEngine?.getSpkNameList() ?: emptyList()
    }
}