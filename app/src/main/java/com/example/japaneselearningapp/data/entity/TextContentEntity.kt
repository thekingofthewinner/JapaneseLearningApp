package com.example.japaneselearningapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.japaneselearningapp.data.entity.LessonEntity
@Entity(
    tableName = "text_content",
    foreignKeys = [ForeignKey(
        entity = LessonEntity::class,
        parentColumns = ["id"],
        childColumns = ["lessonId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class TextContentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lessonId: Int,         // 关联课时ID
    val textContent: String,   // 课文文本
    val voicePath: String? = null // 音频文件路径（可选）
)