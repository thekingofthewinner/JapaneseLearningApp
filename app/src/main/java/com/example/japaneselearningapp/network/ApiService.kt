package com.example.japaneselearningapp.network

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiService {
    private const val TAG = "ApiService"
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    data class ChatResponse(
        val text: String,
        val language: String = "ja"
    )
    
    interface ChatCallback {
        fun onResponse(text: String, language: String)
        fun onError(error: String)
    }
    
    interface StreamingCallback {
        fun onChunk(chunk: String, language: String)
        fun onComplete()
        fun onError(error: String)
    }
    
    fun sendChatRequest(prompt: String, callback: ChatCallback) {
        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            gson.toJson(mapOf("prompt" to prompt))
        )
        
        val request = Request.Builder()
            .url("https://api.example.com/chat")
            .post(requestBody)
            .build()
        
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "请求失败: ${e.message}")
                callback.onError(e.message ?: "网络错误")
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback.onError("HTTP 错误: ${response.code}")
                    return
                }

                response.body?.let { body ->
                    try {
                        val json = body.string()
                        val chatResponse = gson.fromJson(json, ChatResponse::class.java)
                        callback.onResponse(chatResponse.text, chatResponse.language)
                    } catch (e: Exception) {
                        callback.onError("解析错误: ${e.message}")
                    }
                } ?: callback.onError("响应为空")
            }
        })
    }
    
    fun sendStreamingChatRequest(prompt: String, callback: StreamingCallback): EventSource? {
        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            gson.toJson(mapOf("prompt" to prompt))
        )
        
        val request = Request.Builder()
            .url("https://api.example.com/chat/stream")
            .post(requestBody)
            .addHeader("Accept", "text/event-stream")
            .build()
        
        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d(TAG, "SSE 连接已打开")
            }
            
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val chunkResponse = gson.fromJson(data, ChatResponse::class.java)
                    callback.onChunk(chunkResponse.text, chunkResponse.language)
                } catch (e: Exception) {
                    callback.onError("解析错误: ${e.message}")
                }
            }
            
            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE 连接已关闭")
                callback.onComplete()
            }
            
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e(TAG, "SSE 失败: ${t?.message}")
                callback.onError(t?.message ?: "SSE 错误")
            }
        }
        
        return EventSources.createFactory(okHttpClient).newEventSource(request, listener)
    }
    
    fun cancelRequest(call: Call) {
        call.cancel()
    }
}