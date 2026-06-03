package com.example.japaneselearningapp

import android.app.Application
import android.util.Log

class JapaneseLearningApplication : Application() {

    companion object {
        private var instance: JapaneseLearningApplication? = null
        fun getInstance() = instance!!
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d("JapaneseLearningApp", "Application onCreate")
    }
}