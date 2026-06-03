package com.example.japaneselearningapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_table",
    foreignKeys = [ForeignKey(
        entity = LessonEntity::class,
        parentColumns = ["id"],
        childColumns = ["lessonId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lessonId: Int,       // 关联课时ID
    val wordJp: String,      // 日文单词
    val wordCn: String,      // 中文释义
    val wordPron: String? = null // 读音（可选）
)