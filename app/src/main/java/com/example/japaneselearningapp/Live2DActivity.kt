package com.example.japaneselearningapp

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent

class Live2DActivity : Activity() {
    private lateinit var glSurfaceView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live2d)

        glSurfaceView = findViewById(R.id.live2d_view)
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(Live2DRenderer(this))
        glSurfaceView.setOnTouchListener { _, event ->
            handleTouchEvent(event)
            true
        }

        JniBridgeJava.setActivityInstance(this)
    }

    private fun handleTouchEvent(event: MotionEvent) {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                JniBridgeJava.nativeOnTouchesBegan(x, y)
            }
            MotionEvent.ACTION_UP -> {
                JniBridgeJava.nativeOnTouchesEnded(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                JniBridgeJava.nativeOnTouchesMoved(x, y)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        JniBridgeJava.nativeOnStart()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
        JniBridgeJava.nativeOnPause()
    }

    override fun onStop() {
        super.onStop()
        JniBridgeJava.nativeOnStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        JniBridgeJava.nativeOnDestroy()
    }
}