package com.example.japaneselearningapp.asr

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorder(
    private val sampleRate: Int = 16000,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    private val TAG = "AudioRecorder"
    
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    )
    
    interface AudioCallback {
        fun onAudioData(data: FloatArray)
        fun onError(error: String)
    }
    
    /**
     * 开始录音
     */
    fun startRecording(callback: AudioCallback): Boolean {
        if (isRecording.get()) {
            Log.w(TAG, "已经在录音中")
            return false
        }
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                callback.onError("AudioRecord 初始化失败")
                return false
            }
            
            isRecording.set(true)
            
            recordingThread = Thread {
                try {
                    val buffer = ShortArray(bufferSize / 2)
                    audioRecord?.startRecording()
                    
                    Log.d(TAG, "开始录音...")
                    
                    while (isRecording.get()) {
                        val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        
                        if (readSize > 0) {
                            // 转换为 FloatArray
                            val floatData = FloatArray(readSize) { i ->
                                buffer[i] / 32768.0f
                            }
                            callback.onAudioData(floatData)
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "录音错误", e)
                    callback.onError("录音错误: ${e.message}")
                }
            }.apply {
                name = "AudioRecorder-Thread"
                start()
            }
            
            return true
            
        } catch (e: SecurityException) {
            Log.e(TAG, "缺少麦克风权限", e)
            callback.onError("缺少麦克风权限")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "启动录音失败", e)
            callback.onError("启动录音失败: ${e.message}")
            return false
        }
    }
    
    /**
     * 停止录音
     */
    fun stopRecording() {
        if (!isRecording.get()) {
            return
        }
        
        isRecording.set(false)
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            recordingThread?.join(1000)
            recordingThread = null
            
            Log.d(TAG, "录音已停止")
            
        } catch (e: Exception) {
            Log.e(TAG, "停止录音失败", e)
        }
    }
    
    /**
     * 是否正在录音
     */
    fun isRecording(): Boolean = isRecording.get()
}