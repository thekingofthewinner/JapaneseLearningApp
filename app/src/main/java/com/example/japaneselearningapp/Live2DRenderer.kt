
package com.example.japaneselearningapp

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Live2DRenderer(private val context: Context) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "Live2DRenderer"
    }

    init {
        Log.d(TAG, "Live2DRenderer 初始化")
        JniBridgeJava.setContext(context)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        Log.d(TAG, "onSurfaceCreated")
        try {
            JniBridgeJava.nativeOnSurfaceCreated()
            Log.d(TAG, "nativeOnSurfaceCreated 成功")
        } catch (e: Exception) {
            Log.e(TAG, "nativeOnSurfaceCreated 失败", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: $width x $height")
        try {
            JniBridgeJava.nativeOnSurfaceChanged(width, height)
            Log.d(TAG, "nativeOnSurfaceChanged 成功")
        } catch (e: Exception) {
            Log.e(TAG, "nativeOnSurfaceChanged 失败", e)
        }
    }

    override fun onDrawFrame(gl: GL10) {
        try {
            JniBridgeJava.nativeOnDrawFrame()
        } catch (e: Exception) {
            Log.e(TAG, "nativeOnDrawFrame 失败", e)
        }
    }
}
