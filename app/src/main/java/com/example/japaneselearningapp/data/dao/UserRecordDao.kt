package com.example.japaneselearningapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.japaneselearningapp.data.entity.UserRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserRecordDao {
    @Insert
    suspend fun insertRecord(record: UserRecordEntity)

    @Update
    suspend fun updateRecord(record: UserRecordEntity)

    @Query("SELECT * FROM user_record LIMIT 1")
    fun getUserRecord(): Flow<UserRecordEntity?>
}