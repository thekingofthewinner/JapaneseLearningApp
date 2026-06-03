package com.example.japaneselearningapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.japaneselearningapp.data.entity.GrammarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GrammarDao {
    @Insert
    suspend fun insertGrammar(grammar: GrammarEntity)

    @Insert
    suspend fun insertGrammarList(grammars: List<GrammarEntity>)

    @Query("SELECT * FROM grammar_table WHERE lessonId = :lessonId")
    fun getGrammarByLesson(lessonId: Int): Flow<List<GrammarEntity>>
}