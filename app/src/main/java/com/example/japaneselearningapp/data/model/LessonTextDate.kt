package com.example.japaneselearningapp.data.model

data class LessonTextData(
    val lessonId: Int,
    val textContent: String,
    val voicePath: String? = null
)

