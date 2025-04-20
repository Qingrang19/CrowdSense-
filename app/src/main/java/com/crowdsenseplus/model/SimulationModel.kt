package com.crowdsenseplus.model

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 模拟器核心模型类
 * 重构自Python多接入边缘雾计算群智感知模拟器
 */
class SimulationModel {
    // 模拟参数
    data class SimulationParameters(
        val days: Int = 3,
        val numberOfUsers: Int = 2000,
        val locomotionType: Int = 1, // 1=walk, 2=bike, 3=drive
        val numberOfTasks: Int = 30,
        val executionRange: Int = 100, // 任务执行范围(米)
        val taskDuration: Int = 60,    // 任务持续时间(分钟)
        val timeslotDuration: Int = 60, // 时间槽持续时间(分钟)
        val platformType: Int = 1       // 1=MCS, 2=FOG-MCS, 3=MEC-MCS
    )
    
    // 用户移动事件
    data class UserMovementEvent(
        val userId: Int,
        val latitude: Double,
        val longitude: Double,
        val timestamp: Double,
        val day: Int,
        val hour: Int,
        val minute: Int,
        val second: Int
    )
    
    // 任务定义
    data class Task(
        val taskId: Int,
        val latitude: Double,
        val longitude: Double,
        val timestamp: Double,
        val duration: Int,
        val distance: Int,
        val timeslots: Int
    )
    
    // 模拟结果
    data class SimulationResult(
        val taskId: Int,
        val candidates: Int
    )
    
    private val TAG = "SimulationModel"
    private var parameters = SimulationParameters()
    private val userMovements = mutableListOf<UserMovementEvent>()
    private val tasks = mutableListOf<Task>()
    private val results = mutableListOf<SimulationResult>()
    
    /**
     * 计算两点之间的Haversine距离(米)
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // 地球半径(米)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2) + 
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * 
                Math.sin(dLon / 2).pow(2)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
    
    /**
     * 设置模拟参数
     */
    fun setParameters(params: SimulationParameters) {
        parameters = params
    }
    
    /**
     * 生成用户移动事件
     * 简化版本，实际应用中可以使用真实GPS数据或更复杂的移动模型
     */
    suspend fun generateUserMovements(context: Context, cityName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            userMovements.clear()
            val random = Random(System.currentTimeMillis())
            
            // 模拟城市边界 (实际应用中应该使用真实地图数据)
            // 这里使用简化的随机生成方法
            val centerLat = 42.8371 // 示例中心点
            val centerLon = -1.6389
            val radius = 0.05 // 约5公里半径
            
            val endTime = System.currentTimeMillis() / 1000.0
            val startTime = endTime - 60 * 60 * 24 * parameters.days
            
            // 用户移动速度范围(米/秒)
            val (minSpeed, maxSpeed) = when(parameters.locomotionType) {
                1 -> Pair(1.0, 1.5)     // 步行 3.6-5.4 km/h
                2 -> Pair(2.7, 5.5)     // 自行车 10-20 km/h
                3 -> Pair(5.5, 13.9)    // 驾车 20-50 km/h
                else -> Pair(1.0, 1.5)  // 默认步行
            }
            
            // 为每个用户生成移动事件
            for (userId in 1..parameters.numberOfUsers) {
                var currentTime = startTime
                var currentLat = centerLat + (random.nextDouble() - 0.5) * radius * 2
                var currentLon = centerLon + (random.nextDouble() - 0.5) * radius * 2
                
                while (currentTime < endTime) {
                    // 添加当前位置事件
                    val timestamp = currentTime
                    val date = java.util.Date((timestamp * 1000).toLong())
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = date
                    
                    val day = (currentTime - startTime) / (24 * 60 * 60)
                    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                    val minute = calendar.get(java.util.Calendar.MINUTE)
                    val second = calendar.get(java.util.Calendar.SECOND)
                    
                    userMovements.add(UserMovementEvent(
                        userId = userId,
                        latitude = currentLat,
                        longitude = currentLon,
                        timestamp = timestamp,
                        day = day.toInt(),
                        hour = hour,
                        minute = minute,
                        second = second
                    ))
                    
                    // 计算下一个位置
                    val speed = minSpeed + random.nextDouble() * (maxSpeed - minSpeed)
                    val moveDuration = 60.0 * (5 + random.nextInt(10)) // 5-15分钟移动一次
                    val moveDistance = speed * moveDuration
                    
                    // 随机方向移动
                    val angle = random.nextDouble() * 2 * Math.PI
                    val latChange = moveDistance / 111000.0 * Math.cos(angle) // 约111km每纬度
                    val lonChange = moveDistance / (111000.0 * Math.cos(Math.toRadians(currentLat))) * Math.sin(angle)
                    
                    currentLat += latChange
                    currentLon += lonChange
                    currentTime += moveDuration
                }
            }
            
            // 保存到文件(可选)
            saveUserMovementsToFile(context)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "生成用户移动事件失败: ${e.message}")
            false
        }
    }
    
    /**
     * 生成任务
     */
    suspend fun generateTasks(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            tasks.clear()
            val random = Random(System.currentTimeMillis())
            
            // 使用与用户移动相同的时间范围
            val endTime = System.currentTimeMillis() / 1000.0
            val startTime = endTime - 60 * 60 * 24 * parameters.days
            
            // 从用户移动数据中提取位置范围
            var minLat = Double.MAX_VALUE
            var maxLat = Double.MIN_VALUE
            var minLon = Double.MAX_VALUE
            var maxLon = Double.MIN_VALUE
            
            if (userMovements.isNotEmpty()) {
                for (movement in userMovements) {
                    if (movement.latitude < minLat) minLat = movement.latitude
                    if (movement.latitude > maxLat) maxLat = movement.latitude
                    if (movement.longitude < minLon) minLon = movement.longitude
                    if (movement.longitude > maxLon) maxLon = movement.longitude
                }
            } else {
                // 如果没有用户移动数据，使用默认范围
                minLat = 42.8 - 0.05
                maxLat = 42.8 + 0.05
                minLon = -1.64 - 0.05
                maxLon = -1.64 + 0.05
            }
            
            // 生成任务
            for (taskId in 1..parameters.numberOfTasks) {
                val timestamp = startTime + random.nextDouble() * (endTime - startTime)
                val latitude = minLat + random.nextDouble() * (maxLat - minLat)
                val longitude = minLon + random.nextDouble() * (maxLon - minLon)
                
                tasks.add(Task(
                    taskId = taskId,
                    latitude = latitude,
                    longitude = longitude,
                    timestamp = timestamp,
                    duration = parameters.taskDuration,
                    distance = parameters.executionRange,
                    timeslots = parameters.timeslotDuration
                ))
            }
            
            // 保存到文件(可选)
            saveTasksToFile(context)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "生成任务失败: ${e.message}")
            false
        }
    }
    
    /**
     * 计算每个任务的候选用户
     */
    suspend fun computeCandidatesForTasks(): List<SimulationResult> = withContext(Dispatchers.Default) {
        results.clear()
        
        for (task in tasks) {
            val candidatesForTask = mutableListOf<UserMovementEvent>()
            val timeslotDuration = task.duration / task.timeslots * 60.0 // 转换为秒
            
            // 找出时间范围内的用户
            val timeRangeUsers = userMovements.filter { 
                it.timestamp >= task.timestamp && 
                it.timestamp <= (task.timestamp + timeslotDuration) 
            }
            
            // 在这些用户中找出距离范围内的用户
            for (user in timeRangeUsers) {
                val distance = haversineDistance(
                    task.latitude, task.longitude,
                    user.latitude, user.longitude
                )
                
                // 如果用户在任务执行范围内，添加为候选者
                // 根据原Python代码，只要距离计算有效（>=0）且小于等于任务执行范围就添加为候选者
                if (distance >= 0 && distance <= task.distance) {
                    candidatesForTask.add(user)
                }
            }
            
            // 添加结果
            results.add(SimulationResult(
                taskId = task.taskId,
                candidates = candidatesForTask.distinctBy { it.userId }.size // 去重计数
            ))
        }
        
        results
    }
    
    /**
     * 保存用户移动数据到文件
     */
    private fun saveUserMovementsToFile(context: Context) {
        try {
            val file = File(context.filesDir, "user_movements.txt")
            file.bufferedWriter().use { writer ->
                writer.write("user_id lat lon timestamp day hour minute second\n")
                for (movement in userMovements) {
                    writer.write("${movement.userId} ${movement.latitude} ${movement.longitude} " +
                            "${movement.timestamp} ${movement.day} ${movement.hour} " +
                            "${movement.minute} ${movement.second}\n")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存用户移动数据失败: ${e.message}")
        }
    }
    
    /**
     * 保存任务数据到文件
     */
    private fun saveTasksToFile(context: Context) {
        try {
            val file = File(context.filesDir, "tasks.txt")
            file.bufferedWriter().use { writer ->
                writer.write("id_task lat lon timestamp duration distance timeslots\n")
                for (task in tasks) {
                    writer.write("${task.taskId} ${task.latitude} ${task.longitude} " +
                            "${task.timestamp} ${task.duration} ${task.distance} ${task.timeslots}\n")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存任务数据失败: ${e.message}")
        }
    }
    
    /**
     * 保存模拟结果到文件
     */
    fun saveResultsToFile(context: Context) {
        try {
            val file = File(context.filesDir, "simulation_results.txt")
            file.bufferedWriter().use { writer ->
                writer.write("task_id candidates\n")
                for (result in results) {
                    writer.write("${result.taskId} ${result.candidates}\n")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存模拟结果失败: ${e.message}")
        }
    }
    
    /**
     * 获取当前模拟参数
     */
    fun getParameters(): SimulationParameters = parameters
    
    /**
     * 获取用户移动数据
     */
    fun getUserMovements(): List<UserMovementEvent> = userMovements
    
    /**
     * 获取任务数据
     */
    fun getTasks(): List<Task> = tasks
    
    /**
     * 获取模拟结果
     */
    fun getResults(): List<SimulationResult> = results
}