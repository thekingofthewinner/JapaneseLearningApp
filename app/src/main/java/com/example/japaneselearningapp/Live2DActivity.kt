package com.example.japaneselearningapp

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent

class Live2DActivity : Activity() {
    private lateinit var glSurfaceView: GLSurfaceView

    companion object {
        private const val TAG = "Live2DActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate - 开始初始化")
        
        try {
            setContentView(R.layout.activity_live2d)
            Log.d(TAG, "setContentView 成功")

            glSurfaceView = findViewById(R.id.live2d_view)
            Log.d(TAG, "findViewById 成功")

            glSurfaceView.setEGLContextClientVersion(2)
            Log.d(TAG, "setEGLContextClientVersion 成功")

            glSurfaceView.setRenderer(Live2DRenderer(this))
            Log.d(TAG, "setRenderer 成功")

            glSurfaceView.setOnTouchListener { _, event ->
                handleTouchEvent(event)
                true
            }

            JniBridgeJava.setContext(this)
            Log.d(TAG, "setContext 成功")
            JniBridgeJava.setActivityInstance(this)
            Log.d(TAG, "setActivityInstance 成功")
            
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
            throw e
        }
        
        Log.d(TAG, "onCreate - 初始化完成")
    }

    private fun handleTouchEvent(event: MotionEvent) {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.d(TAG, "触摸开始 ($x, $y)")
                JniBridgeJava.nativeOnTouchesBegan(x, y)
            }
            MotionEvent.ACTION_UP -> {
                Log.d(TAG, "触摸结束 ($x, $y)")
                JniBridgeJava.nativeOnTouchesEnded(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                Log.d(TAG, "触摸移动 ($x, $y)")
                JniBridgeJava.nativeOnTouchesMoved(x, y)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        JniBridgeJava.nativeOnStart()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        glSurfaceView.onPause()
        JniBridgeJava.nativeOnPause()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
        JniBridgeJava.nativeOnStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        JniBridgeJava.nativeOnDestroy()
    }
}