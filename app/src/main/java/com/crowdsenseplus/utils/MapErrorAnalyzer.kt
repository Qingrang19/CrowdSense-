package com.crowdsenseplus.utils

import android.content.Context
import android.util.Log
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import java.io.File

/**
 * 地图错误分析工具
 * 用于分析和诊断地图相关的错误，帮助开发者快速定位问题
 */
object MapErrorAnalyzer {
    private const val TAG = "MapErrorAnalyzer"
    
    /**
     * 分析地图状态并返回诊断结果
     * @param context 上下文
     * @param mapView 地图视图实例
     * @param aMap 地图控制器实例
     * @return 诊断结果
     */
    fun analyzeMapState(context: Context, mapView: MapView?, aMap: AMap?): String {
        val diagnosticBuilder = StringBuilder()
        diagnosticBuilder.append("地图状态诊断报告:\n")
        
        // 检查MapView实例
        if (mapView == null) {
            diagnosticBuilder.append("- 错误: MapView实例为空\n")
        } else {
            diagnosticBuilder.append("- MapView实例正常\n")
        }
        
        // 检查AMap实例
        if (aMap == null) {
            diagnosticBuilder.append("- 错误: AMap实例为空\n")
        } else {
            diagnosticBuilder.append("- AMap实例正常\n")
        }
        
        // 检查高德地图SDK文件
        val sdkFiles = checkMapSdkFiles(context)
        diagnosticBuilder.append(sdkFiles)
        
        // 检查最近的错误日志
        val latestError = MapErrorLogger.getLatestErrorLog(context)
        if (latestError != null) {
            diagnosticBuilder.append("\n最近的错误记录:\n")
            // 只取错误日志的前500个字符，避免过长
            val truncatedError = if (latestError.length > 500) {
                latestError.substring(0, 500) + "...\n(错误日志过长，已截断)"
            } else {
                latestError
            }
            diagnosticBuilder.append(truncatedError)
        } else {
            diagnosticBuilder.append("\n没有找到最近的错误记录")
        }
        
        return diagnosticBuilder.toString()
    }
    
    /**
     * 检查高德地图SDK文件是否存在
     * @param context 上下文
     * @return 检查结果
     */
    private fun checkMapSdkFiles(context: Context): String {
        val result = StringBuilder()
        result.append("\n高德地图SDK文件检查:\n")
        
        // 检查libs目录
        val libsDir = File(context.applicationInfo.nativeLibraryDir)
        if (libsDir.exists()) {
            val soFiles = libsDir.listFiles { file -> file.name.endsWith(".so") }
            if (soFiles != null && soFiles.isNotEmpty()) {
                val mapSoFiles = soFiles.filter { it.name.contains("amap") || it.name.contains("3dmap") }
                if (mapSoFiles.isNotEmpty()) {
                    result.append("- 找到高德地图相关.so文件: ${mapSoFiles.size}个\n")
                    mapSoFiles.forEach { 
                        result.append("  - ${it.name} (${it.length() / 1024}KB)\n")
                    }
                } else {
                    result.append("- 警告: 未找到高德地图相关.so文件\n")
                }
            } else {
                result.append("- 警告: libs目录为空或不包含.so文件\n")
            }
        } else {
            result.append("- 错误: 找不到libs目录\n")
        }
        
        return result.toString()
    }
    
    /**
     * 生成完整的诊断报告
     * @param context 上下文
     * @return 诊断报告文件路径
     */
    fun generateDiagnosticReport(context: Context): String {
        val report = StringBuilder()
        report.append("===== 地图组件诊断报告 =====\n")
        report.append("时间: ${java.util.Date()}\n\n")
        
        // 设备信息
        report.append("设备信息:\n")
        report.append("- 制造商: ${android.os.Build.MANUFACTURER}\n")
        report.append("- 型号: ${android.os.Build.MODEL}\n")
        report.append("- Android版本: ${android.os.Build.VERSION.RELEASE}\n")
        report.append("- SDK版本: ${android.os.Build.VERSION.SDK_INT}\n\n")
        
        // 应用信息
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            report.append("应用信息:\n")
            report.append("- 包名: ${context.packageName}\n")
            report.append("- 版本名: ${packageInfo.versionName}\n")
            report.append("- 版本号: ${packageInfo.versionCode}\n\n")
        } catch (e: Exception) {
            report.append("无法获取应用信息: ${e.message}\n\n")
        }
        
        // 错误日志汇总
        val errorLogs = MapErrorLogger.getErrorLogs(context)
        report.append("错误日志汇总:\n")
        if (errorLogs.isNotEmpty()) {
            report.append("- 共有 ${errorLogs.size} 个错误日志\n")
            errorLogs.sortedByDescending { it.lastModified() }.take(5).forEach { file ->
                report.append("- ${file.name} (${java.util.Date(file.lastModified())})\n")
            }
        } else {
            report.append("- 没有找到错误日志\n")
        }
        
        // 保存报告到文件
        try {
            val reportDir = File(context.getExternalFilesDir(null), "map_reports")
            if (!reportDir.exists()) {
                reportDir.mkdirs()
            }
            
            val reportFile = File(reportDir, "map_diagnostic_${System.currentTimeMillis()}.txt")
            reportFile.writeText(report.toString())
            
            Log.i(TAG, "诊断报告已保存到: ${reportFile.absolutePath}")
            return reportFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "保存诊断报告失败: ${e.message}")
            return "保存诊断报告失败: ${e.message}"
        }
    }
}