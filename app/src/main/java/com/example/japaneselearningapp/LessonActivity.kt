package com.example.japaneselearningapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japaneselearningapp.ui.theme.JapaneseLearningAppTheme

class LessonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val levelName = intent.getStringExtra("LEVEL_NAME") ?: "N5"

        setContent {
            JapaneseLearningAppTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.a6),
                            contentDescription = "背景图片",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        LessonListPage(
                            levelName = levelName,
                            onLessonClick = { lessonNum ->
                                val intent = Intent(this@LessonActivity, LessonDetailActivity::class.java)
                                intent.putExtra("LESSON_ID", lessonNum)
                                startActivity(intent)
                            },
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LessonListPage(
    levelName: String,
    onLessonClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val swipeThreshold = screenWidth * 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 右滑返回（只作用于屏幕，不影响按钮点击）
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        if (dragAmount.x > 0) {
                            offsetX = dragAmount.x
                        }
                    },
                    onDragEnd = {
                        if (offsetX > swipeThreshold) onBack()
                        offsetX = 0f
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = offsetX.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(24.dp))
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
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$levelName - 標準日本語 1～25課",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            LazyColumn(
                modifier = Modifier.padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(25) { index ->
                    val lessonNum = index + 1
                    var isPressed by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Transparent)
                            .drawWithContent {
                                drawContent()
                                drawRoundRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (isPressed) 0.6f else 0.35f),
                                            Color.Transparent,
                                            Color.Transparent,
                                            Color.White.copy(alpha = if (isPressed) 0.5f else 0.25f)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, size.height)
                                    ),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
                                )
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true
                                        tryAwaitRelease()
                                    },
                                    onTap = {
                                        isPressed = false
                                        onLessonClick(lessonNum)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "第 $lessonNum 課",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = if (isPressed) 0.9f else 1f)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- 预览 ----------------------
@Preview(showBackground = true, name = "课时列表预览")
@Composable
fun LessonListPagePreview() {
    JapaneseLearningAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF25B8A8), Color(0xFF75E4D2))
                    )
                )
        ) {
            LessonListPage("N5", {}, {})
        }
    }
}