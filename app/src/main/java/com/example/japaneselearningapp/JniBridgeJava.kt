
package com.example.japaneselearningapp

import android.app.Activity
import android.content.Context
import java.io.IOException
import java.io.InputStream

object JniBridgeJava {

    init {
        System.loadLibrary("native-lib")
    }

    external fun nativeOnStart()
    external fun nativeOnPause()
    external fun nativeOnStop()
    external fun nativeOnDestroy()
    external fun nativeOnSurfaceCreated()
    external fun nativeOnSurfaceChanged(width: Int, height: Int)
    external fun nativeOnDrawFrame()
    external fun nativeOnTouchesBegan(pointX: Float, pointY: Float)
    external fun nativeOnTouchesEnded(pointX: Float, pointY: Float)
    external fun nativeOnTouchesMoved(pointX: Float, pointY: Float)

    private var context: Context? = null
    private var activityInstance: Activity? = null

    fun setContext(context: Context) {
        this.context = context
    }

    fun setActivityInstance(activity: Activity) {
        this.activityInstance = activity
    }

    fun getAssetList(dirPath: String): Array<String> {
        return try {
            context?.assets?.list(dirPath) ?: emptyArray()
        } catch (e: IOException) {
            e.printStackTrace()
            emptyArray()
        }
    }

    fun loadFile(filePath: String): ByteArray? {
        var fileData: InputStream? = null
        return try {
            fileData = context?.assets?.open(filePath)
            val fileSize = fileData?.available() ?: 0
            val fileBuffer = ByteArray(fileSize)
            fileData?.read(fileBuffer, 0, fileSize)
            fileBuffer
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            try {
                fileData?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun moveTaskToBack() {
        activityInstance?.moveTaskToBack(true)
    }
}

