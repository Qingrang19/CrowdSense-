package com.crowdsenseplus.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crowdsenseplus.model.SimulationModel
import kotlin.math.min

/**
 * 静态地图可视化界面，使用Canvas绘制地图、任务和用户位置
 * 这是一个替代高德地图的解决方案，避免SDK集成问题导致的应用崩溃
 */
@Composable
fun StaticMapVisualizationScreen(
    userMovements: List<SimulationModel.UserMovementEvent>,
    tasks: List<SimulationModel.Task>,
    results: List<SimulationModel.SimulationResult>
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // 状态变量
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    
    // 计算地图边界
    val mapBounds = remember(tasks, userMovements) {
        if (tasks.isEmpty() && userMovements.isEmpty()) {
            // 默认边界（西班牙潘普洛纳市附近）
            MapBounds(
                minLat = 42.8 - 0.05,
                maxLat = 42.8 + 0.05,
                minLon = -1.64 - 0.05,
                maxLon = -1.64 + 0.05
            )
        } else {
            // 从数据计算边界
            var minLat = Double.MAX_VALUE
            var maxLat = Double.MIN_VALUE
            var minLon = Double.MAX_VALUE
            var maxLon = Double.MIN_VALUE
            
            // 考虑任务位置
            tasks.forEach { task ->
                minLat = minOf(minLat, task.latitude)
                maxLat = maxOf(maxLat, task.latitude)
                minLon = minOf(minLon, task.longitude)
                maxLon = maxOf(maxLon, task.longitude)
            }
            
            // 考虑用户位置
            userMovements.forEach { user ->
                minLat = minOf(minLat, user.latitude)
                maxLat = maxOf(maxLat, user.latitude)
                minLon = minOf(minLon, user.longitude)
                maxLon = maxOf(maxLon, user.longitude)
            }
            
            // 添加边距
            val latPadding = (maxLat - minLat) * 0.1
            val lonPadding = (maxLon - minLon) * 0.1
            
            MapBounds(
                minLat = minLat - latPadding,
                maxLat = maxLat + latPadding,
                minLon = minLon - lonPadding,
                maxLon = maxLon + lonPadding
            )
        }
    }
    
    // 获取最新的用户位置（每个用户只显示最新位置）
    val latestUserPositions = remember(userMovements) {
        userMovements
            .groupBy { it.userId }
            .mapValues { it.value.maxByOrNull { user -> user.timestamp } }
            .values
            .filterNotNull()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 标题和图例
        Text(
            text = "模拟结果可视化（静态地图）",
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
        
        // 缩放说明
        Text(
            text = "提示：使用双指缩放和平移地图",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        // 地图区域
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(Color(0xFFF8F8F8)) // 更亮的背景色提高可读性
        ) {
            // 地图画布
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // 更新缩放和平移
                            scale *= zoom
                            offset += pan
                            
                            // 限制缩放范围
                            scale = scale.coerceIn(0.5f, 5f)
                        }
                    }
            ) {
                // 应用缩放
                scale(scale, scale, center) {
                    // 手动应用平移变换
                    val translateX = offset.x
                    val translateY = offset.y
                    
                    // 绘制网格和坐标
                    drawGrid(mapBounds)
                    
                    // 绘制任务，手动应用偏移
                    tasks.forEach { task ->
                        // 计算位置并应用偏移
                        val basePosition = mapBounds.toCanvasPosition(
                            task.latitude,
                            task.longitude,
                            size
                        )
                        val taskPosition = Offset(basePosition.x + translateX, basePosition.y + translateY)
                        
                        // 绘制任务执行范围（圆圈）
                        val radiusInMeters = task.distance.toDouble()
                        val radiusInPixels = mapBounds.metersToPixels(
                            radiusInMeters,
                            task.latitude,
                            size
                        )
                        
                        // 绘制圆圈表示任务范围
                        drawCircle(
                            color = Color(0x330D47A1), // 半透明蓝色
                            center = taskPosition,
                            radius = radiusInPixels,
                            style = Stroke(width = 2f)
                        )
                        
                        // 绘制任务标记点
                        val isSelected = task.taskId == selectedTaskId
                        val markerSize = if (isSelected) 16f else 10f
                        val markerColor = Color(0xFF6200EE) // 使用固定的紫色
                            
                        // 绘制带背景的任务标记点，提高可见度
                        drawCircle(
                            color = Color.White,
                            center = taskPosition,
                            radius = (markerSize / 2) + 2f
                        )
                        drawCircle(
                            color = markerColor,
                            center = taskPosition,
                            radius = markerSize / 2
                        )
                            
                        // 只在选中或悬停时显示详细信息
                        if (isSelected) {
                            val candidateCount = results.find { it.taskId == task.taskId }?.candidates ?: 0
                            val taskText = "任务 ${task.taskId}"
                                
                            // 绘制文本背景
                            val textBgPaint = Paint().apply {
                                color = Color.White.toArgb()
                                alpha = 230
                            }
                            
                            val textWidth = 120f
                            val textHeight = 40f
                            
                            drawContext.canvas.nativeCanvas.drawRect(
                                taskPosition.x - textWidth/2,
                                taskPosition.y - markerSize - textHeight - 10f,
                                taskPosition.x + textWidth/2,
                                taskPosition.y - markerSize - 5f,
                                textBgPaint
                            )
                                
                            // 使用原生Canvas绘制文本
                            val paint = Paint().apply {
                                color = Color.Black.toArgb()
                                textSize = 30f
                                typeface = Typeface.DEFAULT_BOLD
                                textAlign = Paint.Align.CENTER
                            }
                                
                            drawContext.canvas.nativeCanvas.drawText(
                                taskText,
                                taskPosition.x,
                                taskPosition.y - markerSize - 20f,
                                paint
                            )
                        } else {
                            // 非选中状态只显示任务ID
                            val paint = Paint().apply {
                                color = Color.Black.toArgb()
                                textSize = 24f
                                textAlign = Paint.Align.CENTER
                            }
                            
                            drawContext.canvas.nativeCanvas.drawText(
                                "${task.taskId}",
                                taskPosition.x,
                                taskPosition.y - markerSize - 5f,
                                paint
                            )
                        }
                    }
                    
                    // 绘制用户位置
                    latestUserPositions.forEach { user ->
                        // 计算位置并应用偏移
                        val basePosition = mapBounds.toCanvasPosition(
                            user.latitude,
                            user.longitude,
                            size
                        )
                        val userPosition = Offset(basePosition.x + translateX, basePosition.y + translateY)
                        
                        // 绘制用户标记 - 使用白色边框提高可见度
                        drawCircle(
                            color = Color.White,
                            center = userPosition,
                            radius = 6f,
                            alpha = 0.9f
                        )
                        drawCircle(
                            color = Color(0xFF03DAC5), // 使用固定的青色
                            center = userPosition,
                            radius = 4f,
                            alpha = 0.8f
                        )
                        
                        // 简化用户标签，只显示ID
                        val paint = Paint().apply {
                            color = Color.Black.toArgb()
                            textSize = 24f
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.DEFAULT
                        }
                        
                        // 绘制简短的用户ID
                        drawContext.canvas.nativeCanvas.drawText(
                            "${user.userId}",
                            userPosition.x,
                            userPosition.y - 10f,
                            paint
                        )
                    }
                }
            }
            
            // 控制按钮和任务选择器
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { scale *= 1.2f },
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Text("+")
                }
                
                FloatingActionButton(
                    onClick = { scale /= 1.2f },
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Text("-")
                }
                
                FloatingActionButton(
                    onClick = { 
                        scale = 1f
                        offset = Offset.Zero
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Text("重置")
                }
            }
            
            // 添加任务选择器 - 左侧面板
            if (tasks.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .background(Color.White.copy(alpha = 0.8f), shape = MaterialTheme.shapes.small)
                        .padding(8.dp)
                ) {
                    Text(
                        "选择任务:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    // 显示前5个任务的快速选择按钮
                    tasks.take(5).forEach { task ->
                        val isSelected = task.taskId == selectedTaskId
                        val buttonColor = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                            
                        Button(
                            onClick = { 
                                selectedTaskId = if (isSelected) null else task.taskId 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .size(width = 60.dp, height = 30.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "#${task.taskId}", 
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    if (tasks.size > 5) {
                        Text(
                            "...${tasks.size - 5}个更多",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
        
        // 任务信息面板和结果解读
        if (tasks.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp) // 增加高度以容纳更多内容
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface // 使用更亮的背景色
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "模拟结果解读",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 基本统计信息
                    Text("总任务数: ${tasks.size}")
                    Text("有候选用户的任务数: ${results.count { it.candidates > 0 }}")
                    Text("平均候选用户数: ${results.map { it.candidates }.average().format(2)}")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 添加结果解读
                    val coverageRate = (results.count { it.candidates > 0 }.toFloat() / tasks.size * 100).format(1)
                    val highCandidateTasks = results.count { it.candidates >= 3 }
                    
                    Text(
                        text = "结果分析",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("任务覆盖率: ${coverageRate}%")
                    Text("高候选用户任务数(≥3): $highCandidateTasks")
                    
                    // 添加简单评估
                    val evaluationText = when {
                        coverageRate.toFloat() > 80 -> "任务分配效果良好，大部分任务都有候选用户"
                        coverageRate.toFloat() > 50 -> "任务分配效果一般，约半数任务有候选用户"
                        else -> "任务分配效果较差，建议调整任务分布或增加用户数量"
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = evaluationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 地图边界数据类
 */
data class MapBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
) {
    // 将地理坐标转换为画布坐标
    fun toCanvasPosition(lat: Double, lon: Double, canvasSize: androidx.compose.ui.geometry.Size): Offset {
        val x = ((lon - minLon) / (maxLon - minLon)) * canvasSize.width
        // 注意：纬度需要反转，因为画布坐标系的y轴是向下的
        val y = (1 - (lat - minLat) / (maxLat - minLat)) * canvasSize.height
        return Offset(x.toFloat(), y.toFloat())
    }
    
    // 将米转换为像素（近似值，基于纬度）
    fun metersToPixels(meters: Double, lat: Double, canvasSize: androidx.compose.ui.geometry.Size): Float {
        // 在给定纬度下，1度经度大约等于多少米
        val metersPerLonDegree = 111320 * Math.cos(Math.toRadians(lat))
        // 计算米对应的经度差
        val lonDiff = meters / metersPerLonDegree
        // 将经度差转换为像素
        return ((lonDiff / (maxLon - minLon)) * canvasSize.width).toFloat()
    }
}

/**
 * 在画布上绘制网格和坐标
 */
private fun DrawScope.drawGrid(bounds: MapBounds) {
    val gridColor = Color.Gray.copy(alpha = 0.2f) // 降低网格线透明度
    val gridSteps = 4 // 减少网格线数量提高清晰度
    
    // 绘制经度线
    for (i in 0..gridSteps) {
        val lon = bounds.minLon + (bounds.maxLon - bounds.minLon) * i / gridSteps
        val x = ((lon - bounds.minLon) / (bounds.maxLon - bounds.minLon)) * size.width
        
        drawLine(
            color = gridColor,
            start = Offset(x.toFloat(), 0f),
            end = Offset(x.toFloat(), size.height),
            strokeWidth = 0.8f // 更细的线条
        )
        
        // 绘制经度标签背景
        val textBgPaint = Paint().apply {
            color = Color.White.toArgb()
            alpha = 200
        }
        
        val labelWidth = 80f
        val labelHeight = 25f
        
        drawContext.canvas.nativeCanvas.drawRect(
            x.toFloat() - labelWidth/2,
            size.height - labelHeight - 5f,
            x.toFloat() + labelWidth/2,
            size.height - 5f,
            textBgPaint
        )
        
        // 绘制经度标签
        val textPaint = Paint().apply {
            color = Color.DarkGray.toArgb()
            textSize = 22f // 更小的字体
            textAlign = Paint.Align.CENTER
        }
        
        drawContext.canvas.nativeCanvas.drawText(
            String.format("%.3f°", lon), // 减少小数位数
            x.toFloat(),
            size.height - 10f,
            textPaint
        )
    }
    
    // 绘制纬度线
    for (i in 0..gridSteps) {
        val lat = bounds.minLat + (bounds.maxLat - bounds.minLat) * i / gridSteps
        val y = (1 - (lat - bounds.minLat) / (bounds.maxLat - bounds.minLat)) * size.height
        
        drawLine(
            color = gridColor,
            start = Offset(0f, y.toFloat()),
            end = Offset(size.width, y.toFloat()),
            strokeWidth = 0.8f // 更细的线条
        )
        
        // 绘制纬度标签背景
        val textBgPaint = Paint().apply {
            color = Color.White.toArgb()
            alpha = 200
        }
        
        val labelWidth = 80f
        val labelHeight = 25f
        
        drawContext.canvas.nativeCanvas.drawRect(
            5f,
            y.toFloat() - labelHeight/2,
            labelWidth + 5f,
            y.toFloat() + labelHeight/2,
            textBgPaint
        )
        
        // 绘制纬度标签
        val textPaint = Paint().apply {
            color = Color.DarkGray.toArgb()
            textSize = 22f // 更小的字体
            textAlign = Paint.Align.LEFT
        }
        
        drawContext.canvas.nativeCanvas.drawText(
            String.format("%.3f°", lat), // 减少小数位数
            10f,
            y.toFloat() + 8f,
            textPaint
        )
    }
}

/**
 * 格式化Double为指定小数位数的字符串
 */
fun Double.format(digits: Int): String = String.format("%.${digits}f", this)

/**
 * 格式化Float为指定小数位数的字符串
 */
fun Float.format(digits: Int): String = String.format("%.${digits}f", this)
