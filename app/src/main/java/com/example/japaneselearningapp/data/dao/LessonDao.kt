package com.example.japaneselearningapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.japaneselearningapp.data.entity.LessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Insert
    suspend fun insertLessons(lessons: List<LessonEntity>)
    
    @Query("SELECT COUNT(*) FROM lesson_table")
    suspend fun getLessonCount(): Int
    
    @Query("SELECT COUNT(*) FROM lesson_table")
    fun getLessonCountSync(): Int
    
    @Query("SELECT * FROM lesson_table WHERE levelId = :levelId ORDER BY lessonNum ASC")
    fun getLessonsByLevel(levelId: Int): Flow<List<LessonEntity>>
}