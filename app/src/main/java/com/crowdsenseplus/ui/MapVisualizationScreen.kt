package com.crowdsenseplus.ui

import android.graphics.Color
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
// 高德地图相关导入 - 确保正确引用
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.UiSettings
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.Marker
import com.crowdsenseplus.model.SimulationModel
import android.util.Log
import android.widget.Toast
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 地图可视化界面，用于显示用户位置和任务分布
 */
@Composable
fun MapVisualizationScreen(
    userMovements: List<SimulationModel.UserMovementEvent>,
    tasks: List<SimulationModel.Task>,
    results: List<SimulationModel.SimulationResult>
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val TAG = "MapVisualization"
    
    // 错误信息状态
    var errorMessage by remember { mutableStateOf("") }
    // 初始化状态跟踪
    val isMapInitialized = remember { AtomicBoolean(false) }
    
    // 日志记录函数
    fun logMapError(operation: String, message: String, e: Exception) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()
        
        val errorMsg = "[$operation] $message: ${e.message}\n$stackTrace"
        Log.e(TAG, errorMsg)
        errorMessage = "错误: ${e.message}"
        
        // 将错误信息写入文件以便后续分析
        try {
            val logDir = File(context.getExternalFilesDir(null), "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val logFile = File(logDir, "map_error_$timestamp.txt")
            
            logFile.writeText(errorMsg)
            Log.i(TAG, "错误日志已保存到: ${logFile.absolutePath}")
        } catch (logEx: Exception) {
            Log.e(TAG, "无法保存错误日志: ${logEx.message}")
        }
        
        // 显示Toast提示
        Toast.makeText(context, "地图加载错误: ${e.message}", Toast.LENGTH_LONG).show()
    }
    
    // 计算地图中心点（所有点的平均位置）
    val centerLocation = remember {
        try {
            Log.d(TAG, "计算地图中心点")
            if (tasks.isNotEmpty()) {
                LatLng(tasks.map { it.latitude }.average(), tasks.map { it.longitude }.average())
            } else if (userMovements.isNotEmpty()) {
                LatLng(userMovements.map { it.latitude }.average(), userMovements.map { it.longitude }.average())
            } else {
                LatLng(42.8371, -1.6389) // 默认位置
            }
        } catch (e: Exception) {
            logMapError("初始化", "计算中心点错误", e)
            LatLng(42.8371, -1.6389) // 出错时使用默认位置
        }
    }
    
    // 使用remember确保地图组件在重组时保持状态
    var mapView: MapView? by remember { mutableStateOf(null) }
    var aMap: AMap? by remember { mutableStateOf(null) }
    // 添加错误状态跟踪
    var hasMapError by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 地图标题和说明
        Text(
            text = "模拟结果可视化",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        
        // 地图图例
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text("任务位置", modifier = Modifier.padding(start = 8.dp, end = 16.dp))
            
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.secondary)
            )
            Text("用户位置", modifier = Modifier.padding(start = 8.dp))
        }
        
        // 地图视图
        Box(modifier = Modifier.weight(1f)) {
            // 使用AndroidView集成高德地图
            AndroidView(
                factory = { ctx ->
                    Log.i(TAG, "开始创建地图视图")
                    // 创建MapView并确保正确初始化
                    val mapViewInstance = try {
                        MapView(ctx).apply {
                            // 确保在创建时不立即初始化，等待生命周期管理
                            Log.d(TAG, "MapView实例创建成功")
                        }
                    } catch (e: Exception) {
                        logMapError("创建地图", "创建MapView实例失败", e)
                        hasMapError = true
                        return@AndroidView MapView(ctx) // 返回一个空的MapView以避免崩溃
                    }
                    
                    // 保存MapView引用
                    mapView = mapViewInstance
                    
                    try {
                        Log.d(TAG, "初始化地图视图")
                        // 使用有效的Bundle进行初始化 - 确保Bundle不为空
                        val bundle = Bundle()
                        // 注意：onCreate会在DisposableEffect中处理，这里不调用
                        // 避免重复初始化导致的问题
                        
                        // 获取AMap对象并进行配置 - 延迟到生命周期事件中处理
                        // 这样可以确保地图组件已经完全准备好
                        
                        Log.d(TAG, "MapView创建完成，等待生命周期事件")
                    } catch (e: Exception) {
                        logMapError("初始化地图", "地图初始化过程中发生错误", e)
                        hasMapError = true
                    }
                    
                    // 返回MapView实例
                    mapViewInstance
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // 确保地图视图在更新时不会出现问题
                    try {
                        Log.d(TAG, "更新地图视图, 初始化状态: ${isMapInitialized.get()}, 错误状态: $hasMapError")
                        // 检查地图实例是否有效
                        if (aMap == null && mapView != null) {
                            try {
                                // 尝试重新获取地图实例
                                aMap = mapView?.map
                                Log.d(TAG, "尝试重新获取地图实例: ${aMap != null}")
                            } catch (e: Exception) {
                                Log.e(TAG, "重新获取地图实例失败: ${e.message}")
                            }
                        }
                        
                        // 只有在地图初始化成功且没有错误时才添加标记
                        if (!hasMapError && aMap != null && tasks.isNotEmpty() && isMapInitialized.get()) {
                            Log.d(TAG, "开始添加地图标记, 任务数量: ${tasks.size}")
                            // 清除之前的标记，避免重复添加
                            try {
                                aMap?.clear()
                                Log.d(TAG, "清除之前的地图标记")
                            } catch (e: Exception) {
                                logMapError("更新地图", "清除地图标记失败", e)
                            }
                            
                            // 添加任务标记和圆圈
                            tasks.forEach { task ->
                                try {
                                    val taskPosition = LatLng(task.latitude, task.longitude)
                                    val candidateCount = results.find { it.taskId == task.taskId }?.candidates ?: 0
                                    
                                    // 添加标记 - 使用安全调用和错误处理
                                    val markerOptions = MarkerOptions()
                                        .position(taskPosition)
                                        .title("任务 ${task.taskId}")
                                        .snippet("候选用户: $candidateCount")
                                    try {
                                        val marker: Marker? = aMap?.addMarker(markerOptions)
                                        marker?.showInfoWindow()
                                    } catch (e: Exception) {
                                        Log.e("MapVisualization", "添加标记失败: ${e.message}")
                                    }
                                    
                                    // 添加圆圈表示任务执行范围 - 使用安全调用
                                    try {
                                        val circleOptions = CircleOptions()
                                            .center(taskPosition)
                                            .radius(task.distance.toDouble())
                                            .fillColor(Color.argb(51, 33, 150, 243)) // 半透明蓝色
                                            .strokeColor(Color.BLUE)
                                            .strokeWidth(2f)
                                        aMap?.addCircle(circleOptions)
                                    } catch (e: Exception) {
                                        Log.e("MapVisualization", "添加圆圈失败: ${e.message}")
                                    }
                                } catch (e: Exception) {
                                    Log.e("MapVisualization", "处理任务标记失败: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                            
                            // 添加用户位置标记
                            if (userMovements.isNotEmpty()) {
                                val latestUserPositions = userMovements
                                    .groupBy { it.userId }
                                    .mapValues { it.value.maxByOrNull { user -> user.timestamp } }
                                    .values
                                    .filterNotNull()
                                
                                latestUserPositions.forEach { user ->
                                    try {
                                        val userPosition = LatLng(user.latitude, user.longitude)
                                        val markerOptions = MarkerOptions()
                                            .position(userPosition)
                                            .title("用户 ${user.userId}")
                                            .snippet("时间: ${user.hour}:${user.minute}")
                                        
                                        // 安全地设置图标
                                        try {
                                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                                .alpha(0.7f)
                                        } catch (e: Exception) {
                                            Log.e("MapVisualization", "设置用户标记图标失败: ${e.message}")
                                            // 使用默认图标
                                        }
                                        
                                        // 安全地添加标记
                                        try {
                                            aMap?.addMarker(markerOptions)
                                        } catch (e: Exception) {
                                            Log.e("MapVisualization", "添加用户标记失败: ${e.message}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MapVisualization", "处理用户位置标记失败: ${e.message}")
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        hasMapError = true
                    }
                }
            )
            
            // 地图控制按钮 - 添加安全检查防止空指针异常
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { 
                        try {
                            if (aMap != null && !hasMapError) {
                                val currentSetting = aMap?.uiSettings?.isZoomControlsEnabled ?: true
                                aMap?.uiSettings?.isZoomControlsEnabled = !currentSetting
                            }
                        } catch (e: Exception) {
                            Log.e("MapVisualization", "切换缩放控件失败: ${e.message}")
                            e.printStackTrace()
                            hasMapError = true
                        }
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("缩放")
                }
                
                FloatingActionButton(
                    onClick = { 
                        try {
                            if (aMap != null && !hasMapError) {
                                aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(centerLocation, 13f))
                            }
                        } catch (e: Exception) {
                            Log.e("MapVisualization", "重置地图视图失败: ${e.message}")
                            e.printStackTrace()
                            hasMapError = true
                        }
                    }
                ) {
                    Text("重置")
                }
            }
        }
    }
    
    // 错误提示 - 改进错误提示的显示方式，使其覆盖在地图上而不是替换地图
    if (hasMapError) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "地图加载失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (errorMessage.isNotEmpty()) errorMessage else "请检查网络连接或重试",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            try {
                                Log.i(TAG, "用户点击重试按钮")
                                hasMapError = false
                                errorMessage = ""
                                isMapInitialized.set(false)
                                
                                // 尝试重新初始化地图
                                try {
                                    // 先销毁再重建
                                    mapView?.onDestroy()
                                    
                                    // 重新创建Bundle并初始化
                                    val bundle = Bundle()
                                    mapView?.onCreate(bundle)
                                    mapView?.onResume()
                                    
                                    // 重新获取AMap实例
                                    val aMapInstance = mapView?.map
                                    if (aMapInstance != null) {
                                        aMap = aMapInstance
                                        // 重新设置UI和相机位置
                                        aMapInstance.uiSettings.isZoomControlsEnabled = true
                                        aMapInstance.uiSettings.isCompassEnabled = true
                                        aMapInstance.uiSettings.isScaleControlsEnabled = true
                                        aMapInstance.moveCamera(CameraUpdateFactory.newLatLngZoom(centerLocation, 13f))
                                        
                                        // 设置地图加载完成监听器
                                        aMapInstance.setOnMapLoadedListener {
                                            Log.i(TAG, "地图重新加载完成")
                                            isMapInitialized.set(true)
                                        }
                                        
                                        Log.i(TAG, "地图重新初始化成功")
                                        Toast.makeText(context, "地图重新加载中...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        throw IllegalStateException("重新初始化地图失败: 无法获取AMap实例")
                                    }
                                } catch (e: Exception) {
                                    logMapError("重试", "重试初始化地图失败", e)
                                    hasMapError = true
                                }
                            } catch (e: Exception) {
                                logMapError("重试", "重试按钮点击处理失败", e)
                                hasMapError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("重试")
                    }
                }
            }
        }
    }
    
    // 生命周期管理 - 优化以防止内存泄漏和崩溃
    DisposableEffect(lifecycleOwner) {
        // 保存Bundle状态
        val mapViewBundle = Bundle()
        
        val observer = LifecycleEventObserver { _, event ->
            Log.d(TAG, "生命周期事件: $event, 错误状态: $hasMapError")
            
            try {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> {
                        try {
                            // 确保MapView已经创建并初始化
                            Log.d(TAG, "生命周期 ON_CREATE: 初始化MapView")
                            mapView?.onCreate(mapViewBundle)
                            
                            // 获取AMap对象并进行配置 - 在onCreate后初始化
                            val aMapInstance = mapView?.map
                            if (aMapInstance == null) {
                                val error = IllegalStateException("无法初始化地图: map为null")
                                logMapError("初始化地图", "获取AMap实例失败", error)
                                hasMapError = true
                                return@LifecycleEventObserver
                            }
                            
                            Log.d(TAG, "成功获取AMap实例")
                            aMap = aMapInstance
                            
                            // 设置地图UI控制 - 简化UI设置减少资源消耗
                            try {
                                val uiSettings = aMapInstance.uiSettings
                                uiSettings.isZoomControlsEnabled = true
                                uiSettings.isCompassEnabled = true
                                uiSettings.isScaleControlsEnabled = true
                                Log.d(TAG, "地图UI设置完成")
                            } catch (e: Exception) {
                                logMapError("初始化地图", "设置UI控件失败", e)
                            }
                        } catch (e: Exception) {
                            logMapError("生命周期", "ON_CREATE处理失败", e)
                            hasMapError = true
                        }
                    }
                    Lifecycle.Event.ON_START -> {
                        // MapView没有onStart方法
                        Log.d(TAG, "生命周期 ON_START: MapView没有对应方法")
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        try {
                            Log.d(TAG, "生命周期 ON_RESUME: 恢复MapView")
                            mapView?.onResume()
                            
                            // 移动相机到中心位置 - 在onResume中执行
                            try {
                                if (aMap != null && !hasMapError) {
                                    aMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(centerLocation, 13f))
                                    Log.d(TAG, "相机移动到中心位置: $centerLocation")
                                    
                                    // 设置地图加载完成监听器
                                    aMap?.setOnMapLoadedListener {
                                        Log.i(TAG, "地图加载完成")
                                        isMapInitialized.set(true)
                                    }
                                }
                            } catch (e: Exception) {
                                logMapError("初始化地图", "移动相机失败", e)
                            }
                        } catch (e: Exception) {
                            logMapError("生命周期", "ON_RESUME处理失败", e)
                            hasMapError = true
                        }
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        try {
                            Log.d(TAG, "生命周期 ON_PAUSE: 暂停MapView")
                            mapView?.onPause()
                        } catch (e: Exception) {
                            logMapError("生命周期", "ON_PAUSE处理失败", e)
                        }
                    }
                    Lifecycle.Event.ON_STOP -> {
                        // MapView没有onStop方法
                        Log.d(TAG, "生命周期 ON_STOP: MapView没有对应方法")
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        // 安全地清理资源
                        Log.d(TAG, "生命周期 ON_DESTROY: 销毁MapView资源")
                        try {
                            // 确保在销毁前清除所有标记和图形
                            try {
                                aMap?.clear()
                                Log.d(TAG, "清除地图标记和图形")
                            } catch (e: Exception) {
                                Log.e(TAG, "清除地图标记失败: ${e.message}")
                            }
                            
                            // 销毁MapView
                            try {
                                mapView?.onDestroy()
                                Log.d(TAG, "MapView销毁完成")
                            } catch (e: Exception) {
                                Log.e(TAG, "MapView销毁失败: ${e.message}")
                            }
                        } catch (e: Exception) {
                            logMapError("生命周期", "ON_DESTROY处理失败", e)
                        } finally {
                            // 无论如何都确保释放资源引用
                            aMap = null
                            mapView = null
                            Log.d(TAG, "地图资源引用已清空")
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                logMapError("生命周期", "生命周期事件处理错误", e)
                hasMapError = true
            }
        }
        
        try {
            Log.d(TAG, "添加生命周期观察者")
            lifecycleOwner.lifecycle.addObserver(observer)
        } catch (e: Exception) {
            logMapError("生命周期", "添加生命周期观察者失败", e)
        }
        
        onDispose {
            Log.d(TAG, "组件销毁, 开始清理资源")
            try {
                // 移除生命周期观察者，避免重复调用生命周期方法
                try {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    Log.d(TAG, "移除生命周期观察者")
                } catch (e: Exception) {
                    Log.e(TAG, "移除生命周期观察者失败: ${e.message}")
                }
                
                // 确保在组件销毁时清除所有标记和图形
                try {
                    aMap?.clear()
                    Log.d(TAG, "清除地图标记和图形")
                } catch (e: Exception) {
                    Log.e(TAG, "清除地图标记失败: ${e.message}")
                }
                
                // 确保在组件销毁时释放地图资源
                try {
                    mapView?.onPause()
                    Log.d(TAG, "暂停MapView")
                } catch (e: Exception) {
                    Log.e(TAG, "暂停MapView失败: ${e.message}")
                }
                
                // 销毁MapView
                try {
                    mapView?.onDestroy()
                    Log.d(TAG, "销毁MapView")
                } catch (e: Exception) {
                    Log.e(TAG, "销毁MapView失败: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "组件销毁过程中发生错误: ${e.message}")
                e.printStackTrace()
            } finally {
                // 无论如何都确保释放资源引用
                aMap = null
                mapView = null
            }
        }
    }
}