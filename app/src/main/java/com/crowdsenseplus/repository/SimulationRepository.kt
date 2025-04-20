package com.crowdsenseplus.repository

import android.content.Context
import android.util.Log
import com.crowdsenseplus.model.SimulationModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 模拟数据仓库，负责数据持久化
 */
class SimulationRepository {
    private val TAG = "SimulationRepository"
    
    /**
     * 保存的模拟数据
     */
    data class SavedSimulation(
        val userMovements: List<SimulationModel.UserMovementEvent>,
        val tasks: List<SimulationModel.Task>,
        val results: List<SimulationModel.SimulationResult>
    )
    
    /**
     * 保存模拟数据
     */
    suspend fun saveSimulation(
        context: Context,
        userMovements: List<SimulationModel.UserMovementEvent>,
        tasks: List<SimulationModel.Task>,
        results: List<SimulationModel.SimulationResult>
    ) = withContext(Dispatchers.IO) {
        try {
            // 创建保存目录
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val simulationDir = File(context.filesDir, "saved_simulations/$timestamp")
            simulationDir.mkdirs()
            
            // 保存用户移动数据
            val userMovementsFile = File(simulationDir, "user_movements.txt")
            userMovementsFile.bufferedWriter().use { writer ->
                writer.write("user_id lat lon timestamp day hour minute second\n")
                for (movement in userMovements) {
                    writer.write("${movement.userId} ${movement.latitude} ${movement.longitude} " +
                            "${movement.timestamp} ${movement.day} ${movement.hour} " +
                            "${movement.minute} ${movement.second}\n")
                }
            }
            
            // 保存任务数据
            val tasksFile = File(simulationDir, "tasks.txt")
            tasksFile.bufferedWriter().use { writer ->
                writer.write("id_task lat lon timestamp duration distance timeslots\n")
                for (task in tasks) {
                    writer.write("${task.taskId} ${task.latitude} ${task.longitude} " +
                            "${task.timestamp} ${task.duration} ${task.distance} ${task.timeslots}\n")
                }
            }
            
            // 保存结果数据
            val resultsFile = File(simulationDir, "results.txt")
            resultsFile.bufferedWriter().use { writer ->
                writer.write("task_id candidates\n")
                for (result in results) {
                    writer.write("${result.taskId} ${result.candidates}\n")
                }
            }
            
            Log.d(TAG, "模拟数据已保存到 $simulationDir")
            true
        } catch (e: Exception) {
            Log.e(TAG, "保存模拟数据失败: ${e.message}")
            throw e
        }
    }
    
    /**
     * 加载保存的模拟数据
     */
    suspend fun loadSimulation(context: Context, simulationId: String): SavedSimulation = withContext(Dispatchers.IO) {
        try {
            val simulationDir = File(context.filesDir, "saved_simulations/$simulationId")
            if (!simulationDir.exists()) {
                throw Exception("模拟数据不存在")
            }
            
            // 加载用户移动数据
            val userMovementsFile = File(simulationDir, "user_movements.txt")
            val userMovements = mutableListOf<SimulationModel.UserMovementEvent>()
            userMovementsFile.bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 8) {
                        userMovements.add(
                            SimulationModel.UserMovementEvent(
                                userId = parts[0].toInt(),
                                latitude = parts[1].toDouble(),
                                longitude = parts[2].toDouble(),
                                timestamp = parts[3].toDouble(),
                                day = parts[4].toInt(),
                                hour = parts[5].toInt(),
                                minute = parts[6].toInt(),
                                second = parts[7].toInt()
                            )
                        )
                    }
                }
            }
            
            // 加载任务数据
            val tasksFile = File(simulationDir, "tasks.txt")
            val tasks = mutableListOf<SimulationModel.Task>()
            tasksFile.bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 7) {
                        tasks.add(
                            SimulationModel.Task(
                                taskId = parts[0].toInt(),
                                latitude = parts[1].toDouble(),
                                longitude = parts[2].toDouble(),
                                timestamp = parts[3].toDouble(),
                                duration = parts[4].toInt(),
                                distance = parts[5].toInt(),
                                timeslots = parts[6].toInt()
                            )
                        )
                    }
                }
            }
            
            // 加载结果数据
            val resultsFile = File(simulationDir, "results.txt")
            val results = mutableListOf<SimulationModel.SimulationResult>()
            resultsFile.bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        results.add(
                            SimulationModel.SimulationResult(
                                taskId = parts[0].toInt(),
                                candidates = parts[1].toInt()
                            )
                        )
                    }
                }
            }
            
            SavedSimulation(userMovements, tasks, results)
        } catch (e: Exception) {
            Log.e(TAG, "加载模拟数据失败: ${e.message}")
            throw e
        }
    }
    
    /**
     * 获取保存的模拟列表
     */
    suspend fun getSavedSimulations(context: Context): List<String> = withContext(Dispatchers.IO) {
        try {
            val savedDir = File(context.filesDir, "saved_simulations")
            if (!savedDir.exists()) {
                return@withContext emptyList<String>()
            }
            
            savedDir.list()?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "获取保存的模拟列表失败: ${e.message}")
            emptyList<String>()
        }
    }
    
    /**
     * 删除保存的模拟
     */
    suspend fun deleteSimulation(context: Context, simulationId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val simulationDir = File(context.filesDir, "saved_simulations/$simulationId")
            if (simulationDir.exists()) {
                simulationDir.deleteRecursively()
                Log.d(TAG, "已删除模拟 $simulationId")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除模拟失败: ${e.message}")
            false
        }
    }
}