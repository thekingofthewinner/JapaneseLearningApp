package com.example.japaneselearningapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.japaneselearningapp.ui.theme.JapaneseLearningAppTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JapaneseLearningAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JapaneseLearningHomePage(
                        onLevelClick = { level ->
                            // 点击 N5/N4... 跳转到课时页面
                            val intent = Intent(this@MainActivity, LessonActivity::class.java)
                            intent.putExtra("LEVEL_NAME", level)
                            startActivity(intent)
                        },
                        onFunctionClick = { function ->
                            when (function) {
                                "conversion" -> {
                                    // 会话练习 - 跳转到 Live2DActivity
                                    val intent = Intent(this@MainActivity, Live2DActivity::class.java)
                                    startActivity(intent)
                                }
                                else -> {
                                    // 其他功能暂未实现
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// ---------------------- 首页 UI ----------------------
@Composable
fun JapaneseLearningHomePage(
    onLevelClick: (String) -> Unit,
    onFunctionClick: (String) -> Unit
) {
    // 用固定比例的 Column 来控制整体布局
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. 顶部图片（bar.png）
        Image(
            painter = painterResource(id = R.drawable.bar),
            contentDescription = "顶部栏",
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f),
            contentScale = ContentScale.Crop
        )

// 中间等级按钮区（占屏幕 70%，纯图片按钮，无文字）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // 把等级和图片资源对应起来
            val levelList = listOf(
                "N5" to R.drawable.a5,
                "N4" to R.drawable.a4,
                "N3" to R.drawable.a3,
                "N2" to R.drawable.a2,
                "N1" to R.drawable.a1
            )

            levelList.forEach { (level, drawableId) ->
                Button(
                    onClick = { onLevelClick(level) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent // 透明背景，让图片完全显示
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(0),
                    contentPadding = PaddingValues(0.dp) // 去掉按钮默认内边距，让图片铺满
                ) {
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = "$level 等级按钮",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // 图片自动铺满按钮，裁剪多余部分
                    )
                }
            }
        }

        // 3. 中间间距（占屏幕 5%，N1按钮和底部按钮之间的空隙）
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .weight(0.05f)
//        )

        // 4. 底部功能按钮区（占屏幕 10%，3个无圆角方块按钮，平分一行）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.1f)
        ) {
            val functionList = listOf(
                "conversion" to R.drawable.conversation,
                "example" to R.drawable.example,
                "vocabulary" to R.drawable.vocabulary
            )

            functionList.forEach { (func, drawableId) ->
                Button(
                    onClick = { onFunctionClick(func) },
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f), // 平分宽度
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent // 透明背景，让图片完全显示
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(0), // 无圆角，纯方块
                    contentPadding = PaddingValues(0.dp) // 去掉按钮默认内边距，让图片铺满
                ) {
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = "$func 功能按钮",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // 图片自动铺满按钮，裁剪多余部分
                    )
                }
            }
        }
    }
}
// ---------------------- 预览入口 ----------------------
        @Preview(showBackground = true, name = "首页预览")
        @Composable
        fun JapaneseLearningHomePagePreview() {
            JapaneseLearningAppTheme {
                JapaneseLearningHomePage(
                    onLevelClick = {},
                    onFunctionClick = {}
                )
            }
        }