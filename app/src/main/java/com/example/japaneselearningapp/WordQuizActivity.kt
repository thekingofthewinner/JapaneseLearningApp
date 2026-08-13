// C:/IdeaProjects/JapaneseLearningApp/app/src/main/java/com/example/japaneselearningapp/WordQuizActivity.kt
package com.example.japaneselearningapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onGloballyPositioned
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japaneselearningapp.data.AppDatabase
import com.example.japaneselearningapp.data.entity.WordEntity
import com.example.japaneselearningapp.ui.theme.JapaneseLearningAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

class WordQuizActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 让内容延伸到状态栏/导航栏区域，配合 WindowInsets.safeDrawing
        // 才能正确避开前置摄像头（刘海/挖孔）和底部导航横条。
        // 用 WindowCompat 替代 activity-ktx 的 enableEdgeToEdge()，避免额外依赖要求。
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val lessonId = intent.getIntExtra("LESSON_ID", 1)

        setContent {
            JapaneseLearningAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WordQuizPage(lessonId = lessonId)
                }
            }
        }
    }
}

@Composable
fun WordQuizPage(lessonId: Int) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val allWords by database.wordDao().getWordsByLesson(lessonId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var quizWords by remember { mutableStateOf<List<WordEntity>>(emptyList()) }
    var currentWordIndex by remember { mutableStateOf(0) }
    var currentQuiz by remember { mutableStateOf<QuizData?>(null) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }
    var showCompletionConfetti by remember { mutableStateOf(false) }
    var completedCount by remember { mutableStateOf(0) }

    var parties by remember { mutableStateOf<List<Party>>(emptyList()) }
    // 每次发起一轮新彩蛋就自增，配合 key() 强制 KonfettiView 重建子树，
    // 让其内部 LaunchedEffect(Unit) 重新读取 parties 并喷射。
    var confettiKey by remember { mutableStateOf(0) }

    // 精确捕捉单词框在最外层 Box（与 KonfettiView 同尺寸）中的位置
    var containerWidth by remember { mutableStateOf(0) }
    var containerHeight by remember { mutableStateOf(0) }
    var wordBoxLeftRel by remember { mutableStateOf(0.05) }   // 左 x 相对坐标 (0~1)
    var wordBoxRightRel by remember { mutableStateOf(0.95) }  // 右 x 相对坐标
    var wordBoxBottomRel by remember { mutableStateOf(0.45) } // 底 y 相对坐标

    // 根据 currentWordIndex 生成下一道题（复用，避免重复代码）
    fun generateNextQuiz() {
        if (currentWordIndex < quizWords.size) {
            val currentWord = quizWords[currentWordIndex]
            val displayJapanese = kotlin.random.Random.nextBoolean()

            val wrongOptions = quizWords
                .filter { it.id != currentWord.id }
                .shuffled()
                .take(3)

            val allOptions = (wrongOptions + currentWord).shuffled()
            val correctIndex = allOptions.indexOf(currentWord)

            currentQuiz = QuizData(
                word = currentWord,
                displayJapanese = displayJapanese,
                options = allOptions,
                correctIndex = correctIndex
            )
            selectedOptionIndex = null
            isCorrect = null
        }
    }

    LaunchedEffect(allWords) {
        if (allWords.isNotEmpty()) {
            quizWords = allWords.shuffled()
            currentWordIndex = 0
            selectedOptionIndex = null
            isCorrect = null
            completedCount = 0
            showCompletionConfetti = false

            // 直接生成第一个题目
            generateNextQuiz()
        }
    }

    fun handleOptionClick(index: Int) {
        if (selectedOptionIndex != null) return

        selectedOptionIndex = index
        val quiz = currentQuiz ?: return
        val correct = index == quiz.correctIndex
        isCorrect = correct

        if (correct) {
            completedCount++
            confettiKey++

            // 彩蛋从单词框的左右下角精确喷出（通过 onGloballyPositioned 实时定位）
            parties = listOf(
                Party(
                    emitter = Emitter(duration = 1, TimeUnit.SECONDS).perSecond(100),
                    position = Position.Relative(wordBoxLeftRel, wordBoxBottomRel),
                    colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def)
                ),
                Party(
                    emitter = Emitter(duration = 1, TimeUnit.SECONDS).perSecond(100),
                    position = Position.Relative(wordBoxRightRel, wordBoxBottomRel),
                    colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def)
                )
            )
        }

        // 无论对错，延迟后都自动切换下一题；选错时延长延迟以便看清正确答案
        scope.launch {
            delay(if (correct) 1500 else 2000)
            parties = emptyList()

            currentWordIndex++
            if (currentWordIndex >= quizWords.size) {
                showCompletionConfetti = true
                confettiKey++
                // timeToLive 从默认 2000ms 延长到 5000ms，
                // 确保从顶部 (0.5, 0.0) 出发的彩带能一路落到底部再消散。
                parties = listOf(
                    Party(
                        emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(200),
                        position = Position.Relative(0.5, 0.0),
                        timeToLive = 5000,
                        colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def)
                    )
                )
            } else {
                generateNextQuiz()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                containerWidth = coords.size.width
                containerHeight = coords.size.height
            }
    ) {
        // safeDrawing 包含 statusBars（含刘海/挖孔）+ 底部导航栏，
        // 确保标题"单词训练"不会被前置摄像头挡住，底部也不会被横条遮挡。
        val safePadding = WindowInsets.safeDrawing.asPaddingValues()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(
                    top = safePadding.calculateTopPadding() + 8.dp,
                    bottom = safePadding.calculateBottomPadding() + 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "单词训练",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (showCompletionConfetti) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎉 完成本课单词训练！",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "共完成 ${completedCount} 个单词",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            } else {
                currentQuiz?.let { quiz ->
                    WordDisplayBox(
                        word = quiz.word,
                        displayJapanese = quiz.displayJapanese,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.dp)
                            .weight(0.3f)
                            .padding(horizontal = 16.dp)
                            .onGloballyPositioned { coords ->
                                if (containerWidth > 0 && containerHeight > 0) {
                                    val boundsInContainer =
                                        coords.boundsInParent() // 在外层 Box 坐标系中
                                    wordBoxLeftRel =
                                        boundsInContainer.left.toDouble() / containerWidth
                                    wordBoxRightRel =
                                        boundsInContainer.right.toDouble() / containerWidth
                                    wordBoxBottomRel =
                                        boundsInContainer.bottom.toDouble() / containerHeight
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OptionsGrid(
                        options = quiz.options,
                        displayJapanese = quiz.displayJapanese,
                        selectedIndex = selectedOptionIndex,
                        correctIndex = quiz.correctIndex,
                        isCorrect = isCorrect,
                        onOptionClick = { handleOptionClick(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.dp)
                            .weight(0.7f)
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // KonfettiView 内部用 LaunchedEffect(Unit) 只在进入组合时读取 parties，
        // 因此必须条件挂载：parties 非空时才把它放入组合，清空时移除；
        // 再用 key(confettiKey) 强制每轮新彩蛋重建子树，确保连续两轮（如答对后接完成彩蛋）
        // 不会因未卸载而漏喷。
        if (parties.isNotEmpty()) {
            key(confettiKey) {
                KonfettiView(
                    modifier = Modifier.fillMaxSize(),
                    parties = parties
                )
            }
        }
    }
}

data class QuizData(
    val word: WordEntity,
    val displayJapanese: Boolean,
    val options: List<WordEntity>,
    val correctIndex: Int
)

@Composable
fun WordDisplayBox(
    word: WordEntity,
    displayJapanese: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (displayJapanese) word.wordJp else word.wordCn,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OptionsGrid(
    options: List<WordEntity>,
    displayJapanese: Boolean,
    selectedIndex: Int?,
    correctIndex: Int,
    isCorrect: Boolean?,
    onOptionClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptionButton(
                text = if (displayJapanese) options[0].wordCn else options[0].wordJp,
                isSelected = selectedIndex == 0,
                isCorrectOption = correctIndex == 0,
                isAnswered = isCorrect != null,
                isCorrect = isCorrect,
                onClick = { onOptionClick(0) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            OptionButton(
                text = if (displayJapanese) options[1].wordCn else options[1].wordJp,
                isSelected = selectedIndex == 1,
                isCorrectOption = correctIndex == 1,
                isAnswered = isCorrect != null,
                isCorrect = isCorrect,
                onClick = { onOptionClick(1) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptionButton(
                text = if (displayJapanese) options[2].wordCn else options[2].wordJp,
                isSelected = selectedIndex == 2,
                isCorrectOption = correctIndex == 2,
                isAnswered = isCorrect != null,
                isCorrect = isCorrect,
                onClick = { onOptionClick(2) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            OptionButton(
                text = if (displayJapanese) options[3].wordCn else options[3].wordJp,
                isSelected = selectedIndex == 3,
                isCorrectOption = correctIndex == 3,
                isAnswered = isCorrect != null,
                isCorrect = isCorrect,
                onClick = { onOptionClick(3) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
fun OptionButton(
    text: String,
    isSelected: Boolean,
    isCorrectOption: Boolean,
    isAnswered: Boolean,
    isCorrect: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 已作答后：正确答案恒为绿；选错的选项为红；其余为白
    val backgroundColor = when {
        !isAnswered -> Color.White
        isCorrectOption -> Color(0xFF4CAF50)
        isSelected && isCorrect == false -> Color(0xFFF44336)
        else -> Color.White
    }

    val textColor = when {
        !isAnswered -> Color.Black
        isCorrectOption -> Color.White
        isSelected && isCorrect == false -> Color.White
        else -> Color.Black
    }

    Box(
        modifier = modifier
            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !isAnswered) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}