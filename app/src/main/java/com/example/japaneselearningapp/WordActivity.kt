package com.example.japaneselearningapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japaneselearningapp.data.AppDatabase
import com.example.japaneselearningapp.data.entity.WordEntity
import com.example.japaneselearningapp.tts.TtsConfig
import com.example.japaneselearningapp.tts.TtsConfig.VoiceStyles.STYLE_6
import com.example.japaneselearningapp.ui.theme.JapaneseLearningAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WordActivity : ComponentActivity() {
    private val TAG = "WordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JapaneseLearningAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WordListPage(
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        TtsConfig.initialize(this) { success, error ->
            if (success) {
                Log.d(TAG, "TTS 引擎初始化成功")
            } else {
                Log.e(TAG, "TTS 引擎初始化失败: $error")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TtsConfig.release()
    }
}

@Composable
fun WordListPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val allWords by database.wordDao().getAllWords().collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2)
                    )
                )
            )
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 32.dp)
        ) {
            items(allWords) { word ->
                WordCard(
                    word = word,
                    onLongPress = {
                        if (TtsConfig.isInitialized()) {
                            val cleanedWord = cleanJapaneseWord(word.wordJp)
                            TtsConfig.speak(cleanedWord, "ja",STYLE_6, 0.7f) { success, error ->
                                if (!success) {
                                    Log.e("WordActivity", "TTS 播放失败: $error")
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun WordCard(
    word: WordEntity,
    onLongPress: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(20.dp))
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.3f),
                ambientColor = Color.Black.copy(alpha = 0.2f)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.85f else 0.7f),
                        Color.White.copy(alpha = if (isPressed) 0.75f else 0.55f),
                        Color.White.copy(alpha = if (isPressed) 0.7f else 0.5f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (!released) {
                            return@detectTapGestures
                        }
                    },
                    onLongPress = {
                        isPressed = false
                        onLongPress()
                    },
                    onTap = {
                        isPressed = false
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = word.wordJp,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = word.wordCn,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black.copy(alpha = 0.75f)
                )
            }
            
            word.wordAttr?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }
        }
    }
}
private fun cleanJapaneseWord(word: String): String {
    return word.trim()
        // 1. 清除开头 [词性] 方括号标注
        .replace(Regex("^\\[.*?\\]"), "")
        // 2. 清除首尾波浪号 ~
        .replace(Regex("^~+|~+$"), "")
        // 3. 同时匹配半角( 与全角（，删除括号前空格 + 括号及后面所有内容
        .replace(Regex("\\s*[(（].*"), "")
        // 4. 清除末尾空格+带圈数字①~⑩
        .replace(Regex("\\s*[①②③④⑤⑥⑦⑧⑨⑩]$"), "")
        // 最后清理一遍多余空格
        .trim()
}