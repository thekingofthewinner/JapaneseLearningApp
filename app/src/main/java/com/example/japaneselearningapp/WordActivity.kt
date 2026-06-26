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
}

@Composable
fun WordListPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val allWords by database.wordDao().getAllWords().collectAsState(initial = emptyList())

    // 按 lessonId 分组
    val wordsByLesson = remember(allWords) {
        allWords.groupBy { it.lessonId }.toSortedMap()
    }

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
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Transparent)
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.3f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "单词列表",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 单词列表（支持滑动）
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                wordsByLesson.forEach { (lessonId, words) ->
                    item {
                        // 课时标题
                        Text(
                            text = "第 ${lessonId} 课",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(words) { word ->
                        WordCard(
                            word = word,
                            onLongPress = {
                                // 长按播放 TTS
                                if (TtsConfig.isInitialized()) {
                                    TtsConfig.speak(word.wordJp, "ja",STYLE_6, 0.7f) { success, error ->
                                        if (!success) {
                                            Log.e("WordActivity", "TTS 播放失败: $error")
                                        }
                                    }
                                }
                            }
                        )
                    }

                    // 课时之间的间隔
                    item {
                        Box(modifier = Modifier.height(16.dp))
                    }
                }
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
    var pressJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            // 玻璃板效果 - 更厚实的阴影和半透明背景
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
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
                        // 开始计时，0.5秒后触发长按
                        pressJob = CoroutineScope(Dispatchers.Main).launch {
                            delay(500)
                            onLongPress()
                        }
                        tryAwaitRelease()
                        isPressed = false
                        pressJob?.cancel()
                    }
                )
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 日文单词
            Text(
                text = word.wordJp,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )

            // 中文释义
            Text(
                text = word.wordCn,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}
