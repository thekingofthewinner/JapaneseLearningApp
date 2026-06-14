package com.example.japaneselearningapp.network

import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object MockApiService {
    private const val TAG = "MockApiService"
    
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    
    private val mockResponses = mapOf(
        "こんにちは" to listOf(
            MockResponse("こんにちは！私は春です。", "ja"),
            MockResponse("はじめまして、よろしくお願いします！", "ja"),
            MockResponse("今日はいい天気ですね。", "ja")
        ),
        "hello" to listOf(
            MockResponse("Hello! My name is Haru.", "en"),
            MockResponse("Nice to meet you!", "en"),
            MockResponse("How can I help you today?", "en")
        ),
        "你好" to listOf(
            MockResponse("你好！我是春。", "zh"),
            MockResponse("很高兴认识你！", "zh"),
            MockResponse("今天天气真好啊。", "zh")
        ),
        "test" to listOf(
            MockResponse("こんにちは！私はAIアシスタントの春です。", "ja"),
            MockResponse("どのようなことにお手伝いできますか？", "ja"),
            MockResponse("日本語の学習をお手伝いしますよ！", "ja")
        ),
        "default" to listOf(
            MockResponse("ご質問ありがとうございます！", "ja"),
            MockResponse("私はいつでもお手伝いできますよ。", "ja"),
            MockResponse("どうぞお気軽にお問い合わせください。", "ja")
        )
    )
    
    data class MockResponse(val text: String, val language: String)
    
    // 流式回调接口（保留，因为不能简化为 lambda）
    interface MockStreamingCallback {
        fun onChunk(chunk: String, language: String)
        fun onComplete()
        fun onError(error: String)
    }
    
    fun getMockResponse(prompt: String, callback: (String, String) -> Unit) {
        executor.execute {
            try {
                Thread.sleep(1500)
                
                val responses = when {
                    prompt.contains("こんにちは") -> mockResponses["こんにちは"]
                    prompt.contains("hello", ignoreCase = true) || prompt.contains("hi", ignoreCase = true) -> mockResponses["hello"]
                    prompt.contains("你好") -> mockResponses["你好"]
                    prompt.contains("test", ignoreCase = true) -> mockResponses["test"]
                    else -> mockResponses["default"]
                } ?: mockResponses["default"]
                
                val randomResponse = responses?.random() ?: mockResponses["default"]?.first()
                randomResponse?.let {
                    callback(it.text, it.language)
                }
                
            } catch (e: InterruptedException) {
                Log.e(TAG, "Mock 请求被中断: ${e.message}")
            }
        }
    }
    
    fun getMockStreamingResponse(prompt: String, callback: MockStreamingCallback): Runnable {
        val responses = when {
            prompt.contains("こんにちは") -> mockResponses["こんにちは"]
            prompt.contains("hello", ignoreCase = true) || prompt.contains("hi", ignoreCase = true) -> mockResponses["hello"]
            prompt.contains("你好") -> mockResponses["你好"]
            prompt.contains("test", ignoreCase = true) -> mockResponses["test"]
            else -> mockResponses["default"]
        } ?: mockResponses["default"]
        
        val selectedResponse = responses?.random() ?: mockResponses["default"]?.first()
        val text = selectedResponse?.text ?: "ご質問ありがとうございます！"
        val language = selectedResponse?.language ?: "ja"
        
        var index = 0
        val runnable = Runnable {
            try {
                while (index < text.length) {
                    val chunkSize = if (text.length - index > 3) (1..3).random() else text.length - index
                    val chunk = text.substring(index, index + chunkSize)
                    index += chunkSize
                    
                    callback.onChunk(chunk, language)
                    Thread.sleep(300 + (Math.random() * 200).toLong())
                }
                callback.onComplete()
            } catch (e: InterruptedException) {
                Log.e(TAG, "Mock 流式请求被中断: ${e.message}")
                callback.onError("请求被中断")
            }
        }
        
        executor.execute(runnable)
        return runnable
    }
    
    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }
}