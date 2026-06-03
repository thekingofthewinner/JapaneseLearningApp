package com.example.japaneselearningapp.data.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "grammar_table",
    foreignKeys = [ForeignKey(
        entity = LessonEntity::class,
        parentColumns = ["id"],
        childColumns = ["lessonId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class GrammarEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lessonId: Int,       // 关联课时ID
    val grammarTitle: String,// 语法标题
    val grammarDetail: String // 语法详情+例句
)
