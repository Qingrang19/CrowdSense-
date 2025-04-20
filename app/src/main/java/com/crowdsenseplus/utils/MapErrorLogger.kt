package com.crowdsenseplus.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 地图错误日志记录工具类
 * 用于捕获和记录地图相关操作中的异常，便于后续分析和调试
 */
object MapErrorLogger {
    private const val TAG = "MapErrorLogger"
    
    /**
     * 记录地图操作中的错误
     * @param context 上下文
     * @param operation 操作名称
     * @param message 错误信息
     * @param exception 异常对象
     * @return 格式化后的错误信息
     */
    fun logError(context: Context, operation: String, message: String, exception: Exception): String {
        // 获取完整的堆栈跟踪
        val sw = StringWriter()
        exception.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()
        
        // 格式化错误信息
        val errorMsg = "[$operation] $message: ${exception.message}\n$stackTrace"
        
        // 记录到Logcat
        Log.e(TAG, errorMsg)
        
        // 保存到文件
        saveErrorToFile(context, errorMsg)
        
        // 显示Toast提示
        Toast.makeText(context, "地图错误: ${exception.message}", Toast.LENGTH_LONG).show()
        
        // 返回简短的错误信息用于UI显示
        return "错误: ${exception.message}"
    }
    
    /**
     * 将错误信息保存到文件
     * @param context 上下文
     * @param errorMsg 错误信息
     */
    private fun saveErrorToFile(context: Context, errorMsg: String) {
        try {
            // 创建日志目录
            val logDir = File(context.getExternalFilesDir(null), "map_error_logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            // 创建带时间戳的日志文件
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val logFile = File(logDir, "map_error_$timestamp.txt")
            
            // 写入错误信息
            logFile.writeText(errorMsg)
            
            Log.i(TAG, "错误日志已保存到: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "保存错误日志失败: ${e.message}")
        }
    }
    
    /**
     * 获取所有保存的错误日志文件
     * @param context 上下文
     * @return 日志文件列表
     */
    fun getErrorLogs(context: Context): List<File> {
        val logDir = File(context.getExternalFilesDir(null), "map_error_logs")
        return if (logDir.exists()) {
            logDir.listFiles()?.filter { it.isFile && it.name.startsWith("map_error_") }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * 读取最新的错误日志内容
     * @param context 上下文
     * @return 最新的错误日志内容，如果没有则返回null
     */
    fun getLatestErrorLog(context: Context): String? {
        val logs = getErrorLogs(context)
        return if (logs.isNotEmpty()) {
            // 按文件修改时间排序，获取最新的日志
            val latestLog = logs.maxByOrNull { it.lastModified() }
            latestLog?.readText()
        } else {
            null
        }
    }
    
    /**
     * 清除所有错误日志
     * @param context 上下文
     * @return 是否成功清除
     */
    fun clearErrorLogs(context: Context): Boolean {
        val logDir = File(context.getExternalFilesDir(null), "map_error_logs")
        return if (logDir.exists()) {
            logDir.listFiles()?.forEach { it.delete() }
            true
        } else {
            false
        }
    }
}