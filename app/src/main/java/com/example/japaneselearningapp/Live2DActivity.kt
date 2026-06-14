package com.example.japaneselearningapp

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.japaneselearningapp.asr.AsrConfig
import com.example.japaneselearningapp.asr.AudioRecorder
import com.example.japaneselearningapp.network.MockApiService
import com.example.japaneselearningapp.tts.TtsConfig
import com.example.japaneselearningapp.tts.TtsConfig.VoiceStyles
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.atomic.AtomicBoolean

class Live2DActivity : AppCompatActivity() {
    private val TAG = "Live2DActivity"
    
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var statusText: TextView
    private lateinit var recordingIndicator: View
    private lateinit var stopButton: FloatingActionButton
    private lateinit var loadingIndicator: ProgressBar
    
    private val messages = mutableListOf<ChatMessage>()
    private var isSpeaking = AtomicBoolean(false)
    private var isProcessing = AtomicBoolean(false)
    
    private lateinit var audioRecorder: AudioRecorder
    
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
        chatRecyclerView = findViewById(R.id.chat_recycler_view)
        glSurfaceView = findViewById(R.id.live2d_view)
        statusText = findViewById(R.id.status_text)
        recordingIndicator = findViewById(R.id.recording_indicator)
        stopButton = findViewById(R.id.stop_button)
        loadingIndicator = findViewById(R.id.loading_indicator)
        
        messageAdapter = MessageAdapter(messages)
        chatRecyclerView.adapter = messageAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(Live2DRenderer(this))
        
        JniBridgeJava.setContext(this)
        JniBridgeJava.setActivityInstance(this)
        
        audioRecorder = AudioRecorder()
        
        stopButton.setOnClickListener { stopSpeaking() }
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
        // 初始化 TTS
        Log.d(TAG, "初始化 TTS 引擎...")
        TtsConfig.initialize(this) { success, error ->
            runOnUiThread {
                if (success) {
                    Log.d(TAG, "TTS 引擎初始化成功")
                    initializeAsr()
                } else {
                    Log.e(TAG, "TTS 引擎初始化失败: $error")
                    Toast.makeText(this, "TTS 初始化失败: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun initializeAsr() {
        // 初始化 ASR
        Log.d(TAG, "初始化 ASR 引擎...")
        AsrConfig.initialize(this,) { success, error ->
            runOnUiThread {
                if (success) {
                    Log.d(TAG, "ASR 引擎初始化成功")
                    addWelcomeMessage()
                    startListening()
                } else {
                    Log.e(TAG, "ASR 引擎初始化失败: $error")
                    Toast.makeText(this, "ASR 初始化失败: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun addWelcomeMessage() {
        messages.add(ChatMessage(
            text = "こんにちは！私はAIアシスタントの春です。何か話しかけてください。",
            isUser = false,
            language = "ja"
        ))
        messageAdapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()
        
        speakText("こんにちは！私はAIアシスタントの春です。何か話しかけてください。", "ja")
    }
    
    private fun startListening() {
        if (isProcessing.get() || isSpeaking.get()) {
            Log.d(TAG, "正在处理或播放中，暂不开始监听")
            return
        }
        
        setStatus("正在监听...")
        recordingIndicator.visibility = View.VISIBLE
        
        audioRecorder.startRecording(object : AudioRecorder.AudioCallback {
            override fun onAudioData(data: FloatArray) {
                if (!isProcessing.get()) {
                    // 使用 VAD + ASR 进行语音识别
                    AsrConfig.startRecognitionWithVad(data, object : AsrConfig.RecognitionCallback {
                        override fun onPartialResult(text: String) {
                            runOnUiThread {
                                setStatus("识别中: $text")
                            }
                        }
                        
                        override fun onFinalResult(text: String, language: String) {
                            runOnUiThread {
                                recordingIndicator.visibility = View.GONE
                                
                                if (text.isNotEmpty()) {
                                    handleUserInput(text, language)
                                } else {
                                    startListening()
                                }
                            }
                        }
                        
                        override fun onError(error: String) {
                            runOnUiThread {
                                Log.e(TAG, "ASR 错误: $error")
                                recordingIndicator.visibility = View.GONE
                                startListening()
                            }
                        }
                        
                        override fun onSpeechStart() {
                            runOnUiThread {
                                setStatus("检测到语音...")
                            }
                        }
                        
                        override fun onSpeechEnd() {
                            runOnUiThread {
                                setStatus("处理中...")
                            }
                        }
                    })
                }
            }
            
            override fun onError(error: String) {
                runOnUiThread {
                    Log.e(TAG, "录音错误: $error")
                    Toast.makeText(this@Live2DActivity, "录音错误: $error", Toast.LENGTH_SHORT).show()
                    recordingIndicator.visibility = View.GONE
                    startListening()
                }
            }
        })
    }
    
    private fun handleUserInput(text: String, language: String) {
        if (text.trim().isEmpty()) {
            startListening()
            return
        }
        
        isProcessing.set(true)
        
        // 添加用户消息
        messages.add(ChatMessage(text = text, isUser = true, language = language))
        messageAdapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()
        
        setStatus("思考中...")
        loadingIndicator.visibility = View.VISIBLE
        
        // 发送到 API（使用 Mock 数据）
        MockApiService.getMockResponse(text) { responseText, responseLanguage ->
            runOnUiThread {
                loadingIndicator.visibility = View.GONE
                
                // 添加机器人回复
                messages.add(ChatMessage(
                    text = responseText,
                    isUser = false,
                    language = responseLanguage
                ))
                messageAdapter.notifyItemInserted(messages.size - 1)
                scrollToBottom()
                
                // 播放语音
                speakText(responseText, responseLanguage)
            }
        }
    }
    
    private fun speakText(text: String, language: String) {
        if (!TtsConfig.isInitialized()) {
            Toast.makeText(this, "TTS 引擎尚未初始化", Toast.LENGTH_SHORT).show()
            isProcessing.set(false)
            startListening()
            return
        }
        
        stopSpeaking()
        
        isSpeaking.set(true)
        stopButton.show()
        setStatus("播放中...")
        
        TtsConfig.speak(
            text = text,
            language = language,
            voiceStyle = VoiceStyles.STYLE_5,
            speed = 1.0f
        ) { success, error ->
            isSpeaking.set(false)
            runOnUiThread {
                stopButton.hide()
                if (!success) {
                    Log.e(TAG, "TTS 播放失败: $error")
                    Toast.makeText(this, "播放失败: $error", Toast.LENGTH_SHORT).show()
                }
                
                isProcessing.set(false)
                startListening()
            }
        }
    }
    
    private fun stopSpeaking() {
        if (isSpeaking.get()) {
            TtsConfig.stop()
            isSpeaking.set(false)
            stopButton.hide()
        }
    }
    
    private fun setStatus(status: String) {
        statusText.text = status
    }
    
    private fun scrollToBottom() {
        chatRecyclerView.postDelayed({
            chatRecyclerView.scrollToPosition(messageAdapter.itemCount - 1)
        }, 100)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.stopRecording()
        stopSpeaking()
        TtsConfig.release()
        AsrConfig.release()
        MockApiService.shutdown()
    }
    
    data class ChatMessage(
        var text: String,
        val isUser: Boolean,
        val language: String
    )
    
    class MessageAdapter(private val messages: List<ChatMessage>) :
        RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
        
        override fun getItemViewType(position: Int): Int {
            return if (messages[position].isUser) 1 else 0
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): MessageViewHolder {
            val layoutId = if (viewType == 1) R.layout.item_message_user else R.layout.item_message_bot
            val view = android.view.LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
            return MessageViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val message = messages[position]
            holder.messageText.text = message.text
        }
        
        override fun getItemCount(): Int = messages.size
        
        class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val messageText: TextView = itemView.findViewById(R.id.message_text)
        }
    }
}