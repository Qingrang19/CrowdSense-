package com.crowdsenseplus.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 地图组件崩溃处理器
 * 用于捕获地图相关操作导致的崩溃，并记录详细信息以便后续分析
 */
class MapCrashHandler private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val TAG = "MapCrashHandler"
    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    
    companion object {
        private var instance: MapCrashHandler? = null
        
        @Synchronized
        fun initialize(context: Context) {
            if (instance == null) {
                instance = MapCrashHandler(context.applicationContext)
            }
            // 设置为默认的未捕获异常处理器
            Thread.setDefaultUncaughtExceptionHandler(instance)
        }
    }
    
    /**
     * 处理未捕获的异常
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 记录崩溃信息
        val crashInfo = collectCrashInfo(thread, throwable)
        saveCrashInfo(crashInfo)
        
        // 如果有默认的异常处理器，交给系统处理
        defaultHandler?.uncaughtException(thread, throwable)
    }
    
    /**
     * 收集崩溃信息
     */
    private fun collectCrashInfo(thread: Thread, throwable: Throwable): String {
        val sb = StringBuilder()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        sb.append("===== 应用崩溃报告 =====\n")
        sb.append("时间: $timestamp\n")
        sb.append("线程: ${thread.name} (ID: ${thread.id})\n\n")
        
        // 设备信息
        sb.append("设备信息:\n")
        sb.append("- 制造商: ${android.os.Build.MANUFACTURER}\n")
        sb.append("- 型号: ${android.os.Build.MODEL}\n")
        sb.append("- Android版本: ${android.os.Build.VERSION.RELEASE}\n")
        sb.append("- SDK版本: ${android.os.Build.VERSION.SDK_INT}\n\n")
        
        // 异常堆栈
        sb.append("异常信息:\n")
        sb.append("- 类型: ${throwable.javaClass.name}\n")
        sb.append("- 消息: ${throwable.message}\n\n")
        
        // 完整堆栈跟踪
        sb.append("堆栈跟踪:\n")
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        sb.append(sw.toString())
        
        return sb.toString()
    }
    
    /**
     * 保存崩溃信息到文件
     */
    private fun saveCrashInfo(crashInfo: String) {
        try {
            val crashDir = File(context.getExternalFilesDir(null), "map_crashes")
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val crashFile = File(crashDir, "crash_$timestamp.txt")
            
            crashFile.writeText(crashInfo)
            Log.e(TAG, "崩溃信息已保存到: ${crashFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "保存崩溃信息失败: ${e.message}")
        }
    }
    
    /**
     * 获取所有崩溃日志文件
     */
    fun getCrashLogs(): List<File> {
        val crashDir = File(context.getExternalFilesDir(null), "map_crashes")
        return if (crashDir.exists()) {
            crashDir.listFiles()?.filter { it.isFile && it.name.startsWith("crash_") }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * 获取最新的崩溃日志内容
     */
    fun getLatestCrashLog(): String? {
        val logs = getCrashLogs()
        return if (logs.isNotEmpty()) {
            val latestLog = logs.maxByOrNull { it.lastModified() }
            latestLog?.readText()
        } else {
            null
        }
    }
    
    /**
     * 清除所有崩溃日志
     */
    fun clearCrashLogs(): Boolean {
        val crashDir = File(context.getExternalFilesDir(null), "map_crashes")
        return if (crashDir.exists()) {
            crashDir.listFiles()?.forEach { it.delete() }
            true
        } else {
            false
        }
    }
}