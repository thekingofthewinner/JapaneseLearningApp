package com.example.japaneselearningapp.data

import android.annotation.SuppressLint
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.japaneselearningapp.data.dao.GrammarDao
import com.example.japaneselearningapp.data.dao.LessonDao
import com.example.japaneselearningapp.data.dao.LevelDao
import com.example.japaneselearningapp.data.dao.TextContentDao
import com.example.japaneselearningapp.data.dao.UserRecordDao
import com.example.japaneselearningapp.data.dao.WordDao
import com.example.japaneselearningapp.data.entity.LevelEntity
import com.example.japaneselearningapp.data.entity.LessonEntity
import com.example.japaneselearningapp.data.entity.GrammarEntity
import com.example.japaneselearningapp.data.entity.WordEntity
import com.example.japaneselearningapp.data.entity.UserRecordEntity
import com.example.japaneselearningapp.data.entity.TextContentEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers


@Database(
    entities = [
        LevelEntity::class,
        LessonEntity::class,
        TextContentEntity::class,
        GrammarEntity::class,
        WordEntity::class,
        UserRecordEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun levelDao(): LevelDao
    abstract fun lessonDao(): LessonDao
    abstract fun textContentDao(): TextContentDao
    abstract fun grammarDao(): GrammarDao
    abstract fun wordDao(): WordDao
    abstract fun userRecordDao(): UserRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private var isInitialized = false


        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                android.util.Log.d("AppDatabase", "开始创建数据库实例")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "japanese_learning_db"
                ).fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                android.util.Log.d("AppDatabase", "数据库实例创建完成")
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                android.util.Log.d("AppDatabase", "数据库onCreate回调被触发")
                
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val database = INSTANCE
                        if (database != null) {
                            initializeData(database, context)
                            isInitialized = true
                            android.util.Log.d("AppDatabase", "数据库初始化完成并标记")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AppDatabase", "数据初始化异常", e)
                    }
                }
            }

            override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onOpen(db)
                android.util.Log.d("AppDatabase", "数据库onOpen回调被触发")
                
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val database = INSTANCE
                        if (database != null && !isInitialized) {
                            val existingLessonCount = database.lessonDao().getLessonCount()
                            if (existingLessonCount == 0) {
                                initializeData(database, context)
                                isInitialized = true
                                android.util.Log.d("AppDatabase", "数据库onOpen中初始化完成")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AppDatabase", "onOpen数据初始化异常", e)
                    }
                }
            }
        }

        private fun initializeData(database: AppDatabase, context: Context) {
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    android.util.Log.d("AppDatabase", "开始初始化数据库数据")
                    
                    // 检查是否已经初始化过数据
                    val existingLessonCount = database.lessonDao().getLessonCount()
                    if (existingLessonCount > 0) {
                        android.util.Log.d("AppDatabase", "检测到已有数据，跳过初始化")
                        return@launch
                    }
                    // 初始化等级数据
                    val levels = listOf(
                        LevelEntity(levelName = "N5", levelSort = 1),
                        LevelEntity(levelName = "N4", levelSort = 2),
                        LevelEntity(levelName = "N3", levelSort = 3),
                        LevelEntity(levelName = "N2", levelSort = 4),
                        LevelEntity(levelName = "N1", levelSort = 5)
                    )
                    database.levelDao().insertLevels(levels)
                    android.util.Log.d("AppDatabase", "等级数据初始化完成: ${levels.size}个等级")

                    // 初始化课时数据（每个等级25课）
                    // JSON文件中的lessonId直接对应LessonEntity的id
                    // N5: lessonId 1-25, N4: lessonId 26-50, N3: lessonId 51-75, N2: lessonId 76-100, N1: lessonId 101-125
                    val lessons = mutableListOf<LessonEntity>()
                    for (lessonId in 1..125) {
                        val levelIndex = (lessonId - 1) / 25  // 0 for N5(lessons 1-25), 1 for N4(lessons 26-50), etc.
                        val actualLevelId = levelIndex + 1  // levelId is 1, 2, 3, 4, 5 for N5, N4, N3, N2, N1
                        val lessonNum = (lessonId - 1) % 25 + 1  // 1-25 for each level
                        lessons.add(
                            LessonEntity(
                                id = lessonId,  // JSON中的lessonId直接用作数据库ID
                                levelId = actualLevelId,  // levelId is 1 for N5, 2 for N4, etc.
                                lessonNum = lessonNum,
                                lessonName = "第${lessonNum}课"
                            )
                        )
                    }
                    database.lessonDao().insertLessons(lessons)
                    android.util.Log.d("AppDatabase", "课时数据初始化完成: ${lessons.size}个课时")

                    // 从JSON文件加载文本内容
                    try {
                        loadTextContentFromJson(database, context)
                    } catch (e: Exception) {
                        android.util.Log.e("AppDatabase", "加载课文数据失败", e)
                    }

                    // 从JSON文件加载单词数据
                    try {
                        loadWordsFromJson(database, context)
                    } catch (e: Exception) {
                        android.util.Log.e("AppDatabase", "加载单词数据失败", e)
                    }

                    // 从JSON文件加载语法数据
                    try {
                        loadGrammarFromJson(database, context)
                    } catch (e: Exception) {
                        android.util.Log.e("AppDatabase", "加载语法数据失败", e)
                    }
                    android.util.Log.d("AppDatabase", "数据库数据初始化完成")

                } catch (e: Exception) {
                    e.printStackTrace()
                    android.util.Log.e("AppDatabase", "数据库初始化失败", e)
                }
            }
        }

        private suspend fun loadTextContentFromJson(database: AppDatabase, context: Context) {
            try {
                val jsonString = context.assets.open("lesson_texts.json").bufferedReader().use { it.readText() }
                val gson = com.google.gson.Gson()
                val textContents = gson.fromJson(jsonString, Array<com.example.japaneselearningapp.data.model.LessonTextData>::class.java)
                android.util.Log.d("AppDatabase", "从JSON加载课文数据: ${textContents.size}个")
                
                if (textContents.isNotEmpty()) {
                    val textContentEntities = textContents.map { textData ->
                        TextContentEntity(
                            lessonId = textData.lessonId,
                            textContent = textData.textContent,
                            voicePath = textData.voicePath
                        )
                    }
                    database.textContentDao().insertTextList(textContentEntities)
                    android.util.Log.d("AppDatabase", "课文数据插入完成")
                }
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "课文数据加载失败", e)
                e.printStackTrace()
            }
        }

//        @SuppressLint("SuspiciousIndentation")
        private suspend fun loadWordsFromJson(database: AppDatabase, context: Context) {
            try {
                val jsonString = context.assets.open("words.json").bufferedReader().use { it.readText() }
                val gson = com.google.gson.Gson()
                val words = gson.fromJson(jsonString, Array<com.example.japaneselearningapp.data.model.WordData>::class.java)
                android.util.Log.d("AppDatabase", "从JSON加载单词数据: ${words.size}个")
                words.forEach { wordData ->
                    val wordEntity = WordEntity(
                        lessonId = wordData.lessonId,
                        wordJp = wordData.japaneseWord,
                        wordCn = wordData.chineseMeaning,
                        wordPron = wordData.pronunciation
                    )
                    database.wordDao().insertWord(wordEntity)
                }
                android.util.Log.d("AppDatabase", "单词数据插入完成")

            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "单词数据加载失败", e)
                e.printStackTrace()
            }
        }

        private suspend fun loadGrammarFromJson(database: AppDatabase, context: Context) {
            try {
                val jsonString = context.assets.open("grammar.json").bufferedReader().use { it.readText() }
                val gson = com.google.gson.Gson()
                val grammars = gson.fromJson(jsonString, Array<com.example.japaneselearningapp.data.model.GrammarData>::class.java)
                android.util.Log.d("AppDatabase", "从JSON加载语法数据: ${grammars.size}个")

                grammars.forEach { grammarData ->
                    val grammar = GrammarEntity(
                        lessonId = grammarData.lessonId,
                        grammarTitle = grammarData.grammarPoint,
                        grammarDetail = "${grammarData.explanation}\n\n例句：\n${grammarData.examples}"

                    )
                    database.grammarDao().insertGrammar(grammar)
                }
                android.util.Log.d("AppDatabase", "语法数据插入完成")
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "语法数据加载失败", e)
                e.printStackTrace()
            }
        }
    }
}