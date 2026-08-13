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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    var showConfetti by remember { mutableStateOf(false) }
    var showCompletionConfetti by remember { mutableStateOf(false) }
    var completedCount by remember { mutableStateOf(0) }

    var parties by remember { mutableStateOf<List<Party>>(emptyList()) }

    LaunchedEffect(allWords) {
        if (allWords.isNotEmpty()) {
            quizWords = allWords.shuffled()
            currentWordIndex = 0
            selectedOptionIndex = null
            isCorrect = null
            completedCount = 0
            showCompletionConfetti = false

            // 直接生成第一个题目
            if (quizWords.isNotEmpty()) {
                val currentWord = quizWords[0]
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
            }
        }
    }

//    val generateQuiz = remember {
//        {
//            if (currentWordIndex < quizWords.size) {
//                val currentWord = quizWords[currentWordIndex]
//                val displayJapanese = kotlin.random.Random.nextBoolean()
//
//                val wrongOptions = quizWords
//                    .filter { it.id != currentWord.id }
//                    .shuffled()
//                    .take(3)
//
//                val allOptions = (wrongOptions + currentWord).shuffled()
//                val correctIndex = allOptions.indexOf(currentWord)
//
//                currentQuiz = QuizData(
//                    word = currentWord,
//                    displayJapanese = displayJapanese,
//                    options = allOptions,
//                    correctIndex = correctIndex
//                )
//                selectedOptionIndex = null
//                isCorrect = null
//            }
//        }
//    }

    fun handleOptionClick(index: Int) {
        if (selectedOptionIndex != null) return

        selectedOptionIndex = index
        val quiz = currentQuiz ?: return
        val correct = index == quiz.correctIndex
        isCorrect = correct

        if (correct) {
            completedCount++
            showConfetti = true

            parties = listOf(
                Party(
                    emitter = Emitter(duration = 1, TimeUnit.SECONDS).perSecond(100),
                    position = Position.Relative(0.1, 1.0),
                    colors = listOf(0xF44336, 0xE91E63, 0x9C27B0, 0x673AB7, 0x3F51B5, 0x2196F3, 0x03A9F4, 0x00BCD4, 0x009688, 0x4CAF50, 0x8BC34A, 0xCDDC39, 0xFFEB3B, 0xFFC107, 0xFF9800, 0xFF5722)
                ),
                Party(
                    emitter = Emitter(duration = 1, TimeUnit.SECONDS).perSecond(100),
                    position = Position.Relative(0.9, 1.0),
                    colors = listOf(0xF44336, 0xE91E63, 0x9C27B0, 0x673AB7, 0x3F51B5, 0x2196F3, 0x03A9F4, 0x00BCD4, 0x009688, 0x4CAF50, 0x8BC34A, 0xCDDC39, 0xFFEB3B, 0xFFC107, 0xFF9800, 0xFF5722)
                )
            )

            scope.launch {
                delay(1500)
                showConfetti = false
                parties = emptyList()

                currentWordIndex++
                if (currentWordIndex >= quizWords.size) {
                    showCompletionConfetti = true
                    parties = listOf(
                        Party(
                            emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(200),
                            position = Position.Relative(0.5, 0.0),
                            colors = listOf(0xF44336, 0xE91E63, 0x9C27B0, 0x673AB7, 0x3F51B5, 0x2196F3, 0x03A9F4, 0x00BCD4, 0x009688, 0x4CAF50, 0x8BC34A, 0xCDDC39, 0xFFEB3B, 0xFFC107, 0xFF9800, 0xFF5722)
                        )
                    )
                } else {
                    // 直接生成下一个题目
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
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

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
                            .weight(0.4f)
                            .padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OptionsGrid(
                        options = quiz.options,
                        displayJapanese = quiz.displayJapanese,
                        selectedIndex = selectedOptionIndex,
                        isCorrect = isCorrect,
                        onOptionClick = { handleOptionClick(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.dp)
                            .weight(0.18f)
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }

        KonfettiView(
            modifier = Modifier.fillMaxSize(),
            parties = parties
        )
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
    isCorrect: Boolean?,
    onOptionClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OptionButton(
                text = if (displayJapanese) options[0].wordCn else options[0].wordJp,
                isSelected = selectedIndex == 0,
                isCorrect = isCorrect,
                onClick = { onOptionClick(0) },
                modifier = Modifier.weight(1f)
            )
            OptionButton(
                text = if (displayJapanese) options[1].wordCn else options[1].wordJp,
                isSelected = selectedIndex == 1,
                isCorrect = isCorrect,
                onClick = { onOptionClick(1) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OptionButton(
                text = if (displayJapanese) options[2].wordCn else options[2].wordJp,
                isSelected = selectedIndex == 2,
                isCorrect = isCorrect,
                onClick = { onOptionClick(2) },
                modifier = Modifier.weight(1f)
            )
            OptionButton(
                text = if (displayJapanese) options[3].wordCn else options[3].wordJp,
                isSelected = selectedIndex == 3,
                isCorrect = isCorrect,
                onClick = { onOptionClick(3) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun OptionButton(
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected && isCorrect == true -> Color(0xFF4CAF50)
        isSelected && isCorrect == false -> Color(0xFFF44336)
        else -> Color.White
    }

    val textColor = when {
        isSelected && isCorrect == true -> Color.White
        isSelected && isCorrect == false -> Color.White
        else -> Color.Black
    }

    Box(
        modifier = modifier
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !isSelected) { onClick() },
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