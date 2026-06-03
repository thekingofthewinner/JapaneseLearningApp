package com.example.japaneselearningapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.japaneselearningapp.data.entity.LevelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelDao {
    @Insert
    suspend fun insertLevels(levels: List<LevelEntity>)

    @Query("SELECT * FROM level_table ORDER BY levelSort ASC")
    fun getAllLevels(): Flow<List<LevelEntity>>
}