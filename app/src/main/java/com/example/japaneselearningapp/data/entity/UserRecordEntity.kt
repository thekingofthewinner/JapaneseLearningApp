package com.example.japaneselearningapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_record")
data class UserRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lastLevelId: Int?,   // 上次选择的等级ID
    val lastLessonId: Int?   // 上次选择的课时ID
)