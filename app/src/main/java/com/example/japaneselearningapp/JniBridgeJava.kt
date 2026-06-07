
package com.example.japaneselearningapp

import android.app.Activity
import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.InputStream

object JniBridgeJava {

    private const val TAG = "JniBridgeJava"

    init {
        try {
            Log.d(TAG, "开始加载 native-lib")
            System.loadLibrary("native-lib")
            Log.d(TAG, "native-lib 加载成功")
        } catch (e: Exception) {
            Log.e(TAG, "native-lib 加载失败", e)
            throw e
        }
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
        Log.d(TAG, "setContext: $context")
        this.context = context
    }

    fun setActivityInstance(activity: Activity) {
        Log.d(TAG, "setActivityInstance: $activity")
        this.activityInstance = activity
    }

    @JvmStatic
    fun GetAssetList(dirPath: String): Array<String> {
        return try {
            Log.d(TAG, "GetAssetList($dirPath)")
            val list = context?.assets?.list(dirPath) ?: emptyArray()
            Log.d(TAG, "GetAssetList 返回 ${list.size} 个元素: ${list.joinToString()}")
            list
        } catch (e: IOException) {
            Log.e(TAG, "GetAssetList 失败", e)
            e.printStackTrace()
            emptyArray()
        }
    }

    @JvmStatic
    fun LoadFile(filePath: String): ByteArray? {
        Log.d(TAG, "LoadFile($filePath)")
        var fileData: InputStream? = null
        return try {
            fileData = context?.assets?.open(filePath)
            val fileSize = fileData?.available() ?: 0
            Log.d(TAG, "文件大小: $fileSize")
            val fileBuffer = ByteArray(fileSize)
            val readSize = fileData?.read(fileBuffer, 0, fileSize) ?: 0
            Log.d(TAG, "读取大小: $readSize")
            fileBuffer
        } catch (e: IOException) {
            Log.e(TAG, "LoadFile 失败: $filePath", e)
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

    @JvmStatic
    fun MoveTaskToBack() {
        Log.d(TAG, "MoveTaskToBack")
        activityInstance?.moveTaskToBack(true)
    }
}

