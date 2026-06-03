package com.example.japaneselearningapp.data.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_table")
data class LevelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val levelName: String, // N5/N4/N3/N2/N1
    val levelSort: Int      // 排序用 1-5
)
