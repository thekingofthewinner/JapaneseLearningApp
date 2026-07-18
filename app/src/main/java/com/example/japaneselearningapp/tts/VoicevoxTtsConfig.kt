package com.example.japaneselearningapp.tts

import android.content.Context
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object VoicevoxTtsConfig {
    private const val TAG = "VoicevoxTtsConfig"

    const val DEFAULT_LANGUAGE = "ja"
    const val DEFAULT_STYLE_ID = 16

    object JapaneseVoices {
        const val KYUSHU_SORA_NORMAL = 16
        const val KYUSHU_SORA_AWAWA = 15
        const val KYUSHU_SORA_TSUNTSUN = 18
        const val KYUSHU_SORA_SEXY = 17
        const val KYUSHU_SORA_WHISPER = 19
        const val DEFAULT = KYUSHU_SORA_NORMAL
    }

    private var bridge: VoicevoxBridge? = null
    private var isInitialized = false
    private var audioTrack: AudioTrack? = null

    fun initialize(context: Context, onComplete: (Boolean, String?) -> Unit) {
        Log.d(TAG, "开始初始化 VoiceVox TTS 引擎...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                bridge = VoicevoxBridge()

                val dictPath = "${context.filesDir.absolutePath}/open_jtalk_dic_utf_8-1.11"
                val onnxruntimePath = "${context.applicationInfo.nativeLibraryDir}/libvoicevox_onnxruntime.so"

                Log.d(TAG, "字典路径: $dictPath")
                Log.d(TAG, "ONNX Runtime路径: $onnxruntimePath")

                copyAssetsIfNeeded(context)

                val initResult = bridge?.initialize(dictPath, onnxruntimePath)
                Log.d(TAG, "VoiceVox 初始化结果: $initResult")

                if (initResult == true) {
                    val modelPath = "${context.filesDir.absolutePath}/voicevox/2.vvm"
                    val speakerId = bridge?.loadVoiceModel(modelPath)
                    Log.d(TAG, "语音模型加载结果: speakerId=$speakerId")

                    if (speakerId != null && speakerId >= 0) {
                        isInitialized = true
                        Log.d(TAG, "VoiceVox TTS 引擎初始化成功！")
                        withContext(Dispatchers.Main) {
                            onComplete(true, null)
                        }
                    } else {
                        Log.e(TAG, "VoiceVox 语音模型加载失败")
                        bridge?.release()
                        bridge = null
                        withContext(Dispatchers.Main) {
                            onComplete(false, "VoiceVox 语音模型加载失败")
                        }
                    }
                } else {
                    Log.e(TAG, "VoiceVox 引擎初始化失败")
                    bridge = null
                    withContext(Dispatchers.Main) {
                        onComplete(false, "VoiceVox 引擎初始化失败")
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "VoiceVox TTS 引擎初始化失败: ${e.message}"
                Log.e(TAG, errorMsg, e)
                bridge?.release()
                bridge = null
                withContext(Dispatchers.Main) {
                    onComplete(false, errorMsg)
                }
            }
        }
    }

    fun speak(
        text: String,
        language: String = DEFAULT_LANGUAGE,
        styleId: Int = DEFAULT_STYLE_ID,
        speed: Float = 1.0f,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        if (!isInitialized || bridge == null) {
            onComplete?.invoke(false, "TTS 引擎未初始化")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "生成语音: text='$text', styleId=$styleId, speed=$speed")

                Log.d(TAG, "开始推理...")
                val inferStartTime = System.currentTimeMillis()
                val result = bridge?.synthesis(text, styleId)
                val inferDuration = System.currentTimeMillis() - inferStartTime
                Log.d(TAG, "推理完成，耗时: ${inferDuration}ms, 结果长度: ${result?.size ?: 0}")

                if (result != null && result.isNotEmpty()) {
                    Log.d(TAG, "语音生成成功，长度: ${result.size} 字节")
                    playAudio(result)
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(true, null)
                    }
                } else {
                    Log.e(TAG, "语音生成失败，结果为空")
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(false, "语音生成失败")
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "语音合成失败: ${e.message}"
                Log.e(TAG, errorMsg, e)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, errorMsg)
                }
            }
        }
    }

    private fun playAudio(wavData: ByteArray) {
        try {
            val sampleRate = 24000
            val channelConfig = AudioTrack.CHANNEL_OUT_MONO
            val audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            audioTrack = AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM
            )

            audioTrack?.play()
            audioTrack?.write(wavData, 0, wavData.size)
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null

            Log.d(TAG, "音频播放完成")
        } catch (e: Exception) {
            Log.e(TAG, "音频播放失败: ${e.message}", e)
            audioTrack?.release()
            audioTrack = null
        }
    }

    fun release() {
        Log.d(TAG, "释放 VoiceVox TTS 引擎...")
        audioTrack?.release()
        audioTrack = null
        bridge?.release()
        bridge = null
        isInitialized = false
    }

    private fun copyAssetsIfNeeded(context: Context) {
        try {
            val openJtalkDest = "${context.filesDir}/open_jtalk_dic_utf_8-1.11"
            val voicevoxDest = "${context.filesDir}/voicevox"

            if (!java.io.File(openJtalkDest).exists()) {
                Log.d(TAG, "复制 OpenJTalk 字典到内部存储...")
                copyDirectoryFromAssets(context, "open_jtalk_dic_utf_8-1.11", openJtalkDest)
            }

            if (!java.io.File(voicevoxDest).exists()) {
                Log.d(TAG, "复制 VoiceVox 模型到内部存储...")
                copyDirectoryFromAssets(context, "voicevox", voicevoxDest)
            }
        } catch (e: Exception) {
            Log.e(TAG, "复制 assets 失败: ${e.message}", e)
        }
    }

    private fun copyDirectoryFromAssets(context: Context, assetPath: String, destPath: String) {
        val assetManager = context.assets
        val files = assetManager.list(assetPath) ?: return

        val destDir = java.io.File(destPath)
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        for (file in files) {
            val sourcePath = "$assetPath/$file"
            val destFilePath = "$destPath/$file"

            try {
                assetManager.open(sourcePath).use { input ->
                    java.io.FileOutputStream(destFilePath).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "复制文件失败: $sourcePath -> $destFilePath, ${e.message}")
            }
        }
    }
}