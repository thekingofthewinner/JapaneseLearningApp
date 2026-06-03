package com.example.japaneselearningapp.data.dao

import com.example.japaneselearningapp.data.entity.TextContentEntity
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TextContentDao {
    @Insert
    suspend fun insertText(text: TextContentEntity)

    @Insert
    suspend fun insertTextList(texts: List<TextContentEntity>)

    @Query("SELECT * FROM text_content WHERE lessonId = :lessonId LIMIT 1")
    fun getTextByLesson(lessonId: Int): Flow<TextContentEntity?>

    @Query("INSERT INTO text_content(lessonId, textContent) VALUES (:lessonId, :textContent)")
    suspend fun insertTextContent(lessonId: Int, textContent: String)
}