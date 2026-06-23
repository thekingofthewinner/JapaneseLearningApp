package com.example.japaneselearningapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.japaneselearningapp.data.entity.WordEntity
import com.example.japaneselearningapp.data.model.WordData
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Insert
    suspend fun insertWord(word: WordEntity)

    @Query("SELECT * FROM word_table WHERE lessonId = :lessonId")
    fun getWordsByLesson(lessonId: Int): Flow<List<WordEntity>>

    @Query("SELECT * FROM word_table ORDER BY lessonId ASC")
    fun getAllWords(): Flow<List<WordEntity>>
}