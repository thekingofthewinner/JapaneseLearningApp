package com.example.japaneselearningapp.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.IOException

object AudioPlayer {
    private const val TAG = "AudioPlayer"

    private var mediaPlayer: MediaPlayer? = null
    private var currentAudioPath: String? = null
    private var isPlaying = false
    private var onCompletionListener: (() -> Unit)? = null

    fun playAudio(
        context: Context,
        lessonId: Int,
        onComplete: (Boolean, String?) -> Unit
    ) {
        try {
            stop()

            val audioPath = getAudioPath(lessonId)
            Log.d(TAG, "准备播放音频: $audioPath")

            val assetFileDescriptor = context.assets.openFd(audioPath)

            mediaPlayer = MediaPlayer()
            mediaPlayer?.setDataSource(
                assetFileDescriptor.fileDescriptor,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.length
            )
            assetFileDescriptor.close()

            mediaPlayer?.prepareAsync()
            mediaPlayer?.setOnPreparedListener {
                Log.d(TAG, "音频准备完成，开始播放")
                mediaPlayer?.start()
                isPlaying = true
            }

            mediaPlayer?.setOnCompletionListener {
                Log.d(TAG, "音频播放完成")
                isPlaying = false
                release()
                onCompletionListener?.invoke()
                onComplete(true, null)
            }

            mediaPlayer?.setOnErrorListener { _, what, extra ->
                val errorMsg = "播放错误: what=$what, extra=$extra"
                Log.e(TAG, errorMsg)
                isPlaying = false
                release()
                onComplete(false, errorMsg)
                true
            }

            currentAudioPath = audioPath

        } catch (e: IOException) {
            val errorMsg = "音频文件不存在或读取失败: ${e.message}"
            Log.e(TAG, errorMsg, e)
            isPlaying = false
            release()
            onComplete(false, errorMsg)
        } catch (e: Exception) {
            val errorMsg = "播放失败: ${e.message}"
            Log.e(TAG, errorMsg, e)
            isPlaying = false
            release()
            onComplete(false, errorMsg)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            mediaPlayer = null
            isPlaying = false
            Log.d(TAG, "音频已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止音频时出错", e)
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    isPlaying = false
                    Log.d(TAG, "音频已暂停")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "暂停音频时出错", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                    isPlaying = true
                    Log.d(TAG, "音频已恢复播放")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "恢复播放时出错", e)
        }
    }

    fun isPlaying(): Boolean = isPlaying

    private fun release() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "释放播放器时出错", e)
        }
    }

    private fun getAudioPath(lessonId: Int): String {
        val levelIndex = (lessonId - 1) / 25
        val lessonNum = (lessonId - 1) % 25 + 1

        val levelName = when (levelIndex) {
            0 -> "N5"
            1 -> "N4"
            2 -> "N3"
            3 -> "N2"
            4 -> "N1"
            else -> "N5"
        }

        return "audio/$levelName/$lessonNum.mp3"
    }

    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionListener = listener
    }
}