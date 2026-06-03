
package com.example.japaneselearningapp

import android.content.Context
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Live2DRenderer(private val context: Context) : GLSurfaceView.Renderer {

    init {
        JniBridgeJava.setContext(context)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        JniBridgeJava.nativeOnSurfaceCreated()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        JniBridgeJava.nativeOnSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        JniBridgeJava.nativeOnDrawFrame()
    }
}
