package com.example.japaneselearningapp

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.japaneselearningapp.asr.AudioRecorder
import com.example.japaneselearningapp.network.MockApiService
import com.example.japaneselearningapp.tts.TtsConfig
import com.example.japaneselearningapp.tts.VoicevoxTtsConfig
import com.example.japaneselearningapp.tts.VoicevoxTtsConfig.JapaneseVoices.KYUSHU_SORA_NORMAL
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.atomic.AtomicBoolean

class Live2DActivity : AppCompatActivity() {
    private val TAG = "Live2DActivity"

    // UI 组件
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var micButton: FloatingActionButton
    private lateinit var closeButton: FloatingActionButton
    private lateinit var voiceStatusContainer: LinearLayout
    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View
    private lateinit var statusText: TextView
    private lateinit var muteHint: TextView

    // 状态管理
    private var isMuted = false
    private var isSpeaking = AtomicBoolean(false)
    private var isProcessing = AtomicBoolean(false)
    private var isListening = AtomicBoolean(false)

    // 音频录制
    private lateinit var audioRecorder: AudioRecorder

    // 动画
    private var dotsAnimation: AnimatorSet? = null

    companion object {
        private const val REQUEST_AUDIO_PERMISSION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live2d)

        initViews()
        checkPermissions()
    }

    private fun initViews() {
        glSurfaceView = findViewById(R.id.live2d_view)
        micButton = findViewById(R.id.mic_button)
        closeButton = findViewById(R.id.close_button)
        voiceStatusContainer = findViewById(R.id.voice_status_container)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)
        statusText = findViewById(R.id.status_text)
        muteHint = findViewById(R.id.mute_hint)

        // 初始化 Live2D
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(Live2DRenderer(this))

        JniBridgeJava.setContext(this)
        JniBridgeJava.setActivityInstance(this)

        // 设置触摸监听器，将触摸事件传递给 Native 层
        glSurfaceView.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    JniBridgeJava.nativeOnTouchesBegan(event.x, event.y)
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    JniBridgeJava.nativeOnTouchesMoved(event.x, event.y)
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    JniBridgeJava.nativeOnTouchesEnded(event.x, event.y)
                    true
                }
                else -> false
            }
        }

        // 初始化音频录制器
        audioRecorder = AudioRecorder()

        // 麦克风按钮点击事件
        micButton.setOnClickListener { toggleMute() }

        // 关闭按钮点击事件
        closeButton.setOnClickListener { finish() }

        // 语音状态容器点击事件（打断 AI 说话）
        voiceStatusContainer.setOnClickListener { interruptAI() }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_AUDIO_PERMISSION
            )
        } else {
            initializeEngines()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeEngines()
            } else {
                Toast.makeText(this, "需要麦克风权限才能使用语音功能", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun initializeEngines() {
        Log.d(TAG, "检查引擎状态...")

        // TTS 已在 MainActivity 中初始化，直接检查状态
        if (VoicevoxTtsConfig.isInitialized()) {
            Log.d(TAG, "TTS 引擎已就绪")
        } else {
            Log.e(TAG, "TTS 引擎未初始化")
            Toast.makeText(this, "TTS 引擎未初始化，将使用文字模式", Toast.LENGTH_SHORT).show()
        }

        // 暂时跳过 ASR 初始化（模型文件损坏）
        // TODO: 下载正确的 ASR 模型后恢复
        /*
        initializeAsr()
        */
        // 直接显示欢迎界面
        micButton.isEnabled = false
        micButton.alpha = 0.5f
        startConversation()
    }

//    private fun initializeAsr() {
//        AsrConfig.initialize(this) { asrSuccess, asrError ->
//            runOnUiThread {
//                if (asrSuccess) {
//                    Log.d(TAG, "ASR 引擎初始化成功")
//                } else {
//                    Log.e(TAG, "ASR 引擎初始化失败: $asrError")
//                    Toast.makeText(this, "ASR 初始化失败，语音功能不可用", Toast.LENGTH_SHORT).show()
//                    // ASR失败时禁用麦克风按钮
//                    micButton.isEnabled = false
//                    micButton.alpha = 0.5f
//                }
//
//                // 无论ASR是否成功，都显示欢迎界面
//                startConversation()
//            }
//        }
//    }

    /**
     * 开始对话
     */
    private fun startConversation() {
        // 播放欢迎语（如果TTS可用）
        if (VoicevoxTtsConfig.isInitialized()) {
            speakText("こんにちは！私は AI アシスタントの春です。いつも画面の前の皆さんとお話できるのを楽しみに待っています。日常のちょっとした雑談はもちろん、疑問の解決や知りたい情報の調べ、気分転換のお喋りまで、どんな小さなことでも気軽に話しかけてください。あなたの言葉をしっかり受け止めて、優しくお返事いたします。", "ja") {
            }
        } else {
            // TTS不可用时，直接显示文字提示
            statusText.text = "欢迎！请点击麦克风开始对话"
        }
    }

    /**
     * 开始监听
     */
    @SuppressLint("MissingPermission")
    private fun startListening() {
        if (isProcessing.get() || isSpeaking.get() || isMuted) {
            Log.d(TAG, "正在处理或播放中，暂不开始监听")
            return
        }

        isListening.set(true)
        updateVoiceStatus(VoiceStatus.LISTENING)

        audioRecorder.startRecording(object : AudioRecorder.AudioCallback {
            override fun onAudioData(data: FloatArray) {
                if (!isProcessing.get() && !isMuted) {

                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    Log.e(TAG, "录音错误: $error")
                    Toast.makeText(this@Live2DActivity, "录音错误: $error", Toast.LENGTH_SHORT).show()
                    isListening.set(false)
                    startListening()
                }
            }
        })
    }

    /**
     * 处理用户输入
     */
    private fun handleUserInput(text: String, language: String) {
        if (text.trim().isEmpty()) {
            startListening()
            return
        }

        isProcessing.set(true)

        // 发送到 API（使用 Mock 数据）
        MockApiService.getMockResponse(text) { responseText, responseLanguage ->
            runOnUiThread {
                // 播放语音
                speakText(responseText, responseLanguage) {
                    isProcessing.set(false)
                    startListening()
                }
            }
        }
    }

    /**
     * 播放语音
     */
    private fun speakText(text: String, language: String, onComplete: () -> Unit = {}) {
        if (!VoicevoxTtsConfig.isInitialized()) {
            Toast.makeText(this, "TTS 引擎尚未初始化", Toast.LENGTH_SHORT).show()
            isProcessing.set(false)
            startListening()
            return
        }

        stopSpeaking()

        isSpeaking.set(true)
        updateVoiceStatus(VoiceStatus.SPEAKING_AI)

        VoicevoxTtsConfig.speak(
            text = text,
            language = language,
            styleId = VoicevoxTtsConfig.DEFAULT_STYLE_ID,
            speed = 1f  // 降低语速，0.5-0.7 比较自然
        ) { success, error ->
            isSpeaking.set(false)
            runOnUiThread {
                if (!success) {
                    Log.e(TAG, "TTS 播放失败: $error")
                    Toast.makeText(this, "播放失败: $error", Toast.LENGTH_SHORT).show()
                }
                onComplete()
            }
        }
    }

    /**
     * 停止播放
     */
    private fun stopSpeaking() {
        if (isSpeaking.get()) {
            TtsConfig.stop()
            isSpeaking.set(false)
        }
    }

    /**
     * 打断 AI 说话
     */
    private fun interruptAI() {
        if (isSpeaking.get()) {
            stopSpeaking()
            isProcessing.set(false)
            startListening()
        }
    }

    /**
     * 切换静音状态
     */
    private fun toggleMute() {
        isMuted = !isMuted

        if (isMuted) {
            // 静音状态
            micButton.setImageResource(R.drawable.ic_mic_off)
            micButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.red)
            muteHint.visibility = View.VISIBLE
            stopListening()
            updateVoiceStatus(VoiceStatus.MUTED)
        } else {
            // 取消静音
            micButton.setImageResource(R.drawable.ic_mic)
            micButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.green)
            muteHint.visibility = View.GONE
            startListening()
        }
    }

    /**
     * 停止监听
     */
    private fun stopListening() {
        isListening.set(false)
        audioRecorder.stopRecording()
    }

    /**
     * 更新语音状态显示
     */
    private fun updateVoiceStatus(status: VoiceStatus) {
        stopDotsAnimation()

        when (status) {
            VoiceStatus.LISTENING -> {
                // 三个白色圆点 + "正在听"
                setDotsAppearance(isWhite = true, isSquare = false)
                statusText.text = "正在听"
            }
            VoiceStatus.SPEAKING_USER -> {
                // 三个拉长的白色圆点 + "正在听"
                setDotsAppearance(isWhite = true, isSquare = false)
                statusText.text = "正在听"
                startDotsAnimation()
            }
            VoiceStatus.SPEAKING_AI -> {
                // 三个灰色方形 + "点击或者说话打断"
                setDotsAppearance(isWhite = false, isSquare = true)
                statusText.text = "点击或者说话打断"
            }
            VoiceStatus.PROCESSING -> {
                // 三个白色圆点 + "处理中"
                setDotsAppearance(isWhite = true, isSquare = false)
                statusText.text = "处理中"
            }
            VoiceStatus.MUTED -> {
                // 三个灰色圆点 + "已静音"
                setDotsAppearance(isWhite = false, isSquare = false)
                statusText.text = "已静音"
            }
        }
    }

    /**
     * 设置圆点外观
     */
    private fun setDotsAppearance(isWhite: Boolean, isSquare: Boolean) {
        val drawableRes = when {
            isSquare -> R.drawable.gray_square
            isWhite -> R.drawable.white_dot
            else -> R.drawable.gray_dot
        }

        dot1.setBackgroundResource(drawableRes)
        dot2.setBackgroundResource(drawableRes)
        dot3.setBackgroundResource(drawableRes)

        // 重置缩放
        dot1.scaleY = 1f
        dot2.scaleY = 1f
        dot3.scaleY = 1f
    }

    /**
     * 开始圆点动画
     */
    private fun startDotsAnimation() {
        stopDotsAnimation()

        val anim1 = ObjectAnimator.ofFloat(dot1, "scaleY", 1f, 2f).apply {
            duration = 300
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }

        val anim2 = ObjectAnimator.ofFloat(dot2, "scaleY", 1f, 2f).apply {
            duration = 300
            startDelay = 100
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }

        val anim3 = ObjectAnimator.ofFloat(dot3, "scaleY", 1f, 2f).apply {
            duration = 300
            startDelay = 200
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }

        dotsAnimation = AnimatorSet().apply {
            playTogether(anim1, anim2, anim3)
            start()
        }
    }

    /**
     * 停止圆点动画
     */
    private fun stopDotsAnimation() {
        dotsAnimation?.cancel()
        dotsAnimation = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: 释放资源")
        
        // 停止所有操作
        stopListening()
        stopSpeaking()
        
        // 停止动画
        stopDotsAnimation()
        
        // 释放 GLSurfaceView（关键修复）
        glSurfaceView.onPause()
        try {
            glSurfaceView.setRenderer(null)
        } catch (e: Exception) {
            Log.w(TAG, "设置 renderer 为 null 失败", e)
        }
        
        // 释放 JNI 资源（关键修复）
        try {
            JniBridgeJava.nativeOnDestroy()
        } catch (e: Exception) {
            Log.w(TAG, "释放 JNI 资源失败", e)
        }
        
        // 释放 ASR 和 TTS 引擎
        TtsConfig.release()
        MockApiService.shutdown()
    }

    /**
     * 语音状态枚举
     */
    enum class VoiceStatus {
        LISTENING,        // 正在听
        SPEAKING_USER,    // 用户正在说话
        SPEAKING_AI,      // AI 正在说话
        PROCESSING,       // 处理中
        MUTED             // 已静音
    }
}