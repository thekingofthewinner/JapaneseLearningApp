package com.example.japaneselearningapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "lesson_table",
    foreignKeys = [ForeignKey(
        entity = LevelEntity::class,
        parentColumns = ["id"],
        childColumns = ["levelId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class LessonEntity(
    @PrimaryKey val id: Int,
    val levelId: Int,      // 关联等级ID
    val lessonNum: Int,    // 课时 1-25
    val lessonName: String // 课时名称，如 "第1课"
)