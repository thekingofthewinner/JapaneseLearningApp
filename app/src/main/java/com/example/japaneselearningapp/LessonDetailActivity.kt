package com.example.japaneselearningapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.japaneselearningapp.data.AppDatabase
import com.example.japaneselearningapp.data.entity.GrammarEntity
import com.example.japaneselearningapp.data.entity.TextContentEntity
import com.example.japaneselearningapp.data.entity.WordEntity
import com.example.japaneselearningapp.ui.theme.JapaneseLearningAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LessonDetailActivity : ComponentActivity() {

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lessonId = intent.getIntExtra("LESSON_ID", 1)
        val lessonName = intent.getStringExtra("LESSON_NAME") ?: "第1課"

        database = AppDatabase.getInstance(this)

        setContent {
            JapaneseLearningAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LessonDetailPage(
                        lessonId = lessonId,
                        lessonName = lessonName,
                        database = database,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailPage(
    lessonId: Int,
    lessonName: String,
    database: AppDatabase,
    onBack: () -> Unit
) {
    var textContent by remember { mutableStateOf<TextContentEntity?>(null) }
    var grammarList by remember { mutableStateOf<List<GrammarEntity>>(emptyList()) }
    var wordList by remember { mutableStateOf<List<WordEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(lessonId) {
        isLoading = true
        
        android.util.Log.d("LessonDetail", "开始加载课程数据: $lessonId")
        
        kotlinx.coroutines.coroutineScope {
            launch {
                try {
                    database.textContentDao().getTextByLesson(lessonId).first { text ->
                        android.util.Log.d("LessonDetail", "课文数据: $text")
                        textContent = text
                        true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LessonDetail", "课文数据加载失败", e)
                }
            }
            launch {
                try {
                    database.grammarDao().getGrammarByLesson(lessonId).first { grammar ->
                        android.util.Log.d("LessonDetail", "语法数据数量: ${grammar.size}")
                        grammarList = grammar
                        true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LessonDetail", "语法数据加载失败", e)
                }
            }
            launch {
                try {
                    database.wordDao().getWordsByLesson(lessonId).first { words ->
                        android.util.Log.d("LessonDetail", "单词数据数量: ${words.size}")
                        wordList = words
                        true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LessonDetail", "单词数据加载失败", e)
                }
            }
        }
        isLoading = false
        android.util.Log.d("LessonDetail", "数据加载完成, 课文: ${textContent != null}, 语法: ${grammarList.size}, 单词: ${wordList.size}")
    }

    var isPlaying by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(false) }
    var showWords by remember { mutableStateOf(false) }
    var showGrammar by remember { mutableStateOf(false) }
    var showQuiz by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部 15% 区域 - 标题
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.15f)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4A90E2),
                                    Color(0xFF357ABD)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = lessonName,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(200.dp)
                                .height(2.dp)
                                .background(Color(0xFF87CEEB))
                        )
                    }
                }

                // 中间 75% 区域 - 对话气泡
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.75f)
                        .background(Color.White)
                ) {
                    if (textContent != null) {
                        ConversationBubbles(
                            textContent = textContent!!.textContent
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "教材の内容はまだありません",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // 底部 10% 区域 - 控制栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.1f)
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧按钮组
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 循环播放按钮
                            IconButton(
                                onClick = { isLooping = !isLooping }
                            ) {
                                Icon(
                                    imageVector = if (isLooping) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                    contentDescription = "循环播放",
                                    tint = if (isLooping) Color(0xFF4A90E2) else Color(0xFF87CEEB),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // 单词展示按钮
                            IconButton(
                                onClick = { showWords = !showWords }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = "单词",
                                    tint = if (showWords) Color(0xFF4A90E2) else Color(0xFF87CEEB),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // 中间播放按钮
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    Color(0xFF4A90E2),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "暂停" else "播放",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // 右侧按钮组
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 语法展示按钮
                            IconButton(
                                onClick = { showGrammar = !showGrammar }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = "语法",
                                    tint = if (showGrammar) Color(0xFF4A90E2) else Color(0xFF87CEEB),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // 习题练习按钮
                            IconButton(
                                onClick = { showQuiz = !showQuiz }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Quiz,
                                    contentDescription = "习题",
                                    tint = if (showQuiz) Color(0xFF4A90E2) else Color(0xFF87CEEB),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 返回按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.White.copy(alpha = 0.3f),
                                CircleShape
                            )
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun ConversationBubbles(
    textContent: String
) {
    val lines = textContent.split("\n").filter { it.isNotBlank() }
    val conversations = parseConversations(lines)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(conversations.size) { index ->
            val conversation = conversations[index]
            val isLeft = index % 2 == 0
            ConversationBubble(
                speaker = conversation.speaker,
                text = conversation.text,
                isLeft = isLeft
            )
        }
    }
}

data class Conversation(
    val speaker: String,
    val text: String
)

fun parseConversations(lines: List<String>): List<Conversation> {
    return lines.mapNotNull { line ->
        val parts = line.split(":", limit = 2)
        if (parts.size == 2) {
            Conversation(parts[0].trim(), parts[1].trim())
        } else {
            Conversation("", line.trim())
        }
    }
}

@Composable
fun ConversationBubble(
    speaker: String,
    text: String,
    isLeft: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isLeft) Arrangement.Start else Arrangement.End
    ) {
        if (isLeft) {
            AvatarPlaceholder()
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    Color.White.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                if (speaker.isNotBlank()) {
                    Text(
                        text = speaker,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = text,
                    fontSize = 16.sp,
                    color = Color.White,
                    lineHeight = 22.sp
                )
            }
        }

        if (!isLeft) {
            Spacer(modifier = Modifier.width(8.dp))
            AvatarPlaceholder()
        }
    }
}

@Composable
fun AvatarPlaceholder() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                Color(0xFF87CEEB),
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "👤",
            fontSize = 20.sp
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LessonDetailPagePreview() {
    JapaneseLearningAppTheme {
        val mockTextContent = """田中: こんにちは、お元気ですか？
山田: はい、元気です。ありがとうございます。
田中: 今日はいい天気ですね。
山田: そうですね。散歩に行きましょうか？
田中: いいですね。一緒に行きましょう。
山田: 楽しみですね！"""

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部 15% 区域 - 标题
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.15f)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4A90E2),
                                    Color(0xFF357ABD)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "第1課",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(200.dp)
                                .height(2.dp)
                                .background(Color(0xFF87CEEB))
                        )
                    }
                }

                // 中间 75% 区域 - 对话气泡
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.75f)
                        .background(Color.White)
                ) {
                    ConversationBubbles(
                        textContent = mockTextContent
                    )
                }

                // 底部 10% 区域 - 控制栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.1f)
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧按钮组
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Repeat,
                                    contentDescription = "循环播放",
                                    tint = Color(0xFF87CEEB),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(
                                onClick = { }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = "单词",
                                    tint = Color(0xFF87CEEB),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // 中间播放按钮
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    Color(0xFF4A90E2),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "播放",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // 右侧按钮组
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = "语法",
                                    tint = Color(0xFF87CEEB),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(
                                onClick = { }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Quiz,
                                    contentDescription = "习题",
                                    tint = Color(0xFF87CEEB),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 返回按钮
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.3f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GrammarItem(grammar: GrammarEntity) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = grammar.grammarTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        grammar.grammarDetail.split("\n").forEach { line ->
            if (line.isNotBlank()) {
                Text(
                    text = line,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun WordRow(word: WordEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = word.wordJp,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )

        Text(
            text = word.wordCn,
            fontSize = 14.sp,
            color = Color.DarkGray,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )

        Text(
            text = word.wordPron ?: "",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
    }
}